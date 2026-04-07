package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.ExcelImportGroupRequestDTO;
import com.example.DoAn.dto.request.ExcelQuestionGroupDTO;
import com.example.DoAn.dto.request.ExcelImportRequestDTO;
import com.example.DoAn.dto.response.ExcelParseGroupResultDTO;
import com.example.DoAn.dto.response.ExcelParseResultDTO;
import com.example.DoAn.dto.response.ExcelParseResultDTO.ErrorRowDTO;
import com.example.DoAn.dto.response.ExcelParseResultDTO.ValidRowDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.model.AnswerOption;
import com.example.DoAn.model.Question;
import com.example.DoAn.model.QuestionGroup;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.AnswerOptionRepository;
import com.example.DoAn.repository.QuestionGroupRepository;
import com.example.DoAn.repository.QuestionRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.ExcelQuestionImportService;
import com.example.DoAn.util.AIQuestionPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelQuestionImportServiceImpl implements ExcelQuestionImportService {

    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserRepository userRepository;
    private final QuestionGroupRepository questionGroupRepository;
    private final AIQuestionPromptBuilder promptBuilder;

    @Override
    public ExcelParseResultDTO parseFile(MultipartFile file, String questionType) throws Exception {
        List<ValidRowDTO> validRows = new ArrayList<>();
        List<ErrorRowDTO> errorRows = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int startRow = findDataStartRow(sheet, questionType);

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;

                int rowIdx = i + 1;
                try {
                    ValidRowDTO dto = parseRow(row, rowIdx, questionType);
                    if (dto != null) {
                        validRows.add(dto);
                    }
                } catch (ValidationException ve) {
                    Map<String, String> raw = extractRawData(row, questionType);
                    errorRows.add(ErrorRowDTO.builder()
                            .rowIndex(rowIdx)
                            .message(ve.getMessage())
                            .rawData(raw)
                            .build());
                }
            }
        }

        return ExcelParseResultDTO.builder()
                .valid(validRows)
                .errors(errorRows)
                .totalRows(validRows.size() + errorRows.size())
                .build();
    }

    @Override
    @Transactional
    public int importQuestions(ExcelImportRequestDTO request, String userEmail) throws Exception {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userEmail));
        log.info("[IMPORT] total questions={}", request.getQuestions().size());
        for (int qi = 0; qi < request.getQuestions().size(); qi++) {
            var qdto = request.getQuestions().get(qi);
            log.info("[IMPORT] question[{}]: content='{}' type={} matchLeft={} matchRight={} correctPairs={}",
                    qi, qdto.getContent(), qdto.getQuestionType(), qdto.getMatchLeft(), qdto.getMatchRight(), qdto.getCorrectPairs());
        }

        int saved = 0;
        for (ExcelImportRequestDTO.ExcelQuestionDTO qdto : request.getQuestions()) {
            String skill = qdto.getSkill() != null ? qdto.getSkill().toUpperCase() : "READING";
            String cefr = qdto.getCefrLevel() != null ? qdto.getCefrLevel().toUpperCase() : "B1";
            checkDuplicate(qdto.getContent(), skill, cefr);

            Question question = buildQuestion(qdto, user, skill, cefr);
            question = questionRepository.save(question);

            if (qdto.getOptions() != null && !qdto.getOptions().isEmpty()) {
                int idx = 0;
                for (ExcelImportRequestDTO.OptionDTO opt : qdto.getOptions()) {
                    AnswerOption ao = AnswerOption.builder()
                            .question(question)
                            .title(opt.getTitle())
                            .correctAnswer(opt.getCorrect())
                            .orderIndex(idx++)
                            .build();
                    answerOptionRepository.save(ao);
                }
            } else if (qdto.getMatchLeft() != null && qdto.getMatchRight() != null
                    && qdto.getCorrectPairs() != null) {
                List<String> left = qdto.getMatchLeft();
                List<String> right = qdto.getMatchRight();
                List<Integer> pairs = qdto.getCorrectPairs();
                log.info("[MATCHING] row={} content='{}' left={} right={} pairs={}",
                        qdto.getRowIndex(), qdto.getContent(), left, right, pairs);
                for (int i = 0; i < left.size(); i++) {
                    int rightIdx = pairs.get(i) - 1;
                    log.info("[MATCHING] i={} left[{}]='{}' pairs[{}]={} rightIdx={} right[{}]='{}'",
                            i, i, left.get(i), i, pairs.get(i), rightIdx, rightIdx, right.get(rightIdx));
                    AnswerOption aoLeft = AnswerOption.builder()
                            .question(question)
                            .title(left.get(i))
                            .correctAnswer(false)
                            .orderIndex(i)
                            .matchTarget(right.get(rightIdx))
                            .build();
                    answerOptionRepository.save(aoLeft);
                    log.info("[MATCHING] SAVED LEFT: title='{}' matchTarget='{}' orderIndex={}",
                            left.get(i), right.get(rightIdx), i);

                    AnswerOption aoRight = AnswerOption.builder()
                            .question(question)
                            .title(right.get(rightIdx))
                            .correctAnswer(false)
                            .orderIndex(left.size() + i)
                            .build();
                    answerOptionRepository.save(aoRight);
                    log.info("[MATCHING] SAVED RIGHT: title='{}' matchTarget=null orderIndex={}",
                            right.get(rightIdx), left.size() + i);
                }
            }
            saved++;
        }
        return saved;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Returns the 0-based row index of the FIRST actual question data row,
     * skipping headers and note rows. For MULTIPLE_CHOICE templates:
     *   row 0 = headers ("Content", "Option A"...)
     *   row 1 = note ("Chú ý: CorrectAnswer...")
     *   row 2+ = actual data rows
     * For templates without note rows, data starts at row 2.
     */
    private int findDataStartRow(Sheet sheet, String questionType) {
        for (int i = 0; i < sheet.getLastRowNum() + 2; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            Cell first = row.getCell(0);
            String val = getCellString(first);
            if (val == null || val.isBlank()) continue;
            // If it's a header or note row, skip it and keep looking
            if (isHeaderOrNoteRow(val)) continue;
            // Found actual data row — return its 0-based index
            return i;
        }
        // Fallback: skip 2 rows (header + note)
        return 2;
    }

    /**
     * Returns true if the first-cell value indicates a header or note row
     * (not an actual question data row).
     */
    private boolean isHeaderOrNoteRow(String firstCellValue) {
        if (firstCellValue == null) return true;
        String lower = firstCellValue.toLowerCase();
        return lower.contains("content") || lower.contains("chú ý")
                || lower.contains("lưu ý") || lower.startsWith("note")
                || lower.contains("sample");
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < 3; i++) {
            Cell c = row.getCell(i);
            if (c != null && c.getCellType() != CellType.BLANK) {
                String v = getCellString(c);
                if (v != null && !v.isBlank()) return false;
            }
        }
        return true;
    }

    private ValidRowDTO parseRow(Row row, int rowIdx, String questionType) throws ValidationException {
        String content = trim(getCell(row, 0));
        if (content == null || content.isBlank()) {
            throw new ValidationException("Dòng " + rowIdx + ": Nội dung câu hỏi trống.");
        }

        String skill = trim(getCell(row, colIndex(questionType, "skill")));
        String cefr = trim(getCell(row, colIndex(questionType, "cefr")));
        String topic = trim(getCell(row, colIndex(questionType, "topic")));
        String explanation = trim(getCell(row, colIndex(questionType, "explanation")));
        String audioUrl = trim(getCell(row, colIndex(questionType, "audioUrl")));

        if (skill != null && !promptBuilder.isValidSkill(skill)) {
            throw new ValidationException("Dòng " + rowIdx + ": Skill '" + skill + "' không hợp lệ.");
        }
        if (cefr != null && !promptBuilder.isValidCefr(cefr)) {
            throw new ValidationException("Dòng " + rowIdx + ": CEFR '" + cefr + "' không hợp lệ (A1-C2).");
        }
        // Only validate AudioUrl for SPEAKING questions when a value is provided
        if ("SPEAKING".equals(questionType) && audioUrl != null && !audioUrl.isBlank()
                && !audioUrl.startsWith("http")) {
            throw new ValidationException("Dòng " + rowIdx + ": AudioUrl phải là URL bắt đầu bằng http/https.");
        }

        ValidRowDTO.ValidRowDTOBuilder builder = ValidRowDTO.builder()
                .rowIndex(rowIdx)
                .content(content)
                .questionType(questionType)
                .skill(skill != null ? skill.toUpperCase() : "READING")
                .cefrLevel(cefr != null ? cefr.toUpperCase() : "B1")
                .topic(topic)
                .explanation(explanation)
                .audioUrl(audioUrl)
                .selected(true);

        log.info("[PARSE-ROW] row={} type={} content='{}' skill='{}' cefr='{}' topic='{}' explanation='{}'",
                rowIdx, questionType, content, skill, cefr, topic, explanation);

        switch (questionType) {
            case "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI" -> parseMCOptions(row, rowIdx, builder, questionType);
            case "FILL_IN_BLANK" -> builder.correctAnswer(trim(getCell(row, 1)));
            case "MATCHING" -> parseMatching(row, rowIdx, builder);
            // WRITING, SPEAKING: no additional fields
        }

        return builder.build();
    }

    private void parseMCOptions(Row row, int rowIdx, ValidRowDTO.ValidRowDTOBuilder builder, String type)
            throws ValidationException {
        String optA = trim(getCell(row, 1));
        String optB = trim(getCell(row, 2));
        String optC = trim(getCell(row, 3));
        String optD = trim(getCell(row, 4));
        String correct = trim(getCell(row, 5));

        if (optA == null && optB == null && optC == null && optD == null) {
            throw new ValidationException("Dòng " + rowIdx + ": Phải có ít nhất 2 đáp án.");
        }

        if (correct == null || correct.isBlank()) {
            throw new ValidationException("Dòng " + rowIdx + ": Chưa chọn đáp án đúng.");
        }

        List<ExcelParseResultDTO.OptionDTO> options = new ArrayList<>();
        int idx = 0;
        for (String label : List.of("A", "B", "C", "D")) {
            String val = idx == 0 ? optA : idx == 1 ? optB : idx == 2 ? optC : optD;
            if (val != null && !val.isBlank()) {
                boolean isCorrect = correct.toUpperCase().contains(label.toUpperCase());
                options.add(ExcelParseResultDTO.OptionDTO.builder().title(val).correct(isCorrect).build());
            }
            idx++;
        }

        long correctCount = options.stream().filter(o -> Boolean.TRUE.equals(o.getCorrect())).count();
        if (correctCount == 0) {
            throw new ValidationException("Dòng " + rowIdx + ": Chưa chọn đáp án đúng.");
        }
        if ("MULTIPLE_CHOICE_MULTI".equals(type) && correctCount == options.size()) {
            throw new ValidationException("Dòng " + rowIdx + ": Multi-select không thể có tất cả đáp án đều đúng.");
        }

        builder.options(options).correctAnswer(correct);
    }

    private void parseMatching(Row row, int rowIdx, ValidRowDTO.ValidRowDTOBuilder builder)
            throws ValidationException {
        String leftStr = trim(getCell(row, 1));
        String rightStr = trim(getCell(row, 2));
        String pairsStr = trim(getCell(row, 3));

        if (leftStr == null || rightStr == null || pairsStr == null) {
            throw new ValidationException("Dòng " + rowIdx + ": Thiếu cột ghép nối.");
        }

        List<String> left = split(leftStr, "|");
        List<String> right = split(rightStr, "|");
        List<Integer> pairs = parsePairs(pairsStr);

        if (left.size() != right.size()) {
            throw new ValidationException("Dòng " + rowIdx + ": Số cặp ghép không khớp (" + left.size() + " trái, " + right.size() + " phải).");
        }
        if (pairs.size() != left.size()) {
            throw new ValidationException("Dòng " + rowIdx + ": CorrectPairs phải có đúng " + left.size() + " số (VD: " + makeExample(left.size()) + ").");
        }
        for (Integer p : pairs) {
            if (p < 1 || p > right.size()) {
                throw new ValidationException("Dòng " + rowIdx + ": CorrectPairs chứa số " + p + " ngoài phạm vi (1-" + right.size() + ").");
            }
        }

        log.info("[PARSE-MATCHING] row={} leftStr='{}' rightStr='{}' pairsStr='{}' => left={} right={} pairs={}",
                rowIdx, leftStr, rightStr, pairsStr, left, right, pairs);
        builder.matchLeft(left).matchRight(right).correctPairs(pairs);
    }

    private Question buildQuestion(ExcelImportRequestDTO.ExcelQuestionDTO dto, User user, String skill, String cefr) {
        return Question.builder()
                .content(dto.getContent())
                .questionType(dto.getQuestionType() != null ? dto.getQuestionType() : "MULTIPLE_CHOICE_SINGLE")
                .skill(skill)
                .cefrLevel(cefr)
                .topic(dto.getTopic())
                .explanation(dto.getExplanation())
                .audioUrl(dto.getAudioUrl())
                .imageUrl(dto.getImageUrl())
                .status("DRAFT")
                .source("EXPERT_BANK")
                .createdMethod("EXCEL_IMPORTED")
                .user(user)
                .build();
    }

    private int colIndex(String questionType, String field) {
        return switch (questionType) {
            case "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI" -> switch (field) {
                case "skill" -> 6; case "cefr" -> 7; case "topic" -> 8; case "explanation" -> 9; default -> 0;
            };
            case "FILL_IN_BLANK" -> switch (field) {
                case "skill" -> 2; case "cefr" -> 3; case "topic" -> 4; case "explanation" -> 5; default -> 0;
            };
            case "MATCHING" -> switch (field) {
                case "skill" -> 4; case "cefr" -> 5; case "topic" -> 6; case "explanation" -> -1; case "audioUrl" -> -1; default -> 0;
            };
            case "WRITING" -> switch (field) {
                case "skill" -> 1; case "cefr" -> 2; case "topic" -> 3; case "explanation" -> 4; default -> 0;
            };
            case "SPEAKING" -> switch (field) {
                case "skill" -> 2; case "cefr" -> 3; case "topic" -> 4; default -> 0;
            };
            default -> 0;
        };
    }

    private String getCell(Row row, int idx) {
        Cell c = row.getCell(idx);
        return c != null ? getCellString(c) : null;
    }

    private String getCellString(Cell c) {
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> c.getNumericCellValue() % 1 == 0
                    ? String.valueOf((long) c.getNumericCellValue())
                    : String.valueOf(c.getNumericCellValue());
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            case FORMULA -> c.getCellFormula();
            default -> null;
        };
    }

    private List<String> split(String s, String delim) {
        if (s == null) return List.of();
        return Arrays.stream(s.split("\\s*" + Pattern.quote(delim) + "\\s*"))
                .filter(t -> !t.isBlank())
                .toList();
    }

    private List<Integer> parsePairs(String s) throws ValidationException {
        try {
            return split(s, ",").stream()
                    .map(Integer::parseInt)
                    .toList();
        } catch (NumberFormatException e) {
            throw new ValidationException("CorrectPairs phải là số nguyên cách nhau bằng dấu phẩy, VD: 1,2,3.");
        }
    }

    private Map<String, String> extractRawData(Row row, String questionType) {
        Map<String, String> raw = new LinkedHashMap<>();
        int max = switch (questionType) {
            case "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI" -> 10;
            case "FILL_IN_BLANK" -> 6;
            case "MATCHING" -> 7;
            case "WRITING" -> 5;
            case "SPEAKING" -> 5;
            default -> 5;
        };
        for (int i = 0; i < max; i++) {
            raw.put("col" + i, getCell(row, i));
        }
        return raw;
    }

    private String makeExample(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            if (i > 1) sb.append(",");
            sb.append(i);
        }
        return sb.toString();
    }

    private String detectQuestionType(String val, String fallback) {
        if (val == null || val.isBlank()) return fallback;
        return switch (val.toUpperCase().replace(" ", "_").replace("-", "_")) {
            case "MULTIPLE_CHOICE_SINGLE", "MC_SINGLE", "MC" -> "MULTIPLE_CHOICE_SINGLE";
            case "MULTIPLE_CHOICE_MULTI", "MC_MULTI" -> "MULTIPLE_CHOICE_MULTI";
            case "FILL_IN_BLANK", "FILL", "FIB" -> "FILL_IN_BLANK";
            case "MATCHING", "MATCH" -> "MATCHING";
            case "WRITING" -> "WRITING";
            case "SPEAKING" -> "SPEAKING";
            default -> fallback;
        };
    }

    @Override
    public ExcelParseGroupResultDTO parseGroupFile(MultipartFile file) throws Exception {
        List<ExcelParseGroupResultDTO.ValidGroupRowDTO> valid = new ArrayList<>();
        List<ExcelParseGroupResultDTO.ErrorRowDTO> errors = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            Row groupHeaderRow = sheet.getRow(0);
            Row groupDataRow = sheet.getRow(2);

            log.info("[PARSE-GROUP] sheet.getRow(0)={}, sheet.getRow(1)={}, sheet.getRow(2)={}, lastRowNum={}",
                    groupHeaderRow != null ? "exists" : "NULL",
                    sheet.getRow(1) != null ? "exists" : "NULL",
                    groupDataRow != null ? "exists" : "NULL",
                    sheet.getLastRowNum());
            if (groupHeaderRow == null || groupDataRow == null) {
                throw new IllegalArgumentException(
                        "File Excel không đúng format. Cần có Row 1 (group header) và Row 3 (group data - dòng điền passage). " +
                        "lastRowNum=" + sheet.getLastRowNum());
            }

            // Parse group metadata
            String passage = trim(getCell(groupDataRow, 0));
            if (passage == null || passage.isBlank()) {
                errors.add(ExcelParseGroupResultDTO.ErrorRowDTO.builder()
                        .rowIndex(2)
                        .message("Passage không được trống.")
                        .rawData(Map.of("passage", passage != null ? passage : ""))
                        .build());
                return ExcelParseGroupResultDTO.builder()
                        .valid(valid).errors(errors).totalRows(0).build();
            }

            String skill = trim(getCell(groupDataRow, 1));
            String cefr = trim(getCell(groupDataRow, 2));
            String topic = trim(getCell(groupDataRow, 3));
            String audioUrl = trim(getCell(groupDataRow, 4));
            String imageUrl = trim(getCell(groupDataRow, 5));
            String explanation = trim(getCell(groupDataRow, 6));

            List<String> validationErrors = new ArrayList<>();
            if (skill != null && !promptBuilder.isValidSkill(skill)) {
                validationErrors.add("Skill '" + skill + "' không hợp lệ.");
            }
            if (cefr != null && !promptBuilder.isValidCefr(cefr)) {
                validationErrors.add("CEFR '" + cefr + "' không hợp lệ (A1-C2).");
            }
            if (!validationErrors.isEmpty()) {
                errors.add(ExcelParseGroupResultDTO.ErrorRowDTO.builder()
                        .rowIndex(2)
                        .message(String.join("; ", validationErrors))
                        .rawData(Map.of("skill", skill != null ? skill : "", "cefr", cefr != null ? cefr : ""))
                        .build());
            }

            // Parse child questions from Sheet 2
            List<ExcelParseResultDTO.ValidRowDTO> childQuestions = new ArrayList<>();
            Sheet childSheet = wb.getSheetAt(1); // Sheet index 1 = "Child Questions"
            if (childSheet == null) {
                errors.add(ExcelParseGroupResultDTO.ErrorRowDTO.builder()
                        .rowIndex(1)
                        .message("Thiếu Sheet 'Child Questions' (Sheet 2). Vui lòng dùng template chuẩn.")
                        .rawData(Map.of())
                        .build());
                return ExcelParseGroupResultDTO.builder()
                        .valid(valid).errors(errors).totalRows(0).build();
            }
            for (int i = 1; i <= childSheet.getLastRowNum(); i++) {
                Row row = childSheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;

                String content = trim(getCell(row, 0));
                if (content == null || content.isBlank()) continue;

                int rowIdx = i + 1;
                try {
                    String typeStr = trim(getCell(row, 1));
                    String questionType = detectQuestionType(typeStr, "MULTIPLE_CHOICE_SINGLE");
                    String childSkill = trim(getCell(row, 10));
                    String childCefr = trim(getCell(row, 11));
                    String childTopic = trim(getCell(row, 12));
                    String childExplanation = trim(getCell(row, 13));

                    if (childSkill != null && !promptBuilder.isValidSkill(childSkill)) {
                        throw new ValidationException("Skill '" + childSkill + "' không hợp lệ.");
                    }
                    if (childCefr != null && !promptBuilder.isValidCefr(childCefr)) {
                        throw new ValidationException("CEFR '" + childCefr + "' không hợp lệ.");
                    }

                    ExcelParseResultDTO.ValidRowDTO.ValidRowDTOBuilder childBuilder =
                            ExcelParseResultDTO.ValidRowDTO.builder()
                                    .rowIndex(rowIdx)
                                    .content(content)
                                    .questionType(questionType)
                                    .skill(childSkill != null ? childSkill.toUpperCase()
                                            : (skill != null ? skill.toUpperCase() : "READING"))
                                    .cefrLevel(childCefr != null ? childCefr.toUpperCase()
                                            : (cefr != null ? cefr.toUpperCase() : "B1"))
                                    .topic(childTopic != null ? childTopic : topic)
                                    .explanation(childExplanation != null ? childExplanation : explanation)
                                    .selected(true);

                    switch (questionType) {
                        case "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI" -> {
                            String optA = trim(getCell(row, 2));
                            String optB = trim(getCell(row, 3));
                            String optC = trim(getCell(row, 4));
                            String optD = trim(getCell(row, 5));
                            String correct = trim(getCell(row, 6));
                            if ((optA == null && optB == null && optC == null && optD == null) ||
                                    correct == null || correct.isBlank()) {
                                throw new ValidationException("Thiếu đáp án hoặc đáp án đúng.");
                            }
                            List<ExcelParseResultDTO.OptionDTO> opts = new ArrayList<>();
                            int idx = 0;
                            for (String label : List.of("A", "B", "C", "D")) {
                                String val = idx == 0 ? optA : idx == 1 ? optB : idx == 2 ? optC : optD;
                                if (val != null && !val.isBlank()) {
                                    opts.add(ExcelParseResultDTO.OptionDTO.builder()
                                            .title(val)
                                            .correct(correct.toUpperCase().contains(label.toUpperCase()))
                                            .build());
                                }
                                idx++;
                            }
                            childBuilder.options(opts).correctAnswer(correct);
                        }
                        case "FILL_IN_BLANK" -> {
                            String answer = trim(getCell(row, 6));
                            if (answer == null || answer.isBlank()) {
                                throw new ValidationException("Thiếu đáp án cho FILL_IN_BLANK.");
                            }
                            childBuilder.correctAnswer(answer);
                        }
                        case "MATCHING" -> {
                            String leftStr = trim(getCell(row, 7));
                            String rightStr = trim(getCell(row, 8));
                            String pairsStr = trim(getCell(row, 9));
                            if (leftStr == null || rightStr == null || pairsStr == null) {
                                throw new ValidationException("Thiếu cột ghép nối cho MATCHING.");
                            }
                            List<String> left = split(leftStr, "|");
                            List<String> right = split(rightStr, "|");
                            List<Integer> pairs = parsePairs(pairsStr);
                            if (left.size() != right.size()) {
                                throw new ValidationException("Số cặp ghép không khớp.");
                            }
                            if (pairs.size() != left.size()) {
                                throw new ValidationException("CorrectPairs phải có đúng " + left.size() + " số.");
                            }
                            childBuilder.matchLeft(left).matchRight(right).correctPairs(pairs);
                        }
                        default -> {
                            // WRITING, SPEAKING: no extra columns needed
                        }
                    }

                    childQuestions.add(childBuilder.build());
                } catch (ValidationException ve) {
                    Map<String, String> raw = new LinkedHashMap<>();
                    for (int c = 0; c < 14; c++) raw.put("col" + c, getCell(row, c));
                    errors.add(ExcelParseGroupResultDTO.ErrorRowDTO.builder()
                            .rowIndex(rowIdx).message(ve.getMessage()).rawData(raw).build());
                }
            }

            if (childQuestions.isEmpty() && validationErrors.isEmpty()) {
                errors.add(ExcelParseGroupResultDTO.ErrorRowDTO.builder()
                        .rowIndex(5)
                        .message("Không tìm thấy câu hỏi con nào trong file.")
                        .rawData(Map.of("passage", passage))
                        .build());
            }

            if (!childQuestions.isEmpty()) {
                List<ExcelImportRequestDTO.ExcelQuestionDTO> excelQuestions = childQuestions.stream()
                        .map(q -> ExcelImportRequestDTO.ExcelQuestionDTO.builder()
                                .content(q.getContent())
                                .questionType(q.getQuestionType())
                                .skill(q.getSkill())
                                .cefrLevel(q.getCefrLevel())
                                .topic(q.getTopic())
                                .explanation(q.getExplanation())
                                .options(q.getOptions() != null ? q.getOptions().stream()
                                        .map(o -> ExcelImportRequestDTO.OptionDTO.builder()
                                                .title(o.getTitle())
                                                .correct(o.getCorrect())
                                                .build())
                                        .toList() : null)
                                .correctAnswer(q.getCorrectAnswer())
                                .matchLeft(q.getMatchLeft())
                                .matchRight(q.getMatchRight())
                                .correctPairs(q.getCorrectPairs())
                                .build())
                        .toList();

                valid.add(ExcelParseGroupResultDTO.ValidGroupRowDTO.builder()
                        .rowIndex(2)
                        .group(ExcelQuestionGroupDTO.builder()
                                .passage(passage)
                                .skill(skill != null ? skill.toUpperCase() : "READING")
                                .cefrLevel(cefr != null ? cefr.toUpperCase() : "B1")
                                .topic(topic)
                                .audioUrl(audioUrl)
                                .imageUrl(imageUrl)
                                .explanation(explanation)
                                .questions(excelQuestions)
                                .build())
                        .questions(childQuestions)
                        .build());
            }
        }

        return ExcelParseGroupResultDTO.builder()
                .valid(valid)
                .errors(errors)
                .totalRows(valid.size() + errors.size())
                .build();
    }

    @Override
    @Transactional
    public int importQuestionGroups(ExcelImportGroupRequestDTO request, String userEmail) throws Exception {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userEmail));

        ExcelQuestionGroupDTO groupDto = request.getGroup();
        if (groupDto.getQuestions() == null || groupDto.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("Bộ câu hỏi phải có ít nhất 1 câu hỏi con.");
        }

        QuestionGroup group = QuestionGroup.builder()
                .groupContent(groupDto.getPassage())
                .audioUrl(groupDto.getAudioUrl())
                .imageUrl(groupDto.getImageUrl())
                .skill(groupDto.getSkill() != null ? groupDto.getSkill().toUpperCase() : "READING")
                .cefrLevel(groupDto.getCefrLevel() != null ? groupDto.getCefrLevel().toUpperCase() : "B1")
                .topic(groupDto.getTopic())
                .explanation(groupDto.getExplanation())
                .status("DRAFT")
                .user(user)
                .build();
        questionGroupRepository.save(group);

        int saved = 0;
        for (ExcelImportRequestDTO.ExcelQuestionDTO qdto : groupDto.getQuestions()) {
            String skill = qdto.getSkill() != null ? qdto.getSkill().toUpperCase() : group.getSkill();
            String cefr = qdto.getCefrLevel() != null ? qdto.getCefrLevel().toUpperCase() : group.getCefrLevel();
            checkDuplicate(qdto.getContent(), skill, cefr);

            Question question = Question.builder()
                    .questionGroup(group)
                    .user(user)
                    .content(qdto.getContent())
                    .questionType(qdto.getQuestionType() != null ? qdto.getQuestionType() : "MULTIPLE_CHOICE_SINGLE")
                    .skill(qdto.getSkill() != null ? qdto.getSkill().toUpperCase() : group.getSkill())
                    .cefrLevel(qdto.getCefrLevel() != null ? qdto.getCefrLevel().toUpperCase() : group.getCefrLevel())
                    .topic(qdto.getTopic() != null ? qdto.getTopic() : group.getTopic())
                    .explanation(qdto.getExplanation())
                    .status("DRAFT")
                    .source("EXPERT_BANK")
                    .createdMethod("EXCEL_IMPORTED")
                    .build();
            questionRepository.save(question);

            if (qdto.getOptions() != null && !qdto.getOptions().isEmpty()) {
                int idx = 0;
                for (ExcelImportRequestDTO.OptionDTO opt : qdto.getOptions()) {
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(question)
                            .title(opt.getTitle())
                            .correctAnswer(opt.getCorrect())
                            .orderIndex(idx++)
                            .build());
                }
            } else if (qdto.getMatchLeft() != null && qdto.getMatchRight() != null
                    && qdto.getCorrectPairs() != null) {
                List<String> left = qdto.getMatchLeft();
                List<String> right = qdto.getMatchRight();
                List<Integer> pairs = qdto.getCorrectPairs();
                for (int i = 0; i < left.size(); i++) {
                    int rightIdx = pairs.get(i) - 1;
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(question).title(left.get(i)).correctAnswer(false)
                            .orderIndex(i).matchTarget(right.get(rightIdx)).build());
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(question).title(right.get(rightIdx)).correctAnswer(false)
                            .orderIndex(left.size() + i).build());
                }
            }
            saved++;
        }
        return saved;
    }

    private String trim(String s) {
        return s != null ? s.trim() : null;
    }

    private static class ValidationException extends Exception {
        ValidationException(String msg) { super(msg); }
    }

    /** Block duplicate: same content + skill + CEFR level. */
    private void checkDuplicate(String content, String skill, String cefrLevel) {
        if (questionRepository.existsByContentIgnoreCaseAndSkillAndCefrLevel(content, skill, cefrLevel)) {
            throw new InvalidDataException("Câu hỏi đã tồn tại: [" + skill + "/" + cefrLevel + "] " + truncate(content, 80));
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
