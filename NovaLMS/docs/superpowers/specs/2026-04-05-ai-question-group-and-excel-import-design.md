# SPEC: AI Generate Question Group + Enhanced Excel Import

**Date:** 2026-04-05
**Author:** Claude
**Status:** Draft
**Related:** `question-bank.html`, `question-create.html`, `ExpertQuestionController`, `AIQuestionServiceImpl`, `ExcelQuestionImportServiceImpl`, `ExpertQuestionServiceImpl`

---

## 1. Mục tiêu

Cập nhật chức năng AI Generate và Excel Import để hỗ trợ:

1. **AI sinh Question Group** (Passage + câu hỏi con) — flat JSON response
2. **AI import Question Group** vào DB
3. **Excel import Question Group** (1 file = 1 group, section-based)
4. Nâng cấp metadata cho cả AI single-question import và Excel single-question import

---

## 2. Data Model — Nhắc lại

### QuestionGroup
```
groupId, groupContent (TEXT), audioUrl, imageUrl,
skill, cefrLevel, topic, explanation,
status, userId (FK→User), createdAt,
questions[] → child Question records
```

### Question (single / child)
```
questionId, groupId (FK→QuestionGroup, nullable), userId, content (TEXT),
questionType, skill, cefrLevel, topic, explanation,
audioUrl, imageUrl, status,
source (EXPERT_BANK|TEACHER_PRIVATE),
createdMethod (MANUAL|AI_GENERATED|EXCEL_IMPORTED),
answerOptions[] → cascade ALL
```

---

## 3. Phần 1 — AI Generate Question Group

### 3.1 Backend Changes

#### A. New DTO: `AIGenerateGroupRequestDTO`

```java
@Data @Builder
public class AIGenerateGroupRequestDTO {
    private String topic;          // free-text topic (quick mode)
    private Integer moduleId;      // OR module-based
    private Integer quantity;       // số câu hỏi con (1–20)
    private String skill;
    private String cefrLevel;
    private java.util.List<String> questionTypes; // loại câu hỏi con
}
```

#### B. New DTO: `AIGenerateGroupResponseDTO`

```java
@Data @Builder
public class AIGenerateGroupResponseDTO {
    private String passage;           // nội dung bài học / kịch bản nghe
    private String audioUrl;         // URL audio nếu có
    private String imageUrl;         // URL image nếu có
    private String skill;
    private String cefrLevel;
    private String topic;
    private String explanation;      // giải thích chung cho passage
    private List<QuestionDTO> questions; // danh sách câu hỏi con
    private String warning;
}
```

#### C. New DTO: `AIImportGroupRequestDTO`

```java
@Data @Builder
public class AIImportGroupRequestDTO {
    private String passage;
    private String audioUrl;
    private String imageUrl;
    private String skill;
    private String cefrLevel;
    private String topic;
    private String explanation;
    private String status;            // DRAFT or PUBLISHED
    private List<AIImportRequestDTO.AIQuestionDTO> questions;
}
```

#### D. AIQuestionPromptBuilder — thêm method

```java
// Prompt yêu cầu flat JSON: passage + questions[]
public String buildGroupPrompt(String topic, int questionCount, List<String> questionTypes)

Example output:
{
  "passage": "...",
  "audioUrl": null,
  "imageUrl": null,
  "skill": "READING",
  "cefrLevel": "B1",
  "topic": "Education",
  "explanation": "This passage discusses...",
  "questions": [
    { "content": "...", "questionType": "MULTIPLE_CHOICE_SINGLE", "options": [...], ... },
    ...
  ]
}
```

Rules trong prompt:
- Passage phải bằng **tiếng Anh**
- Tối thiểu 2 câu hỏi con, tối đa 20
- Mỗi câu hỏi con phải thuộc 1 trong các type hợp lệ
- Audio/imageUrl có thể null

#### E. AIQuestionServiceImpl — thêm method

```java
public AIGenerateGroupResponseDTO generateGroup(AIGenerateGroupRequestDTO request, String userEmail)

// Logic:
// 1. Check rate limit
// 2. Build group prompt
// 3. Call Groq
// 4. Parse flat JSON { passage, questions[] }
// 5. Validate mỗi question con
// 6. Trả về AIGenerateGroupResponseDTO
```

#### F. ExpertQuestionController — thêm endpoints

