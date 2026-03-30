# Spec: AI Generate Question & Import Excel cho Question Bank

**Project:** NovaLMS — Expert Question Bank
**Date:** 2026-03-28
**Status:** Revised (v2 — fixed review issues)

---

## 1. Mục tiêu

Thêm 2 chức năng vào trang **Question Bank** của Expert:

1. **AI Generate Question** — Dùng OpenAI GPT sinh câu hỏi tự động với 2 chế độ: nhanh (topic + số lượng) và context-aware (gắn module).
2. **Import Excel** — Download template POI theo loại câu hỏi, upload file đã fill để import với validate real-time + sửa lỗi trực tiếp trên UI.

---

## 2. Tổng quan cấu trúc

```
Expert Question Bank (question-bank.html)
├── [Tạo thủ công] → question-create.html (hiện tại, không đổi)
├── [🤖 Sinh bằng AI] → AI Generation Panel (tab/modal mới)
└── [📥 Import Excel]  → Import Panel (tab/modal mới)
```

Hai panel độc lập, cùng nằm trên trang Question Bank.

---

## 3. Chức năng 1: AI Generate Question

### 3.1 Hai chế độ sinh

#### Chế độ Nhanh (Quick Generate)
- **Input:** Topic (text) + Số lượng (1-50)
- **AI tự quyết định:** skill, CEFR level, loại câu hỏi phù hợp với topic
- Prompt gửi lên OpenAI: mô tả topic, số lượng, yêu cầu đa dạng loại câu hỏi + đa dạng CEFR

#### Chế độ Context-aware (Module Context)
- **Input:** Chọn Module (dropdown từ danh sách module của Expert) + Số lượng
- **AI đọc context:** đọc nội dung lesson/module gắn module đó, sinh câu hỏi phù hợp ngữ cảnh
- Prompt gửi lên OpenAI: inject nội dung module, topic, số lượng

### 3.2 Luồng hoạt động

```
User chọn chế độ
        │
        ▼
Nhập topic / chọn module + số lượng
        │
        ▼
Bấm "Sinh câu hỏi" → Gọi POST /api/v1/expert/questions/ai/generate
        │
        ▼
OpenAI API trả JSON array questions (chưa lưu DB)
        │
        ▼
Hiển thị Preview: danh sách câu hỏi với checkbox chọn/bỏ
        │
        ▼
User tick chọn câu muốn giữ
        │
        ▼
Bấm "Lưu các câu đã chọn" → POST /api/v1/expert/questions/ai/import
        │
        ▼
Tạo Question entities (status = DRAFT, source = EXPERT_BANK)
Câu không chọn → bỏ, không lưu
        │
        ▼
Redirect về /expert/question-bank
```

### 3.3 API Endpoints

#### `POST /api/v1/expert/questions/ai/generate`
- **Mục đích:** Sinh câu hỏi bằng AI (chưa lưu DB)
- **Auth:** Expert đã login
- **Request Body:**
  ```json
  {
    "topic": "Travel and Tourism",
    "quantity": 10,
    "moduleId": null,          // null = Quick mode, có = Context-aware
    "skill": null,             // null = AI tự chọn
    "cefrLevel": null,         // null = AI tự chọn
    "questionTypes": null      // null = đa dạng, ["MULTIPLE_CHOICE_SINGLE", ...]
  }
  ```
- **Response:**
  ```json
  {
    "code": 200,
    "message": "Sinh câu hỏi thành công",
    "data": {
      "questions": [
        {
          "content": "...",
          "questionType": "MULTIPLE_CHOICE_SINGLE",
          "skill": "READING",
          "cefrLevel": "B1",
          "topic": "Travel",
          "explanation": "...",
          "options": [
            { "title": "A", "correct": false },
            { "title": "B", "correct": true }
          ]
        }
      ]
    }
  }
  ```
- **Lỗi:** 400 (prompt quá dài), 500 (API lỗi), 401 (chưa login)

- **AI Response Validation (server-side):**
  - Parse JSON → nếu không parse được → retry 1 lần → vẫn lỗi → trả 500
  - Mỗi question object phải có: `content` (không trống), `questionType` (enum hợp lệ), `skill` (enum hợp lệ), `cefrLevel` (enum hợp lệ)
  - MC questions phải có ≥ 2 options, mỗi option có `title` (không trống)
  - MC questions phải có ≥ 1 option có `correct = true`; ≤ số option cho multi-select
  - Fill-in-blank phải có `correctAnswer` (không trống)
  - Matching phải có ≥ 2 cặp ghép
  - Nếu AI trả ít hơn `quantity` yêu cầu → vẫn trả về những gì có, kèm warning
  - Nếu 0 question hợp lệ → trả 400 "AI không sinh được câu hỏi hợp lệ"

