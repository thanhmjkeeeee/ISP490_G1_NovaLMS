# Quiz Create Form Redesign — Design Spec

**Date:** 2026-04-05
**Status:** Approved
**Author:** Claude

---

## 1. Mục tiêu

Đập đi xây lại `quiz-create.html` và `quiz-edit.html` cho Expert. Một trang duy nhất với dropdown category, JS ẩn/hiện section theo loại quiz. Backend giữ nguyên — chỉ sửa validation publish.

---

## 2. Quiz Categories

| Category | Label | Ai tạo | Bắt buộc 4 kĩ năng |
|---|---|---|---|
| `ENTRY_TEST` | Bài kiểm tra đầu vào | Expert | Không |
| `COURSE_QUIZ` | Bài kiểm tra khóa học | Expert | **Có** |
| `COURSE_ASSIGNMENT` | Bài tập lớn khóa học | Expert | **Có** |

LESSON_QUIZ (Teacher tạo) xử lý sau.

---

## 3. Cấu trúc `quiz-create.html`

### 3.1 Header & Category Selector

- Header: tiêu đề "Tạo Quiz / Bài kiểm tra", breadcrumb Expert
- Dropdown `quizCategory` (always visible):
  - Option 1: `ENTRY_TEST` — "Bài kiểm tra đầu vào"
  - Option 2: `COURSE_QUIZ` — "Bài kiểm tra khóa học"
  - Option 3: `COURSE_ASSIGNMENT` — "Bài tập lớn khóa học"
- `onCategoryChange()` — JS fires on change, hides/shows sections

### 3.2 Step 1: Metadata (always visible)

| Field | Visibility |
|---|---|
| Tên quiz *(required)* | Always |
| Mô tả | Always |
| Khóa học (`courseId`) | Shown for: `COURSE_QUIZ`, `COURSE_ASSIGNMENT` |

### 3.3 Step 2: Cấu hình nâng cao (always visible)

| Field | Notes |
|---|---|
| Thời gian (phút) | |
| Điểm đạt (%) | |
| Số lần làm lại | |
| Thứ tự câu hỏi (FIXED/RANDOM) | |
| Cho xem đáp án sau nộp | Toggle switch |
| Thời gian theo kĩ năng (L/R/S/W) | **Only for `COURSE_ASSIGNMENT`** |
| Entry Test warning | **Only for `ENTRY_TEST`** |

### 3.4 Step 3: Chọn câu hỏi (always visible)

Layout 2 cột:
- **Left (30%):** Đề thi hiện tại — danh sách câu hỏi đã thêm, group theo skill
- **Right (70%):** Ngân hàng câu hỏi — filter + table + pagination

#### UI cho ENTRY_TEST:
- Không có tabs skill. Chọn câu hỏi phẳng từ ngân hàng. Không bắt buộc 4 skill.

#### UI cho COURSE_QUIZ và COURSE_ASSIGNMENT:
- **4 tabs: LISTENING | READING | SPEAKING | WRITING**
- Mỗi tab: bank browser lọc theo skill đó
- Đã thêm hiện số câu trên badge tab
- **COURSE_ASSIGNMENT**: thêm "Per-skill time limits" ở Step 2
- **COURSE_ASSIGNMENT**: preview hiện 4 skill cards với ✅/⚠️

#### Validation trước xuất bản:

| Category | Rule |
|---|---|
| `ENTRY_TEST` | ≥1 câu hỏi |
| `COURSE_QUIZ` | **Đủ 4 skill** — mỗi skill ≥1 câu |
| `COURSE_ASSIGNMENT` | **Đủ 4 skill** + per-skill time set |

---

## 4. Cấu trúc `quiz-edit.html`

Load quiz từ `GET /api/v1/expert/quizzes/{quizId}`.

- Metadata fields: title, description, course (readonly category)
- Config fields: time, pass score, max attempts, order, show answer
- **Dynamic section theo category:**
  - `ENTRY_TEST` → flat question list
  - `COURSE_QUIZ` / `COURSE_ASSIGNMENT` → 4 tabs L/R/S/W, mỗi tab hiện số câu
- Nút: Lưu thay đổi + Quay lại

---

## 5. Backend Changes

### 5.1 Thêm validation ở publish

`ExpertQuizServiceImpl.publishAssignment()` hiện tại chỉ validate cho ASSIGNMENT. Cần mở rộng để reject COURSE_QUIZ chưa đủ 4 skill.

```
PATCH /api/v1/expert/quizzes/{quizId}/publish
```

Validation rule mới:
- Nếu `quizCategory = COURSE_QUIZ`: kiểm tra đủ 4 skill (mỗi skill ≥1 câu)
- Nếu `quizCategory = COURSE_ASSIGNMENT`: kiểm tra đủ 4 skill + per-skill time
- Nếu `quizCategory = ENTRY_TEST`: chỉ cần ≥1 câu

### 5.2 Thêm endpoint lấy số câu/skill

Dùng endpoint có sẵn: `GET /api/v1/expert/quizzes/{quizId}/skills`

Response:
```json
{
  "LISTENING": { "questionCount": 5 },
  "READING":   { "questionCount": 3 },
  "SPEAKING":  { "questionCount": 0 },
  "WRITING":   { "questionCount": 0 }
}
```

---

## 6. API Contract (Frontend → Backend)

### Tạo quiz
```
POST /api/v1/expert/quizzes
Body: {
  "title": "...",
  "description": "...",
  "quizCategory": "COURSE_QUIZ",
  "courseId": 1,
  "timeLimitMinutes": 60,
  "passScore": 70.0,
  "maxAttempts": 2,
  "questionOrder": "FIXED",
  "showAnswerAfterSubmit": true,
  "isSequential": false,
  "skillOrder": ["LISTENING","READING","SPEAKING","WRITING"],
  "timeLimitPerSkill": null
}
```

### Thêm câu hỏi (flat — cho tất cả category)
```
POST /api/v1/expert/quizzes/{quizId}/section/questions
Body: {
  "questionIds": [1, 2, 3],
  "skill": "LISTENING",
  "itemType": "SINGLE"
}
```

### Publish
```
PATCH /api/v1/expert/quizzes/{quizId}/publish
→ 200: OK
→ 400: { "message": "COURSE_QUIZ requires all 4 skills before publishing" }
```

---

## 7. Files Changed

| File | Action |
|---|---|
| `templates/expert/quiz-create.html` | **Rewrite hoàn toàn** |
| `templates/expert/quiz-edit.html` | **Rewrite hoàn toàn** |
| `service/impl/ExpertQuizServiceImpl.java` | Thêm validation COURSE_QUIZ đủ 4 skill ở publish |

---

## 8. Out of Scope

- LESSON_QUIZ (Teacher tạo cho lesson) — xử lý sau
- MODULE_QUIZ, MODULE_ASSIGNMENT — bỏ
- Entry test wizard hybrid
- Student take-quiz page