```java
// POST /api/v1/expert/questions/ai/generate/group
@PostMapping("/ai/generate/group")
public ResponseData<AIGenerateGroupResponseDTO> generateQuestionGroup(
        @Valid @RequestBody AIGenerateGroupRequestDTO request, Principal principal) {
    AIGenerateGroupResponseDTO result = aiQuestionService.generateGroup(request, ...);
    return ResponseData.success("Sinh bộ câu hỏi thành công", result);
}

// POST /api/v1/expert/questions/ai/import/group
@PostMapping("/ai/import/group")
public ResponseData<Void> importAIQuestionGroup(
        @Valid @RequestBody AIImportGroupRequestDTO request, Principal principal) {
    int saved = questionService.saveAIQuestionGroup(request, email);
    return new ResponseData<>(HttpStatus.CREATED.value(), "Đã lưu bộ câu hỏi với " + saved + " câu con.", null);
}
```

#### G. ExpertQuestionServiceImpl — thêm method

```java
public int saveAIQuestionGroup(AIImportGroupRequestDTO request, String email)
// Logic:
// 1. Tạo QuestionGroup với passage, audioUrl, skill, cefr...
// 2. Với mỗi câu hỏi con: tạo Question (groupId=parent, source=EXPERT_BANK, createdMethod=AI_GENERATED)
// 3. Lưu AnswerOptions cho mỗi câu con
// 4. Trả về số câu hỏi con đã lưu
```

---

### 3.2 Frontend Changes (`question-bank.html`)

#### AI Modal — Thêm tab "Bộ câu hỏi"

```html
<!-- Tabs -->
<ul class="nav nav-tabs mb-3">
    <li class="nav-item"><button class="nav-link active" data-bs-toggle="tab" data-bs-target="#tabQuick">Sinh nhanh</button></li>
    <li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabModule">Theo Module</button></li>
    <li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabGroup">Bộ câu hỏi</button></li>
</ul>

<!-- Tab content thêm: -->
<div class="tab-pane fade" id="tabGroup">
    <div class="row g-3">
        <div class="col-md-4">
            <label class="form-label fw-bold">Chủ đề <span class="text-danger">*</span></label>
            <input type="text" id="groupTopic" class="form-control" placeholder="VD: Climate Change">
        </div>
        <div class="col-md-3">
            <label class="form-label fw-bold">Kỹ năng</label>
            <select id="groupSkill" class="form-select">
                <option value="READING" selected>Reading</option>
                <option value="LISTENING">Listening</option>
                <option value="WRITING">Writing</option>
                <option value="SPEAKING">Speaking</option>
            </select>
        </div>
        <div class="col-md-2">
            <label class="form-label fw-bold">CEFR</label>
            <select id="groupCefr" class="form-select">
                <option value="A1">A1</option>...<option value="B1" selected>B1</option>...
            </select>
        </div>
        <div class="col-md-3">
            <label class="form-label fw-bold">Số câu hỏi con</label>
            <input type="number" id="groupQty" class="form-control" min="2" max="20" value="5">
        </div>
    </div>
    <div class="text-end mt-3">
        <button class="btn btn-primary" onclick="generateAIGroup()">
            <i class="bi bi-lightning-charge me-1"></i> Sinh bộ câu hỏi
        </button>
    </div>
</div>
```

#### JavaScript — Thêm functions

```javascript
let groupPreview = null;

async function generateAIGroup() {
    const topic = document.getElementById('groupTopic').value.trim();
    const skill = document.getElementById('groupSkill').value;
    const cefr = document.getElementById('groupCefr').value;
    const qty = parseInt(document.getElementById('groupQty').value);
    if (!topic) { alert('Vui lòng nhập chủ đề.'); return; }

    const body = { topic, skill, cefrLevel: cefr, quantity: qty };
    document.getElementById('aiLoading').style.display = 'block';
    // Hide single preview, show group preview area
    try {
        const res = await fetch('/api/v1/expert/questions/ai/generate/group', {
            method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)
        });
        const result = await res.json();
        if (!res.ok) { alert(result.message || 'Lỗi'); return; }
        groupPreview = result.data;
        renderAIGroupPreview();
    } finally { document.getElementById('aiLoading').style.display = 'none'; }
}

function renderAIGroupPreview() {
    // Hiển thị passage + danh sách câu hỏi con
    // Checkbox chọn từng câu hỏi con để import
    // Checkbox chọn toàn bộ passage
    // Button "Lưu bộ câu hỏi đã chọn" → importAIGroup()
}

async function importAIGroup() {
    const selectedQs = groupPreview.questions.filter(q => q.selected);
    if (selectedQs.length === 0) { alert('Vui lòng chọn ít nhất 1 câu hỏi con.'); return; }
    const body = {
        passage: groupPreview.passage,
        audioUrl: groupPreview.audioUrl,
        imageUrl: groupPreview.imageUrl,
        skill: groupPreview.skill,
        cefrLevel: groupPreview.cefrLevel,
        topic: groupPreview.topic,
        explanation: groupPreview.explanation,
        status: 'DRAFT',
        questions: selectedQs
    };
    const res = await fetch('/api/v1/expert/questions/ai/import/group', {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)
    });
    const result = await res.json();
    if (!res.ok) { alert(result.message || 'Lỗi'); return; }
    alert(result.message);
    aiModal.hide();
    loadQuestions();
}
```