#### `POST /api/v1/expert/questions/ai/import`
- **Mục đích:** Lưu các câu hỏi đã chọn từ preview vào DB
- **Request Body:**
  ```json
  {
    "questions": [ /* array từ generate response, đã được user chọn */ ]
  }
  ```
- **Response:**
  ```json
  {
    "code": 201,
    "message": "Đã lưu 7 câu hỏi",
    "data": { "savedCount": 7 }
  }
  ```

### 3.4 AI Prompt Strategy

#### Quick Mode Prompt
```
Bạn là giáo viên tiếng Anh chuyên nghiệp. Hãy sinh {quantity} câu hỏi
về chủ đề "{topic}" cho học sinh.

Yêu cầu:
- Đa dạng loại: MULTIPLE_CHOICE_SINGLE, MULTIPLE_CHOICE_MULTI, FILL_IN_BLANK, MATCHING
- Đa dạng CEFR: A1-C2
- Đa dạng skill: LISTENING, READING, WRITING, SPEAKING
- Mỗi câu hỏi phải có: content, questionType, skill, cefrLevel, topic, explanation, options

Trả về JSON array, không có text khác.
```

#### Context-aware Mode Prompt
```
Module "{moduleName}" có nội dung:
{lesson content}

Hãy sinh {quantity} câu hỏi phù hợp với nội dung trên.
[Các yêu cầu tương tự như trên]
```

#### Module Content Fetching (Context-aware)
- **Service:** Gọi `ModuleRepository.findById(moduleId)` để lấy module entity
- **Lesson content:** Gọi `LessonRepository.findByModuleId(moduleId)` → lấy tất cả lesson của module đó
- **Prompt injection:** Concatenate tất cả lesson content (title + description) vào prompt, giới hạn 8000 ký tự
- **Fallback:** Nếu module không có lesson → dùng module name + topic làm prompt, báo warning cho user

---

## 4. Chức năng 2: Import Excel

### 4.1 Luồng hoạt động

```
1. User bấm "Import Excel" → mở panel/modal
2. User chọn loại câu hỏi (VD: Multiple Choice)
3. Bấm "Tải Template" → POST /api/v1/expert/questions/excel/template?type={type}
   → Server tạo .xlsx bằng Apache POI → trả về file download
4. User mở Excel, fill data theo cấu trúc mẫu
5. User upload file → POST /api/v1/expert/questions/excel/parse
   → Server parse từng dòng, trả về result
6. Dòng hợp lệ → thêm vào danh sách preview
7. Dòng lỗi → hiển thị trên UI với thông báo lỗi cụ thể
8. User sửa lỗi trực tiếp trên form → bấm "Kiểm tra lại"
9. Bấm "Lưu tất cả" → POST /api/v1/expert/questions/excel/import
10. Tạo Question + AnswerOption entities → redirect bank
```

### 4.2 API Endpoints

#### `GET /api/v1/expert/questions/excel/template`
- **Param:** `type` = loại câu hỏi (MULTIPLE_CHOICE_SINGLE, FILL_IN_BLANK, MATCHING, WRITING, SPEAKING)
- **Response:** File .xlsx (Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet)
- **Header:** Content-Disposition: attachment; filename=template-{type}.xlsx

#### `POST /api/v1/expert/questions/excel/parse`
- **Request:** Multipart file upload
- **Response:**
  ```json
  {
    "code": 200,
    "data": {
      "valid": [ /* questions hợp lệ */ ],
      "errors": [
        {
          "row": 4,
          "message": "Thiếu đáp án đúng",
          "data": { /* raw data của dòng đó */ }
        }
      ]
    }
  }
  ```

#### `POST /api/v1/expert/questions/excel/import`
- **Request Body:**
  ```json
  {
    "questions": [ /* tất cả questions (valid + đã sửa) */ ]
  }
  ```
- **Response:**
  ```json
  {
    "code": 201,
    "message": "Đã import 15 câu hỏi",
    "data": { "savedCount": 15 }
  }
  ```