---

## 4. Phần 2 — Nâng cấp AI Single-Question Import

### 4.1 Backend

`saveAIQuestions()` trong `ExpertQuestionServiceImpl` đã có:
- `source = "EXPERT_BANK"` ✓
- `createdMethod = "AI_GENERATED"` ✓
- `status = "DRAFT"` ✓

**Cần thêm:**
- `skill` và `cefrLevel` được set từ AI response (đã có từ DTO)
- Topic từ AI response (đã có)

→ **Không cần thay đổi code** — `saveAIQuestions()` đã đúng.

### 4.2 Frontend — AI Modal Quick/Module tabs

Cần truyền thêm `skill`, `cefrLevel`, `topic` vào body khi gọi AI generate và import:

```javascript
// generateAI() body thêm:
const skill = document.getElementById('aiSkill')?.value || 'READING';
const cefr = document.getElementById('aiCefr')?.value || 'B1';
body.skill = skill;
body.cefrLevel = cefr;
body.questionTypes = ['MULTIPLE_CHOICE_SINGLE', 'FILL_IN_BLANK']; // optional filter
```

Thêm dropdown Skill và CEFR vào AI modal Quick/Module tabs.

---

## 5. Phần 3 — Excel Import Question Group

### 5.1 Excel Template Structure (1 file = 1 group)

File Excel cấu trúc section-based:

```
Row 1 (header): | PASSAGE | SKILL | CEFR | TOPIC | AUDIO_URL | IMAGE_URL | EXPLANATION |
Row 2 (data):   | Nội dung passage... | READING | B1 | Education | | | Giải thích... |
Row 3 (blank separator)
Row 4 (header): | CONTENT | TYPE | OPTA | OPTB | OPTC | OPTD | CORRECT | MATCH_LEFT | MATCH_RIGHT | PAIRS | SUB_SKILL | SUB_CEFR | SUB_TOPIC | SUB_EXPLANATION |
Row 5+:         | Q1 content... | MC_SINGLE | A | B | C | D | A | | | | | | |
                | Q2 content... | FILL_BLANK | | | | | Answer | | | | | | |
                | Q3 content... | MATCHING | | | | | | left1 | right1 | 1 | | | |
```

**Quy tắc:**
- Row 1 = Group metadata (PASSAGE, SKILL, CEFR, TOPIC, AUDIO_URL, IMAGE_URL, EXPLANATION)
- Row 2 = Dữ liệu group
- Row 3 = Blank separator
- Row 4 = Header cho child questions (tương tự template single question nhưng thêm SUB_* columns)
- Row 5+ = Từng câu hỏi con

### 5.2 Backend Changes

#### A. New DTOs

```java
// ExcelQuestionGroupDTO - map từ parsed file
@Data @Builder
public class ExcelQuestionGroupDTO {
    private String passage;
    private String skill;
    private String cefrLevel;
    private String topic;
    private String audioUrl;
    private String imageUrl;
    private String explanation;
    private List<ExcelQuestionDTO> questions;
}

// ExcelImportGroupRequestDTO
@Data @Builder
public class ExcelImportGroupRequestDTO {
    private ExcelQuestionGroupDTO group;
}

// ExcelParseGroupResultDTO
@Data @Builder
public class ExcelParseGroupResultDTO {
    private List<ValidGroupRowDTO> valid;
    private List<ErrorRowDTO> errors;
    private int totalRows;
}
```

#### B. ExcelQuestionImportServiceImpl — thêm method