### 4.3 Cấu trúc Template Excel (Apache POI)

#### Multiple Choice (Single/Multi)
| Content | Option A | Option B | Option C | Option D | CorrectAnswer | Skill | CEFR | Topic | Explanation |
|---------|----------|----------|----------|----------|---------------|-------|------|-------|-------------|

#### Fill in the Blank
| Content (dùng ___ cho blank) | CorrectAnswer | Skill | CEFR | Topic | Explanation |

#### Matching
| Content | MatchLeft | MatchRight | CorrectPairs | Skill | CEFR | Topic |

#### Writing
| Content | Skill | CEFR | Topic | Explanation |

#### Speaking
| Content | AudioUrl | Skill | CEFR | Topic |

**Ghi chú:**
- `MatchLeft`: "Apple|Banana|Car" (danh sách phân tách bằng |, không cần prefix A:, B:)
- `MatchRight`: "Quả táo|Quả chuối|Ô tô" (danh sách phân tách bằng |, số thứ tự tự động 1,2,3)
- `CorrectAnswer` (MC): "A" hoặc "A,B" (multi, phân tách bằng dấu phẩy)
- `CorrectPairs` (Matching): "1,2,3" — thứ tự MatchLeft ghép lần lượt với MatchRight theo index
  - VD: MatchLeft="Apple|Banana|Car", MatchRight="Quả táo|Quả chuối|Ô tô", CorrectPairs="1,2,3" → Apple→Quả táo, Banana→Quả chuối, Car→Ô tô

**Định dạng row cho MATCHING:** Mỗi dòng = 1 câu hỏi ghép nối hoàn chỉnh. Nếu MatchLeft có 3 items, MatchRight phải có đúng 3 items.

### 4.5 Speaking Audio Upload

- **Khi import Speaking questions:** Expert nhập `AudioUrl` = đường dẫn public URL đến file audio (host trên Cloudinary hoặc CDN khác)
- **Validation:** `AudioUrl` phải là URL hợp lệ (bắt đầu bằng `http://` hoặc `https://`), hoặc để trống nếu chưa có audio
- **Upload flow (recommend):** Expert upload audio file trước qua Cloudinary → lấy URL paste vào Excel
- **Không hỗ trợ upload file audio trực tiếp trong Excel import** — Expert phải upload riêng

### 4.6 Transaction & Partial Save Policy

**AI Import (`/ai/import`):**
- Wrap trong `@Transactional`
- Tất cả questions hợp lệ hoặc không có câu nào — không partial save
- Nếu 1 câu fail (DB constraint, validation) → rollback toàn bộ → trả lỗi cụ thể

**Excel Import (`/excel/import`):**
- Tương tự: `@Transactional`, rollback on any failure
- User phải fix lỗi trên UI trước khi submit lại
- Không lưu partial — hoặc tất cả thành công, hoặc không có gì

### 4.7 OpenAI Model

- **Model:** `gpt-4o-mini` (cân bằng chi phí + chất lượng)
- **Fallback:** Nếu `gpt-4o-mini` lỗi → retry với `gpt-4o` (1 lần duy nhất)
- **Temperature:** `0.7` — đủ sáng tạo nhưng vẫn đúng format
- **Max tokens:** `4096` cho response
- **Timeout:** `30 giây` cho mỗi request

### 4.8 Excel Validation Rules (bổ sung)

- Dòng thiếu Content → "Dòng {n}: Nội dung câu hỏi trống"
- MC thiếu CorrectAnswer → "Dòng {n}: Chưa chọn đáp án đúng"
- MC multi-select không có đáp án đúng → "Dòng {n}: Multi-select phải có ít nhất 1 đáp án đúng"
- MC multi-select tất cả đúng → "Dòng {n}: Multi-select không thể có tất cả đáp án đều đúng"
- CorrectAnswer không khớp option → "Dòng {n}: Đáp án 'X' không tồn tại trong các lựa chọn"
- CEFR/Skill không hợp lệ → "Dòng {n}: CEFR '{value}' không hợp lệ (A1-C2)"
- MATCHING: số item MatchLeft ≠ số item MatchRight → "Dòng {n}: Số cặp ghép không khớp ({left} trái, {right} phải)"
- MATCHING: CorrectPairs không đúng định dạng → "Dòng {n}: CorrectPairs phải là số nguyên dương cách nhau bằng dấu phẩy, VD: 1,2,3"
- Sửa trực tiếp: khi user sửa dòng lỗi trên UI, re-validate bằng JS trước khi submit lại