```java
public ExcelParseGroupResultDTO parseGroupFile(MultipartFile file) throws Exception
// Logic:
// 1. Row 1, 2 = group metadata
// 2. Tìm row 4 (header child questions), Row 5+ = child questions
// 3. Parse mỗi row con → ExcelQuestionDTO
// 4. Validate: passage không trống, ít nhất 1 câu hỏi con hợp lệ
// 5. Trả về ExcelParseGroupResultDTO

@Transactional
public int importQuestionGroups(ExcelImportGroupRequestDTO request, String userEmail)
// Logic:
// 1. Tạo QuestionGroup
// 2. Với mỗi child: tạo Question(groupId, source=EXPERT_BANK, createdMethod=EXCEL_IMPORTED)
// 3. Trả về số child questions
```

#### C. ExpertQuestionController — thêm endpoints

```java
// GET /api/v1/expert/questions/excel/template?type=QUESTION_GROUP
@GetMapping("/excel/template")
public ResponseEntity<byte[]> downloadTemplate(@RequestParam String type) {
    // type = QUESTION_GROUP → generate group template
}

// POST /api/v1/expert/questions/excel/parse-group
@PostMapping(value = "/excel/parse-group", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseData<ExcelParseGroupResultDTO> parseGroupExcel(
        @RequestParam("file") MultipartFile file) {
    // Gọi excelService.parseGroupFile()
}

// POST /api/v1/expert/questions/excel/import-group
@PostMapping("/excel/import-group")
public ResponseData<Void> importGroupExcel(
        @Valid @RequestBody ExcelImportGroupRequestDTO request, Principal principal) {
    int saved = excelService.importQuestionGroups(request, getEmail(principal));
    return new ResponseData<>(HttpStatus.CREATED.value(), "Đã import bộ câu hỏi với " + saved + " câu con.", null);
}
```

#### D. ExcelTemplateGenerator — thêm method

```java
public byte[] generateGroupTemplate()
// Tạo file Excel với:
// Sheet1 = Group template (passage, skill, cefr, topic, audio, image, explanation)
// Sheet2 = Child questions (header row + 3 sample rows)
```

### 5.3 Frontend (`question-bank.html` Import Modal)

Thêm lựa chọn type "Question Group" trong dropdown:

```html
<option value="QUESTION_GROUP">Bộ câu hỏi (Passage + Sub-questions)</option>
```

Khi user chọn `QUESTION_GROUP`:
1. Download template → `GET /excel/template?type=QUESTION_GROUP`
2. Upload file → `POST /excel/parse-group` → render preview với passage + child questions
3. Confirm import → `POST /excel/import-group`

---

## 6. Tổng hợp thay đổi theo file

### Backend (Java)

| File | Thay đổi |
|------|----------|
| `dto/request/AIGenerateGroupRequestDTO.java` | **Mới** |
| `dto/request/AIImportGroupRequestDTO.java` | **Mới** |
| `dto/request/ExcelImportGroupRequestDTO.java` | **Mới** |
| `dto/response/ExcelParseGroupResultDTO.java` | **Mới** |
| `util/AIQuestionPromptBuilder.java` | Thêm `buildGroupPrompt()` |
| `service/AIQuestionServiceImpl.java` | Thêm `generateGroup()` |
| `service/impl/ExpertQuestionServiceImpl.java` | Thêm `saveAIQuestionGroup()` |
| `service/impl/ExcelQuestionImportServiceImpl.java` | Thêm `parseGroupFile()`, `importQuestionGroups()` |
| `util/ExcelTemplateGenerator.java` | Thêm `generateGroupTemplate()` |
| `controller/ExpertQuestionController.java` | Thêm 4 endpoint mới |

### Frontend (HTML/JS)

| File | Thay đổi |
|------|----------|
| `templates/expert/question-bank.html` | Thêm tab "Bộ câu hỏi" trong AI modal + lựa chọn `QUESTION_GROUP` trong Import modal + JS functions |

---

## 7. Error Handling

- AI Groq error → trả về 500 với message lỗi rõ ràng
- AI rate limit → trả về 429 với message "Bạn đã vượt giới hạn..."
- Excel parse error → trả về error rows như hiện tại, không block toàn bộ
- Import group với 0 câu con hợp lệ → reject với message "Bộ câu hỏi phải có ít nhất 1 câu hỏi con hợp lệ"
- Missing passage → reject với message "Passage không được trống"

---

## 8. Security

- Tất cả AI import và Excel import yêu cầu xác thực (Principal)
- `source = "EXPERT_BANK"` — câu hỏi tạo từ AI/Excel luôn vào shared bank
- `createdMethod = "AI_GENERATED"` / `"EXCEL_IMPORTED"` — phân biệt nguồn gốc
- File upload limit: 5MB (đã có)