---

## 5. Database Schema

Không thêm bảng mới.

**Quyết định:**
- `source = "EXPERT_BANK"` cho tất cả câu hỏi (tạo tay, AI, Excel import)
- Thêm column `created_method` (varchar 20): `"MANUAL" | "AI_GENERATED" | "EXCEL_IMPORTED"`
- `status = "DRAFT"` cho cả AI sinh và Excel import (Expert review trước khi publish)

Nếu cần truy vấn riêng AI-generated questions, filter theo `created_method`.

---

## 6. UI/UX Design

### 6.1 Vị trí trên Question Bank

Thêm 2 buttons vào header của trang question-bank.html:
```html
<button class="btn btn-outline-primary" onclick="openAIGenPanel()">
  <i class="bi bi-robot"></i> Sinh bằng AI
</button>
<button class="btn btn-outline-success" onclick="openImportPanel()">
  <i class="bi bi-file-earmark-arrow-up"></i> Import Excel
</button>
```

### 6.2 AI Generation Panel

- **Dạng:** Modal full-screen hoặc side-panel
- **Tabs:** "Sinh nhanh" | "Theo Module"
- **Quick tab:** Topic input + Số lượng + button "Sinh"
- **Module tab:** Module dropdown + Số lượng + button "Sinh"
- **Loading state:** Spinner khi gọi API
- **Preview:** Danh sách cards, mỗi card có:
  - Checkbox chọn/bỏ
  - Nội dung câu hỏi
  - Type badge, Skill badge, CEFR badge
  - Options preview (nếu có)
  - "Chi tiết" để expand
- **Footer:** "Đã chọn X/Y câu" + button "Lưu các câu đã chọn"

### 6.3 Import Excel Panel

- **Dạng:** Modal
- **Step 1:** Chọn loại câu hỏi → bấm "Tải Template"
- **Step 2:** Upload file đã fill
- **Step 3 (Preview):**
  - Bảng hiển thị tất cả dòng
  - Dòng hợp lệ: màu xanh, có checkbox
  - Dòng lỗi: màu đỏ, có thông báo lỗi, form sửa inline
- **Footer:** "Dòng hợp lệ: X | Lỗi: Y" + button "Lưu tất cả"

---

## 7. Dependencies mới

```xml
<!-- OpenAI GPT -->
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>api</artifactId>
    <version>0.18.2</version>
</dependency>

<!-- Apache POI (Excel) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

---

## 8. Cấu trúc file mới

```
service/
├── AISQuestionService.java          (interface)
├── AISQuestionServiceImpl.java       (OpenAI logic)
└── ExcelImportService.java           (Excel parse + template)

controller/
└── ExpertQuestionController.java     (thêm 5 endpoint mới)

dto/request/
├── AIGenerateRequestDTO.java
└── ExcelImportRequestDTO.java

dto/response/
├── AIGenerateResponseDTO.java
└── ExcelParseResultDTO.java

util/
├── AIQuestionPromptBuilder.java       (build prompt cho từng mode)
└── ExcelTemplateGenerator.java        (Apache POI tạo template)

templates/expert/
├── fragments/
│   └── ai-gen-panel.html             (AI generation panel fragment)
│   └── import-excel-panel.html        (Import Excel panel fragment)
```

---

## 9. Error Handling

| Scenario | Xử lý |
|----------|--------|
| OpenAI API timeout | Trả 504, hiển thị "AI quá tải, thử lại sau" |
| OpenAI API invalid response | Log lỗi, trả 500, user retry |
| Excel parse lỗi format | Trả lỗi cụ thể dòng nào, không crash |
| Excel file quá lớn (>5MB) | Trả 413, thông báo giới hạn |
| Module không tồn tại | Trả 404 |
| Unauthorized | Trả 401, redirect login |

---

## 10. Security

- Tất cả endpoint đều require authentication (Expert role)
- Rate limit: tối đa 10 lần gọi AI generate/phút/user — dùng **in-memory ConcurrentHashMap** với key = userId, value = timestamp list. Cleanup expired entries mỗi lần check.
- Input sanitize cho topic, content (ngăn prompt injection)
- File upload: chỉ chấp nhận `.xlsx`, giới hạn 5MB
- Không lưu API key OpenAI trong code — dùng environment variable
