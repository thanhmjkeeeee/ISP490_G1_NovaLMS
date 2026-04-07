# Kịch Bản Test Manual — NovaLMS (Specs 2026-04-04-001 → 006 + QuestionGroupWizard)

> **Ngày:** 2026-04-04
> **Nguồn:** Tổng hợp từ SPEC 001–006 + QuestionGroupWizard
> **Phạm vi:** Expert tạo Assignment, Teacher tạo Lesson Quiz, Expert duyệt câu hỏi, Student làm Assignment, Teacher chấm điểm Assignment, Teacher chấm điểm Lesson Quiz, Expert tạo QuestionGroup qua Wizard

---

## MỤC LỤC

1. [SPEC 001 — Expert tạo Assignment](#spec-001--expert-tạo-assignment)
2. [SPEC 002 — Teacher tạo Lesson Quiz](#spec-002--teacher-tạo-lesson-quiz)
3. [SPEC 003 — Expert duyệt câu hỏi của Teacher](#spec-003--expert-duyệt-câu-hỏi-của-teacher)
4. [SPEC 004 — Student làm Assignment](#spec-004--student-làm-assignment)
5. [SPEC 005 — Teacher chấm điểm Assignment](#spec-005--teacher-chấm-điểm-assignment)
6. [SPEC 006 — Teacher chấm điểm Lesson Quiz](#spec-006--teacher-chấm-điểm-lesson-quiz)
7. [QuestionGroup Wizard — Expert tạo QuestionGroup](#questiongroup-wizard--expert-tạo-questiongroup)

---

## Ghi chú chung trước khi test

| Ký hiệu | Ý nghĩa |
|---|---|
| ✅ | Bước thành công |
| ❌ | Lỗi — cần báo bug |
| ⏸ | Chờ hệ thống xử lý nền (AI grading, notification…) |
| ⚠️ | Cảnh báo — hành vi không như mong đợi nhưng không crash |

**Tài khoản test cần chuẩn bị:**
- `expert@novalms.com` — role EXPERT
- `teacher@novalms.com` — role TEACHER, đã enroll vào ít nhất 1 class
- `student@novalms.com` — role STUDENT, đã enroll vào class của teacher
- (Tùy test) tài khoản `teacher2@novalms.com` — teacher KHÔNG enroll vào class của `teacher@novalms.com`

---

## SPEC 001 — Expert tạo Assignment

### Vai trò: EXPERT

---

### TC-001-01: Tạo Course Assignment — Step 1 (Cấu hình)

**Mục tiêu:** Expert tạo Assignment loại COURSE_ASSIGNMENT ở bước cấu hình

**Bước thực hiện:**

1. Đăng nhập với tài khoản EXPERT
2. Truy cập `/expert/assignment/create?category=COURSE_ASSIGNMENT`
3. Nhập các trường:
   - **Title:** "Test Assignment TC-001-01"
   - **Description:** "Bài kiểm tra 4 kỹ năng"
   - **Course:** Chọn một khóa học đang active
   - **Điểm đạt:** 70
   - **Số lần làm tối đa:** 2
   - **Thời gian LISTENING:** (để trống)
   - **Thời gian READING:** (để trống)
   - **Thời gian SPEAKING:** 2
   - **Thời gian WRITING:** 30
4. Click **"Tiếp theo: Thêm câu hỏi LISTENING →"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Redirect đến `/expert/assignment/{quizId}/skill/LISTENING` | ✅ URL chứa quizId, hiển thị step LISTENING |
| 2 | Quiz được tạo trong DB với status = `DRAFT` | ✅ Kiểm tra DB: `quiz.status = 'DRAFT'` |
| 3 | `is_sequential = true` | ✅ DB: `quiz.is_sequential = 1` |
| 4 | `skill_order = ["LISTENING","READING","SPEAKING","WRITING"]` | ✅ JSON đúng thứ tự |
| 5 | `time_limit_per_skill` lưu đúng SPEAKING=2, WRITING=30 | ✅ JSON: `{"SPEAKING":2,"WRITING":30}` |
| 6 | `quiz_category = COURSE_ASSIGNMENT` | ✅ |

---

### TC-001-02: Tạo Module Assignment — Step 1

**Mục tiêu:** Expert tạo Assignment loại MODULE_ASSIGNMENT

**Bước thực hiện:**

1. Truy cập `/expert/assignment/create?category=MODULE_ASSIGNMENT`
2. Chọn **Module** (thay vì Course)
3. Nhập title, để trống timer
4. Submit

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Redirect đến step LISTENING | ✅ |
| 2 | DB: `quiz_category = 'MODULE_ASSIGNMENT'` | ✅ |
| 3 | Module selector hiển thị khi category=MODULE_ASSIGNMENT | ✅ |

---

### TC-001-03: Validation — Step 1 không chọn Course

**Mục tiêu:** Form validation hoạt động đúng ở Step 1

**Bước thực hiện:**

1. Mở form tạo COURSE_ASSIGNMENT
2. Bỏ trống **Title** → Submit
3. Bỏ trống **Course** → Submit
4. Nhập **passScore = 150** → Submit
5. Nhập **maxAttempts = 0** → Submit

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Bỏ trống Title → thông báo lỗi "Title không được để trống" | ✅ |
| 2 | Bỏ trống Course → thông báo lỗi validation | ✅ |
| 3 | passScore > 100 → thông báo lỗi validation | ✅ |
| 4 | maxAttempts < 1 → thông báo lỗi validation | ✅ |
| 5 | Không submit được khi validation fail (API trả 400) | ✅ |

---

### TC-001-04: Thêm câu hỏi LISTENING — từ Question Bank

**Mục tiêu:** Expert browse question bank và thêm câu hỏi vào phần LISTENING

**Bước thực hiện:**

1. Từ step LISTENING (`/expert/assignment/{quizId}/skill/LISTENING`)
2. Tìm kiếm trong panel "Chọn từ ngân hàng câu hỏi"
3. Filter theo skill = LISTENING, status = PUBLISHED
4. Check 2 câu hỏi → Click **"Thêm câu hỏi đã chọn"**
5. Quan sát số đếm "Đã thêm: X câu"

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Panel bank hiển thị câu hỏi đã filter LISTENING, status PUBLISHED | ✅ |
| 2 | Question type hợp lệ: MULTIPLE_CHOICE_SINGLE, MULTIPLE_CHOICE_MULTI, FILL_IN_BLANK | ✅ |
| 3 | Sau khi thêm, counter "Đã thêm" tăng lên | ✅ |
| 4 | DB: `quiz_question` có 2 rows mới với `skill = 'LISTENING'` | ✅ |
| 5 | API trả về số lượng câu đã thêm | ✅ |

---

### TC-001-05: Tạo câu hỏi LISTENING inline (mới)

**Mục tiêu:** Expert tạo câu hỏi mới trực tiếp trong Assignment và thêm vào

**Bước thực hiện:**

1. Ở step LISTENING, panel phải "Tạo câu hỏi mới"
2. Tạo câu hỏi MULTIPLE_CHOICE_SINGLE (có audio upload):
   - Nội dung: "What did the speaker say about the meeting?"
   - Audio upload: tải file `.mp3` hợp lệ
   - 4 đáp án, đánh dấu 1 đáp án đúng
   - CEFR: B1
3. Submit question mới

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Câu hỏi được lưu với `status = PENDING_REVIEW` (hoặc EXPERT_BANK tùy spec) | ✅ Spec nói EXPERT_BANK → kiểm tra spec thực tế |
| 2 | Câu hỏi được tự động thêm vào Assignment (quiz_question) | ✅ |
| 3 | Audio URL lưu vào Cloudinary, link hợp lệ | ✅ |
| 4 | Counter "Đã thêm" tăng | ✅ |
| 5 | Question type không hợp lệ cho LISTENING (VD: WRITING) → bị từ chối | ✅ |

---

### TC-001-06: Di chuyển giữa 4 skill sections

**Mục tiêu:** Expert điều hướng qua 4 bước skill đúng thứ tự

**Bước thực hiện:**

1. Đang ở step LISTENING — kiểm tra step indicator (LISTENING active)
2. Thêm ≥1 câu hỏi LISTENING
3. Click link đến READING (sidebar hoặc button)
4. Thêm ≥1 câu hỏi READING (type: MULTIPLE_CHOICE_SINGLE, FILL_IN_BLANK, MATCHING)
5. Chuyển đến SPEAKING
6. Tạo 1 câu hỏi SPEAKING (prompt + audio prompt)
7. Chuyển đến WRITING
8. Tạo 1 câu hỏi WRITING (prompt)

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Step indicator highlight đúng step hiện tại | ✅ |
| 2 | Không thể skip section (VD: từ LISTENING nhảy thẳng WRITING) | ⚠️ Kiểm tra — spec không bắt buộc block UI |
| 3 | Mỗi section chỉ chấp nhận question type phù hợp | ✅ |
| 4 | Sau khi thêm đủ 4 section, counter mỗi section ≥ 1 | ✅ |

---

### TC-001-07: Preview Assignment — thiếu câu hỏi

**Mục tiêu:** Trang preview hiển thị đúng trạng thái khi thiếu câu hỏi

**Bước thực hiện:**

1. Tạo assignment nhưng chỉ thêm câu LISTENING + READING (bỏ SPEAKING + WRITING)
2. Truy cập `/expert/assignment/{quizId}/preview`
3. Quan sát danh sách 4 skill sections

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | SPEAKING section hiển thị "⚠️ Cần thêm câu hỏi" (màu đỏ) | ✅ |
| 2 | WRITING section hiển thị "⚠️ Cần thêm câu hỏi" (màu đỏ) | ✅ |
| 3 | Button "Xuất bản" bị DISABLED | ✅ |
| 4 | Alert hiển thị "Không thể xuất bản! Thiếu: SPEAKING, WRITING" | ✅ |

---

### TC-001-08: Preview Assignment — đủ câu hỏi, xuất bản

**Mục tiêu:** Assignment đủ điều kiện và xuất bản thành công

**Bước thực hiện:**

1. Tạo assignment mới (đủ 4 skill, mỗi skill ≥1 câu)
2. Truy cập preview → tất cả sections đều ✅
3. Click **"Xuất bản"**
4. Xác nhận dialog

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Dialog xác nhận xuất hiện | ✅ |
| 2 | Sau confirm: API `PATCH /publish` được gọi | ✅ |
| 3 | DB: `quiz.status = 'PUBLISHED'` | ✅ |
| 4 | DB: `quiz.is_open = false` (teacher phải mở sau) | ✅ |
| 5 | Redirect đến `/expert/assignment-management` | ✅ |
| 6 | Toast/alert "Xuất bản thành công!" | ✅ |

---

### TC-001-09: Preview Assignment — chỉnh sửa sau preview

**Mục tiêu:** Expert quay lại sửa từ trang preview

**Bước thực hiện:**

1. Từ preview page, click **"← Quay lại sửa"**
2. Quay lại step WRITING, thêm 1 câu hỏi
3. Quay lại preview → kiểm tra số câu đã tăng

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Quay lại sửa không mất dữ liệu đã thêm | ✅ |
| 2 | Thêm câu sau đó quay preview → số câu cập nhật đúng | ✅ |

---

### TC-001-10: Assignment list page

**Mục tiêu:** Trang quản lý Assignment hiển thị đúng danh sách

**Bước thực hiện:**

1. Truy cập `/expert/assignment-management`
2. Tạo 3 assignments (DRAFT, PUBLISHED, ARCHIVED)
3. Quan sát danh sách

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Chỉ hiển thị COURSE_ASSIGNMENT + MODULE_ASSIGNMENT (không có quiz thường) | ✅ |
| 2 | Mỗi row hiển thị: title, category, status, số câu hỏi, ngày tạo | ✅ |
| 3 | Filter theo status (DRAFT / PUBLISHED / ARCHIVED) hoạt động | ✅ |
| 4 | Click row → mở preview/edit | ✅ |

---

### TC-001-11: Xóa Assignment đã xuất bản

**Mục tiêu:** Không thể xóa Assignment đã có kết quả thi

**Bước thực hiện:**

1. Tạo và publish 1 assignment
2. Mở bằng student → làm bài → submit (tạo QuizResult)
3. Quay lại Expert → thử xóa assignment đó

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Button "Xóa" bị ẩn HOẶC click xóa → thông báo "Không thể xóa bài đã có kết quả" | ✅ |
| 2 | DB không có record bị xóa | ✅ |

---

### TC-001-12: Bảo mật — Teacher không được tạo Assignment

**Mục tiêu:** Phân quyền: chỉ EXPERT mới tạo được Assignment

**Bước thực hiện:**

1. Đăng nhập với `teacher@novalms.com`
2. Thử truy cập `/expert/assignment/create`
3. Thử gọi API `POST /api/v1/expert/assignments`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Truy cập URL → redirect login HOẶC 403 Forbidden | ✅ |
| 2 | API → 403 Forbidden | ✅ |

---

## SPEC 002 — Teacher tạo Lesson Quiz

### Vai trò: TEACHER

---

### TC-002-01: Tạo Lesson Quiz — Step 1 (Cấu hình)

**Mục tiêu:** Teacher tạo Lesson Quiz cho 1 lesson

**Bước thực hiện:**

1. Đăng nhập với `teacher@novalms.com`
2. Truy cập lesson đã enroll → click "Tạo Quiz"
3. Hoặc truy cập `/teacher/lesson/{lessonId}/quiz/create`
4. Nhập:
   - **Title:** "Quiz Bài 3 — Ngữ pháp"
   - **Description:** "Ôn tập thì quá khứ"
   - **Class:** Chọn class đã enroll
   - **Time limit:** 30 phút
   - **Pass score:** 70%
   - **Max attempts:** 3
   - **Question order:** RANDOM
5. Submit

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Redirect đến `/teacher/quiz/{quizId}/build` (Step 2: Content Builder) | ✅ |
| 2 | DB: `quiz_category = 'LESSON_QUIZ'` | ✅ |
| 3 | DB: `is_sequential = false` | ✅ |
| 4 | DB: `status = 'DRAFT'` | ✅ |
| 5 | `time_limit_minutes = 30`, `pass_score = 70`, `max_attempts = 3` | ✅ |

---

### TC-002-02: Browse Expert Question Bank — thêm câu hỏi

**Mục tiêu:** Teacher browse Expert bank (chỉ PUBLISHED) và thêm câu vào quiz

**Bước thực hiện:**

1. Đang ở Step 2 (Content Builder)
2. Filter: Skill = LISTENING, CEFR = B1, Source = EXPERT_BANK
3. Tìm kiếm keyword "environment"
4. Check 2 câu hỏi → Click **"Thêm"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Chỉ hiển thị câu hỏi `status = PUBLISHED` và `source = EXPERT_BANK` | ✅ |
| 2 | Không hiển thị câu hỏi `PENDING_REVIEW` của teacher khác | ✅ |
| 3 | Sau khi thêm, sidebar hiển thị câu trong quiz | ✅ |
| 4 | DB: `quiz_question` có 2 rows mới | ✅ |
| 5 | Badge hiển thị "🟢 Đã xuất bản" cho câu đã thêm | ✅ |

---

### TC-002-03: Tạo câu hỏi inline (PENDING_REVIEW)

**Mục tiêu:** Teacher tạo câu hỏi mới → tự động thuộc trạng thái chờ duyệt

**Bước thực hiện:**

1. Ở Step 2, click **"Tạo câu hỏi mới"**
2. Tạo câu hỏi SPEAKING:
   - Prompt: "Describe your favorite vacation destination"
   - CEFR: B1
   - Audio prompt: upload file
3. Sau khi lưu → hộp thoại "Thêm câu hỏi này vào quiz hiện tại?" → **Có**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | DB: `question.status = 'PENDING_REVIEW'` | ✅ |
| 2 | DB: `question.source = 'TEACHER_PRIVATE'` | ✅ |
| 3 | DB: `question.user_id = teacher_id` | ✅ |
| 4 | Sidebar hiển thị "⚠️ Chờ duyệt" badge cho câu này | ✅ |
| 5 | Câu hỏi được thêm vào quiz_question | ✅ |

---

### TC-002-04: Tạo câu hỏi inline — chọn "Không" thêm vào quiz

**Mục tiêu:** Teacher tạo câu nhưng không thêm vào quiz

**Bước thực hiện:**

1. Tạo câu hỏi inline mới
2. Hộp thoại "Thêm vào quiz?" → **Không**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Câu hỏi vẫn được lưu (PENDING_REVIEW) | ✅ |
| 2 | Câu hỏi KHÔNG xuất hiện trong quiz | ✅ |
| 3 | Teacher có thể xem câu đã lưu ở `/teacher/my-questions` | ✅ |

---

### TC-002-05: AI Generate — tạo câu hỏi bằng AI

**Mục tiêu:** Teacher dùng AI tạo câu hỏi (qua Expert AI flow → vẫn là PENDING_REVIEW)

**Bước thực hiện:**

1. Ở Step 2, tìm tab/button AI Generate
2. Nhập topic: "Daily routines", quantity: 5
3. Click **"Generate"**
4. Sau khi có kết quả → Import tất cả vào quiz

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API `/ai/generate` được gọi | ✅ |
| 2 | Kết quả hiển thị danh sách câu đã tạo | ✅ |
| 3 | Import: mỗi câu có `status = PENDING_REVIEW`, `source = TEACHER_PRIVATE` | ✅ |
| 4 | Tất cả được thêm vào quiz_question | ✅ |
| 5 | Sidebar cập nhật với badge "⏳ Chờ duyệt" | ✅ |

---

### TC-002-06: Finish — xem tổng hợp trước khi publish

**Mục tiêu:** Trang finish hiển thị đúng skill breakdown + pending count

**Bước thực hiện:**

1. Thêm câu đủ các skill (LISTENING + SPEAKING, trong đó 1 câu SPEAKING là PENDING_REVIEW)
2. Truy cập `/teacher/quiz/{quizId}/finish`
3. Quan sát trang tổng hợp

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Hiển thị tiêu đề + mô tả quiz | ✅ |
| 2 | Skill breakdown: mỗi skill hiển thị số câu | ✅ |
| 3 | Hiển thị "⚠️ X câu hỏi đang chờ phê duyệt" | ✅ |
| 4 | Total questions, total points đúng | ✅ |
| 5 | Button "Lưu bản nháp" → gọi PUT, không đổi status | ✅ |

---

### TC-002-07: Publish Lesson Quiz — không có câu hỏi

**Mục tiêu:** Validation ngăn publish khi quiz trống

**Bước thực hiện:**

1. Tạo quiz mới (chưa thêm câu)
2. Truy cập finish page → click **"Xuất bản"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API trả về 400 error | ✅ |
| 2 | Thông báo "Quiz phải có ít nhất 1 câu hỏi" | ✅ |
| 3 | Quiz không chuyển sang PUBLISHED | ✅ |

---

### TC-002-08: Publish Lesson Quiz — đủ câu (có PENDING_REVIEW)

**Mục tiêu:** Quiz publish được dù có PENDING_REVIEW questions

**Bước thực hiện:**

1. Quiz có ≥1 câu hỏi (có thể là PENDING_REVIEW)
2. Click **"Xuất bản"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | DB: `quiz.status = 'PUBLISHED'` | ✅ |
| 2 | PENDING_REVIEW questions vẫn trong quiz | ✅ |
| 3 | Redirect đến quiz list HOẶC hiển thị thành công | ✅ |

---

### TC-002-09: Teacher tạo quiz cho lesson không thuộc class mình

**Mục tiêu:** Validation ngăn teacher tạo quiz cho lesson/class không enroll

**Bước thực hiện:**

1. Tạo URL `/teacher/lesson/{lessonId_others}/quiz/create` với lessonId thuộc class khác
2. Thử submit form

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API trả 403 Forbidden HOẶC validation error | ✅ |
| 2 | DB không tạo quiz mới | ✅ |

---

### TC-002-10: Toggle isOpen — mở quiz cho student

**Mục tiêu:** Teacher toggle isOpen để cho phép student làm bài

**Bước thực hiện:**

1. Quiz đang ở trạng thái PUBLISHED, isOpen = false
2. Teacher tìm quiz trong danh sách → toggle isOpen

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Toggle button hiển thị đúng trạng thái | ✅ |
| 2 | Sau toggle ON: `quiz.is_open = true` | ✅ |
| 3 | Student thấy quiz trong danh sách bài tập | ✅ |

---

### TC-002-11: Bảo mật — Expert không được dùng Teacher quiz creation

**Mục tiêu:** Role separation

**Bước thực hiện:**

1. Đăng nhập với `expert@novalms.com`
2. Thử truy cập `/teacher/lesson/{lessonId}/quiz/create`
3. Thử gọi API `POST /api/v1/teacher/lessons/{lessonId}/quizzes`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Truy cập → 403 | ✅ |
| 2 | API → 403 | ✅ |

---

## SPEC 003 — Expert duyệt câu hỏi của Teacher

### Vai trò: EXPERT

---

### TC-003-01: Truy cập trang Approval Queue

**Mục tiêu:** Expert vào trang duyệt câu hỏi

**Bước thực hiện:**

1. Đăng nhập Expert
2. Truy cập `/expert/question-approval`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Trang hiển thị danh sách câu `PENDING_REVIEW` + `TEACHER_PRIVATE` | ✅ |
| 2 | Expert dashboard có badge "Câu hỏi chờ phê duyệt: X" | ✅ |

---

### TC-003-02: Filter Approval Queue

**Mục tiêu:** Expert lọc queue theo skill, CEFR, teacher, date, keyword

**Bước thực hiện:**

1. Đang ở `/expert/question-approval`
2. Filter: Skill = SPEAKING
3. Filter: CEFR = B1
4. Filter: Teacher = "teacher@novalms.com"
5. Filter: Date range = tuần này
6. Search keyword: "vacation"

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Kết quả lọc đúng skill SPEAKING | ✅ |
| 2 | Kết quả đúng CEFR B1 | ✅ |
| 3 | Kết quả đúng teacher | ✅ |
| 4 | Kết quả trong khoảng ngày | ✅ |
| 5 | Kết quả chứa keyword "vacation" | ✅ |
| 6 | Pagination hoạt động (20/page) | ✅ |

---

### TC-003-03: Preview câu hỏi trong modal

**Mục tiêu:** Expert xem chi tiết câu hỏi trước khi duyệt

**Bước thực hiện:**

1. Click **"Preview"** trên 1 câu hỏi PENDING_REVIEW
2. Modal mở ra → kiểm tra nội dung

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Modal hiển thị đầy đủ: content, type badge, skill badge, CEFR badge, topic, tags | ✅ |
| 2 | Audio/image attachments có nút play/preview | ✅ |
| 3 | Đáp án đúng được highlight | ✅ |
| 4 | Passage hiển thị (nếu là group question) | ✅ |
| 5 | Thông tin "Created by: teacher name + email + date" | ✅ |

---

### TC-003-04: Approve câu hỏi (có note)

**Mục tiêu:** Expert phê duyệt câu hỏi

**Bước thực hiện:**

1. Click **"Phê duyệt"** trên 1 câu
2. Dialog xác nhận → nhập note: "Câu hỏi đạt yêu cầu"
3. Confirm

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API `POST /{id}/approve` được gọi | ✅ |
| 2 | DB: `question.status = 'PUBLISHED'` | ✅ |
| 3 | DB: `question.reviewer_id = expert_id` | ✅ |
| 4 | DB: `question.reviewed_at` = thời điểm hiện tại | ✅ |
| 5 | DB: `question.review_note = 'Câu hỏi đạt yêu cầu'` | ✅ |
| 6 | Notification được gửi đến teacher | ✅ |
| 7 | Câu biến mất khỏi queue | ✅ |

---

### TC-003-05: Reject câu hỏi (có note, không xóa)

**Mục tiêu:** Expert từ chối câu hỏi

**Bước thực hiện:**

1. Click **"Từ chối"** trên 1 câu
2. Dialog: nhập note "Câu hỏi không phù hợp tiêu chuẩn", không tick "Xóa"
3. Confirm

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API `POST /{id}/reject` được gọi với `{ delete: false }` | ✅ |
| 2 | DB: `question.status = 'DRAFT'` | ✅ |
| 3 | DB: `question.review_note` lưu đúng note | ✅ |
| 4 | Notification được gửi đến teacher | ✅ |
| 5 | Câu biến mất khỏi queue | ✅ |

---

### TC-003-06: Reject + Xóa câu hỏi

**Mục tiêu:** Expert từ chối và xóa câu hỏi

**Bước thực hiện:**

1. Click **"Từ chối"** → tick checkbox "Xóa câu hỏi" → Confirm

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API gọi với `{ delete: true }` | ✅ |
| 2 | DB: question bị xóa | ✅ |
| 3 | Notification gửi teacher (nội dung có ghi chú lý do) | ✅ |

---

### TC-003-07: Teacher nhận notification — Question APPROVED

**Mục tiêu:** Teacher thấy notification khi câu được duyệt

**Bước thực hiện:**

1. Expert approve câu của teacher
2. Teacher đăng nhập → kiểm tra notification / inbox

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Notification hiển thị: "Câu hỏi của bạn đã được phê duyệt" | ✅ |
| 2 | Notification có link về `/teacher/my-questions` | ✅ |
| 3 | Badge count trên notification tăng | ✅ |

---

### TC-003-08: Teacher nhận notification — Question REJECTED

**Mục tiêu:** Teacher nhận thông báo khi bị từ chối

**Bước thực hiện:**

1. Expert reject câu của teacher (có note)
2. Teacher đăng nhập

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Notification: "Câu hỏi của bạn bị từ chối" | ✅ |
| 2 | Message chứa `reviewNote` từ Expert | ✅ |
| 3 | Link về `/teacher/my-questions` | ✅ |

---

### TC-003-09: Teacher xem danh sách câu hỏi của mình

**Mục tiêu:** Teacher theo dõi trạng thái câu hỏi đã tạo

**Bước thực hiện:**

1. Teacher truy cập `/teacher/my-questions`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Hiển thị tất cả câu `TEACHER_PRIVATE` của teacher đó | ✅ |
| 2 | Badge "⏳ Chờ phê duyệt" cho PENDING_REVIEW | ✅ |
| 3 | Badge "🟢 Đã phê duyệt" cho PUBLISHED (kèm reviewer + ngày duyệt) | ✅ |
| 4 | Badge "🔴 Bị từ chối" cho DRAFT (kèm reviewNote) | ✅ |

---

### TC-003-10: Expert duyệt câu đã PUBLISHED

**Mục tiêu:** Validation ngăn approve câu đã duyệt rồi

**Bước thực hiện:**

1. Thử approve 1 câu đã có `status = PUBLISHED`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API trả 400 error: "Question is already approved" | ✅ |
| 2 | Status không thay đổi | ✅ |

---

## SPEC 004 — Student làm Assignment

### Vai trò: STUDENT

---

### TC-004-01: Student mở Assignment — tạo session

**Mục tiêu:** Student bắt đầu Assignment → hệ thống tạo AssignmentSession

**Bước thực hiện:**

1. Teacher đã publish + open 1 Assignment cho class của student
2. Student đăng nhập
3. Truy cập `/student/assignment/{quizId}`
4. Click **"Bắt đầu"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Trang welcome hiển thị: title, 4 skill sections, timer info | ✅ |
| 2 | DB: `assignment_session` được tạo với `status = 'IN_PROGRESS'` | ✅ |
| 3 | DB: `current_skill_index = 0` (LISTENING) | ✅ |
| 4 | DB: `section_statuses = {"LISTENING":"IN_PROGRESS","READING":"LOCKED",...}` | ✅ |
| 5 | Redirect đến `/student/assignment/session/{sessionId}/section/LISTENING` | ✅ |

---

### TC-004-02: LISTENING Section — hiển thị và trả lời

**Mục tiêu:** Student làm phần LISTENING

**Bước thực hiện:**

1. Đang ở section LISTENING
2. Kiểm tra: quiz-level timer hiển thị ở góc phải
3. Trả lời 2 câu (1 MC, 1 FILL_IN_BLANK)
4. Click **"Lưu tạm"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Chỉ hiển thị câu hỏi LISTENING | ✅ |
| 2 | Audio player hoạt động (nếu có audio) | ✅ |
| 3 | Quiz-level timer đếm ngược | ✅ |
| 4 | Auto-save thành công → toast "Đã lưu" | ✅ |
| 5 | DB: `section_answers` lưu JSON answers | ✅ |
| 6 | Answers được hiển thị lại sau khi reload section | ✅ |

---

### TC-004-03: LISTENING — nộp section

**Mục tiêu:** Student submit LISTENING section

**Bước thực hiện:**

1. Trả lời tất cả câu LISTENING
2. Click **"Nộp phần này"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API `POST /section/LISTENING/submit` được gọi | ✅ |
| 2 | MC/FILL questions auto-graded đúng | ✅ |
| 3 | DB: `section_statuses["LISTENING"] = 'COMPLETED'` | ✅ |
| 4 | DB: `current_skill_index = 1` (READING) | ✅ |
| 5 | DB: `section_statuses["READING"] = 'IN_PROGRESS'` | ✅ |
| 6 | Redirect đến READING section | ✅ |
| 7 | Quay lại LISTENING → thấy đã COMPLETED, không edit được | ✅ |

---

### TC-004-04: READING Section — đầy đủ workflow

**Mục tiêu:** Student làm phần READING

**Bước thực hiện:**

1. Từ READING section, trả lời các câu
2. Test MATCHING question (kéo thả / chọn)
3. Submit section

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Question types đúng: MC, FILL_IN_BLANK, MATCHING | ✅ |
| 2 | Image hiển thị (nếu passage có hình) | ✅ |
| 3 | MATCHING: cặp đáp án được ghép đúng | ✅ |
| 4 | Auto-graded sau submit | ✅ |
| 5 | Chuyển sang SPEAKING sau submit | ✅ |

---

### TC-004-05: SPEAKING Section — ghi âm

**Mục tiêu:** Student ghi âm trả lời SPEAKING

**Bước thực hiện:**

1. Truy cập SPEAKING section
2. Nghe audio prompt (nếu có)
3. Click **"🔴 START RECORDING"**
4. Timer SPEAKING bắt đầu đếm ngược
5. Ghi âm ~30 giây → click **"⏹ STOP"**
6. Preview → click **"🔄 Ghi âm lại"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Recording bắt đầu, giao diện chuyển sang trạng thái recording | ✅ |
| 2 | Timer SPEAKING đếm ngược từ `timeLimitPerSkill.SPEAKING` | ✅ |
| 3 | Stop → hiển thị audio player để preview | ✅ |
| 4 | Re-record → timer reset về full, recording mới thay thế | ✅ |
| 5 | Re-record không giới hạn số lần | ✅ |
| 6 | Audio upload lên Cloudinary → URL lưu trong `section_answers` | ✅ |

---

### TC-004-06: SPEAKING — timer hết giờ → auto submit

**Mục tiêu:** Timer hết → tự động submit

**Bước thực hiện:**

1. Bắt đầu recording
2. KHÔNG stop → để timer đếm về 0

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Timer về 0 → recording auto-stop | ✅ |
| 2 | Audio auto-upload | ✅ |
| 3 | Section auto-submitted | ✅ |
| 4 | API `POST /section/SPEAKING/submit` được gọi | ✅ |
| 5 | Chuyển sang WRITING section | ✅ |

---

### TC-004-07: SPEAKING — submit section

**Mục tiêu:** Student nộp SPEAKING section

**Bước thực hiện:**

1. Có recording đã upload
2. Click **"Nộp phần này"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API `POST /section/SPEAKING/submit` được gọi với Cloudinary URLs | ✅ |
| 2 | DB: SPEAKING section = COMPLETED | ✅ |
| 3 | Redirect đến WRITING section | ✅ |
| 4 | Audio URLs được gửi cho AI grading async | ⏸ |

---

### TC-004-08: WRITING Section — timer + submit

**Mục tiêu:** Student làm phần WRITING

**Bước thực hiện:**

1. Ở WRITING section, kiểm tra timer hiển thị `timeLimitPerSkill.WRITING`
2. Viết câu trả lời (textarea có character count)
3. Auto-save mỗi 30s
4. Click **"Nộp bài & Kết thúc"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Timer WRITING hiển thị countdown | ✅ |
| 2 | Auto-save → toast "Đã lưu" | ✅ |
| 3 | Submit → completion page hiển thị | ✅ |
| 4 | DB: `assignment_session.status = 'COMPLETED'` | ✅ |
| 5 | DB: `quiz_result` được tạo | ✅ |
| 6 | AI grading được fire cho SPEAKING + WRITING | ⏸ |

---

### TC-004-09: Quiz-level timer hết giờ

**Mục tiêu:** Tổng thời gian quiz hết → force submit toàn bộ

**Bước thực hiện:**

1. Đang ở section (VD: READING)
2. Quiz-level timer về 0

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | `POST /auto-submit` được gọi | ✅ |
| 2 | Tất cả sections còn lại → EXPIRED | ✅ |
| 3 | `session.status = COMPLETED` | ✅ |
| 4 | `quiz_result` được tạo với điểm phần đã làm | ✅ |
| 5 | Student được chuyển đến completion page | ✅ |

---

### TC-004-10: Resume — quay lại Assignment đang làm dở

**Mục tiêu:** Student resume Assignment sau khi đóng trình duyệt

**Bước thực hiện:**

1. Student đang làm (đã submit LISTENING + READING, đang ở SPEAKING)
2. Đóng trình duyệt
3. Mở lại → truy cập `/student/assignment/{quizId}`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Redirect đến SPEAKING section (đang ở `currentSkillIndex=2`) | ✅ |
| 2 | Answers đã lưu ở LISTENING + READING được hiển thị đúng | ✅ |
| 3 | SPEAKING timer chưa start (vì chưa vào section) | ✅ |

---

### TC-004-11: Student không được truy cập Assignment chưa open

**Mục tiêu:** Assignment `isOpen = false` → student bị chặn

**Bước thực hiện:**

1. Teacher chưa toggle open Assignment
2. Student truy cập `/student/assignment/{quizId}`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Thông báo "Bài đã đóng" HOẶC 403 | ✅ |

---

### TC-004-12: Hết lượt làm bài

**Mục tiêu:** Student hết attempts

**Bước thực hiện:**

1. Assignment có `maxAttempts = 1`
2. Student làm và submit lần 1
3. Student thử truy cập lại

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Redirect đến trang "Bạn đã hết lượt làm bài" | ✅ |
| 2 | Có link xem kết quả nếu đã có | ✅ |

---

### TC-004-13: Xem kết quả Assignment

**Mục tiêu:** Student xem điểm sau khi hoàn thành

**Bước thực hiện:**

1. Sau khi hoàn thành, student truy cập `/student/quiz/result/{resultId}`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | LISTENING: điểm auto-graded ✅ | ✅ |
| 2 | READING: điểm auto-graded ✅ | ✅ |
| 3 | SPEAKING: "⏳ Đang chấm..." HOẶC AI score + "Chờ giáo viên duyệt" | ✅ |
| 4 | WRITING: "⏳ Đang chấm..." HOẶC AI score + "Chờ giáo viên duyệt" | ✅ |
| 5 | Điểm tổng: phần auto-graded hiển thị, phần pending = "—" | ✅ |

---

### TC-004-14: Bảo mật — Student không enroll vào class

**Mục tiêu:** Phân quyền đúng

**Bước thực hiện:**

1. Tạo Assignment cho class A
2. Student đăng nhập, không enroll vào class A
3. Thử truy cập `/student/assignment/{quizId}`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | 403 Forbidden HOẶC "Bạn không có quyền làm bài này" | ✅ |

---

## SPEC 005 — Teacher chấm điểm Assignment

### Vai trò: TEACHER

---

### TC-005-01: Truy cập Grading Queue

**Mục tiêu:** Teacher xem danh sách submissions cần chấm

**Bước thực hiện:**

1. Teacher đăng nhập
2. Truy cập `/teacher/assignment/grading`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Hiển thị danh sách students + assignment | ✅ |
| 2 | Status badges đúng: 🎧✅ 📖✅ 🎤⏳ ✍️⏳ | ✅ |
| 3 | Filter Assignment / Class / Status hoạt động | ✅ |
| 4 | Chỉ hiển thị submissions của class teacher enroll | ✅ |

---

### TC-005-02: Grading LISTENING / READING — override điểm sai

**Mục tiêu:** Teacher override điểm auto-graded cho phần sai

**Bước thực hiện:**

1. Từ queue, click **"Chấm điểm →"** trên 1 student
2. Tab LISTENING: thấy câu đã auto-graded ✅/❌
3. Một câu student sai → nhập điểm override
4. Tab READING: thấy tương tự

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Auto-graded questions hiển thị đúng | ✅ |
| 2 | Câu sai hiển thị student answer vs correct answer | ✅ |
| 3 | Teacher nhập override → điểm thay đổi | ✅ |
| 4 | Section total tự recalculate | ✅ |

---

### TC-005-03: Grading SPEAKING — xem AI score, nhập điểm

**Mục tiêu:** Teacher nhập điểm SPEAKING (AI pre-populated)

**Bước thực hiện:**

1. Tab SPEAKING
2. Nghe audio recording của student
3. Xem AI Score + AI Feedback + AI Rubric breakdown
4. Nhập điểm teacher (giữ nguyên hoặc chỉnh sửa)
5. Nhập ghi chú

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Audio player hoạt động với recording URL | ✅ |
| 2 | AI Score hiển thị (VD: 7/10) | ✅ |
| 3 | AI Feedback hiển thị | ✅ |
| 4 | AI Rubric hiển thị breakdown | ✅ |
| 5 | Teacher input field pre-filled với AI score | ✅ |
| 6 | Ghi chú được lưu | ✅ |

---

### TC-005-04: Grading SPEAKING — AI chưa xong

**Mục tiêu:** UI hiển thị đúng khi AI đang chấm

**Bước thực hiện:**

1. Mở tab SPEAKING khi AI còn đang grading

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Hiển thị "⏳ Đang chấm tự động..." | ✅ |
| 2 | Audio vẫn phát được | ✅ |
| 3 | Teacher vẫn nhập điểm thủ công được | ✅ |

---

### TC-005-05: Submit Grading — đầy đủ

**Mục tiêu:** Teacher submit grading cho toàn bộ Assignment

**Bước thực hiện:**

1. Hoàn thành chấm tất cả 4 tabs
2. Click **"Nộp điểm"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API `POST /{resultId}/grade` được gọi với `sectionScores` JSON | ✅ |
| 2 | DB: `quiz_result.section_scores` lưu điểm từng phần | ✅ |
| 3 | DB: `quiz_result.score = tổng 4 phần` | ✅ |
| 4 | DB: `quiz_result.passed = true/false` (so với passScore) | ✅ |
| 5 | Queue row → badge "✅ Đã chấm xong" | ✅ |

---

### TC-005-06: Submit Grading — trước khi AI xong

**Mục tiêu:** Teacher vẫn chấm được khi AI chưa trả kết quả

**Bước thực hiện:**

1. AI SPEAKING chưa xong
2. Teacher nhập điểm thủ công → submit

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Submit thành công | ✅ |
| 2 | Kết quả AI sau đó được bỏ qua (teacher đã chấm) | ✅ |

---

### TC-005-07: Re-grade

**Mục tiêu:** Teacher có thể chấm lại sau khi đã chấm

**Bước thực hiện:**

1. Result đang ở trạng thái "Đã chấm"
2. Click "Chấm lại" → thay đổi điểm SPEAKING → Submit

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Điểm cũ bị ghi đè | ✅ |
| 2 | DB: scores cập nhật đúng | ✅ |

---

### TC-005-08: Bảo mật — Teacher không chấm được submission ngoài class mình

**Mục tiêu:** Phân quyền grading

**Bước thực hiện:**

1. Teacher A enroll class X
2. Assignment gắn với class Y (không enroll)
3. Teacher A thử truy cập `/teacher/assignment/grading/{resultId_of_classY}`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | 403 Forbidden | ✅ |

---

### TC-005-09: AI grading thất bại → Teacher chấm thủ công

**Mục tiêu:** UI xử lý AI fail gracefully

**Bước thực hiện:**

1. AI grading cho SPEAKING trả về lỗi / null
2. Teacher mở tab SPEAKING

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Badge "⚠️ Chấm tự động thất bại" | ✅ |
| 2 | Teacher buộc phải nhập điểm thủ công | ✅ |
| 3 | Submit vẫn hoạt động | ✅ |

---

## SPEC 006 — Teacher chấm điểm Lesson Quiz

### Vai trò: TEACHER

---

### TC-006-01: Grading Queue — phân biệt Assignment vs Lesson Quiz

**Mục tiêu:** Teacher phân biệt được 2 loại bài trong queue

**Bước thực hiện:**

1. Teacher có cả Assignment submissions và Lesson Quiz submissions
2. Truy cập `/teacher/quiz/grading`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Filter "Loại bài" có: "Tất cả", "Bài tập nhỏ" (Lesson Quiz), "Bài kiểm tra lớn" (Assignment) | ✅ |
| 2 | Kết quả lọc đúng theo loại | ✅ |

---

### TC-006-02: Grading Detail — dynamic skill tabs

**Mục tiêu:** Tabs chỉ hiển thị skill có trong quiz (không cố định 4)

**Bước thực hiện:**

1. Mở 1 Lesson Quiz result có 2 skills: LISTENING + SPEAKING

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Chỉ 2 tabs: LISTENING + SPEAKING (không có READING + WRITING) | ✅ |
| 2 | Tab LISTENING: auto-graded + override | ✅ |
| 3 | Tab SPEAKING: audio player + AI score + teacher input | ✅ |

---

### TC-006-03: Quiz chỉ có 1 skill (VD: LISTENING only)

**Mục tiêu:** UI xử lý quiz 1 skill

**Bước thực hiện:**

1. Teacher tạo quiz chỉ có LISTENING (1 câu)
2. Student làm → submit
3. Teacher mở grading detail

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Chỉ 1 tab LISTENING | ✅ |
| 2 | Submit grading hoạt động đúng | ✅ |

---

### TC-006-04: Submit grading cho Lesson Quiz

**Mục tiêu:** Teacher submit điểm Lesson Quiz

**Bước thực hiện:**

1. Chấm điểm các câu trong Lesson Quiz
2. Submit grading

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API `POST /{resultId}/grade` được gọi | ✅ |
| 2 | DB: `quiz_result.skill_scores` lưu JSON | ✅ |
| 3 | AI grading cho SPEAKING/WRITING được fire async | ⏸ |

---

### TC-006-05: Teacher my-grading dashboard

**Mục tiêu:** Trang tổng hợp công việc chấm điểm

**Bước thực hiện:**

1. Teacher truy cập `/teacher/my-grading`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Tabs: "Bài kiểm tra lớn" (Assignment) | "Bài tập nhỏ" (Lesson Quiz) | ✅ |
| 2 | Summary cards: số pending, hoàn thành hôm nay | ✅ |
| 3 | Quick links đến queue urgent nhất | ✅ |

---

## QuestionGroup Wizard — Expert tạo QuestionGroup

### Vai trò: EXPERT

---

### TC-WZ-01: Mở Wizard — Step 1

**Mục tiêu:** Expert vào wizard và thấy Step 1

**Bước thực hiện:**

1. Expert đăng nhập
2. Truy cập `/expert/questions/wizard`
3. Kiểm tra step indicator: Step 1 active

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Trang hiển thị Step 1: Group Setup | ✅ |
| 2 | Step indicator: Step 1 highlighted, 2-3-4 chưa active | ✅ |
| 3 | Session được clear khi load fresh wizard | ✅ |
| 4 | "Leave Wizard" button hiển thị | ✅ |

---

### TC-WZ-02: Step 1 — Toggle Passage-based vs Topic-based

**Mục tiêu:** Toggle giữa 2 chế độ

**Bước thực hiện:**

1. Chọn **"Passage-based"**
2. Quan sát: Passage textarea xuất hiện
3. Chọn **"Topic-based"**
4. Quan sát: Passage textarea ẩn

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Passage textarea hiện/ẩn đúng theo mode | ✅ |
| 2 | Mode được submit đúng | ✅ |

---

### TC-WZ-03: Step 1 — Validation

**Mục tiêu:** Validation Step 1

**Bước thực hiện:**

1. Bỏ trống Topic → Submit
2. Chọn Passage-based + bỏ trống Passage → Submit
3. Passage < 10 ký tự → Submit
4. Audio URL sai định dạng → Submit

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Bỏ Topic → thông báo "Topic is required" | ✅ |
| 2 | Passage-based + bỏ Passage → "PASSAGE_REQUIRED" | ✅ |
| 3 | Passage < 10 → "PASSAGE_TOO_SHORT" | ✅ |
| 4 | Audio URL sai → "INVALID_AUDIO_URL" | ✅ |

---

### TC-WZ-04: Step 2 — AI Generate

**Mục tiêu:** Expert dùng AI tạo câu hỏi trong wizard

**Bước thực hiện:**

1. Đã hoàn thành Step 1 (VD: skill=LISTENING, CEFR=B1)
2. Tab **"AI Generate"**
3. Nhập topic: "Travel", quantity: 5
4. Check types: MULTIPLE_CHOICE_SINGLE, FILL_IN_BLANK
5. Click **"Generate"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API `/step2` gọi AI service | ✅ |
| 2 | Kết quả hiển thị danh sách câu đã tạo | ✅ |
| 3 | Mỗi câu hiển thị: type badge, skill badge, CEFR badge | ✅ |
| 4 | Rate limit: gọi >10 lần/phút → thông báo lỗi "RATE_LIMITED" | ✅ |

---

### TC-WZ-05: Step 2 — Excel Import (hợp lệ)

**Mục tiêu:** Expert import Excel hợp lệ

**Bước thực hiện:**

1. Tab **"Import Excel"**
2. Upload file `.xlsx` hợp lệ (đúng format)
3. Click **"Upload"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | File validation: chỉ `.xlsx` được chấp nhận | ✅ |
| 2 | File > 5MB → "FILE_TOO_LARGE" | ✅ |
| 3 | Kết quả hiển thị: valid rows + error rows | ✅ |
| 4 | Valid rows thêm vào danh sách câu | ✅ |

---

### TC-WZ-06: Step 2 — Excel Import (file sai format)

**Mục tiêu:** Validation file Excel

**Bước thực hiện:**

1. Upload file `.pdf` HOẶC `.xls` (không phải .xlsx)

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Thông báo "INVALID_FILE_TYPE" | ✅ |
| 2 | File không được xử lý | ✅ |

---

### TC-WZ-07: Step 2 — Manual Add

**Mục tiêu:** Expert thêm câu hỏi thủ công trong wizard

**Bước thực hiện:**

1. Tab **"Manual Add"**
2. Click **"Add Question"**
3. Nhập: content, type (MULTIPLE_CHOICE_SINGLE), 4 options, đánh dấu đáp án đúng
4. Thêm 2 câu → nhấn Continue

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Danh sách câu thủ công hiển thị với edit/delete | ✅ |
| 2 | Sau khi thêm, questions được lưu vào session | ✅ |
| 3 | Nút "Continue to Preview" enabled khi có ≥1 câu | ✅ |

---

### TC-WZ-08: Step 2 — Kết hợp AI + Manual + Excel

**Mục tiêu:** Expert dùng nhiều nguồn trong cùng 1 wizard session

**Bước thực hiện:**

1. AI Generate → 5 câu
2. Excel Import → 3 câu (thêm vào)
3. Manual Add → 2 câu (thêm vào)
4. Kiểm tra tổng số câu

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Tổng = 10 câu (5+3+2) | ✅ |
| 2 | Câu từ AI + Excel + Manual đều trong danh sách | ✅ |
| 3 | Có thể xóa câu từng nguồn riêng | ✅ |

---

### TC-WZ-09: Step 3 — Validation ERROR → chặn proceed

**Mục tiêu:** Validation hiển thị lỗi và block Step 4

**Bước thực hiện:**

1. Tạo 1 câu WRITING trong khi group skill = LISTENING (sai type)
2. Tiến đến Step 3
3. Xem kết quả validate

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Red banner: "INVALID_TYPE_FOR_SKILL" | ✅ |
| 2 | Mỗi câu lỗi hiển thị danh sách lỗi cụ thể | ✅ |
| 3 | "Continue to Step 4" bị DISABLED | ✅ |
| 4 | Nút "Re-validate" gọi lại validate API | ✅ |

---

### TC-WZ-10: Step 3 — Validation WARNING → cho phép proceed

**Mục tiêu:** Warnings không block proceed

**Bước thực hiện:**

1. Tạo câu hỏi không có explanation và không có tags
2. Tiến đến Step 3

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Yellow banner: "EXPLANATION_MISSING" (WARNING) | ✅ |
| 2 | Yellow banner: "TAGS_MISSING" (WARNING) | ✅ |
| 3 | "Continue to Step 4" vẫn ENABLED | ✅ |

---

### TC-WZ-11: Step 3 — Fix lỗi rồi re-validate

**Mục tiêu:** Expert fix lỗi và tiếp tục

**Bước thực hiện:**

1. Step 3 hiển thị 2 lỗi
2. Quay lại Step 2 → xóa câu lỗi
3. Thêm câu đúng type
4. Step 3 → "Re-validate" → isClean = true

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Sau fix: "Continue to Step 4" ENABLED | ✅ |
| 2 | Error count = 0 | ✅ |

---

### TC-WZ-12: Step 4 — Save as Draft (Expert Bank)

**Mục tiêu:** Expert lưu group ở trạng thái Draft, source Expert Bank

**Bước thực hiện:**

1. Step 4: chọn **"Save as Draft"** + **"Expert Bank"** (shared)
2. Click **"Save & Finish"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API `/save` được gọi với `{ status: "DRAFT", source: "EXPERT_BANK" }` | ✅ |
| 2 | DB: `question_group.status = 'DRAFT'` | ✅ |
| 3 | DB: `question_group.source = 'EXPERT_BANK'` | ✅ |
| 4 | Redirect đến `/expert/questions` | ✅ |
| 5 | Group mới xuất hiện trong question bank | ✅ |
| 6 | Session được clear sau save | ✅ |

---

### TC-WZ-13: Step 4 — Publish ngay (Expert Bank)

**Mục tiêu:** Expert save + publish cùng lúc

**Bước thực hiện:**

1. Step 4: chọn **"Publish"** + **"Expert Bank"**
2. Click **"Save & Finish"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | DB: `question_group.status = 'PUBLISHED'` | ✅ |
| 2 | Questions có `status = 'PUBLISHED'` | ✅ |
| 3 | Teacher browse bank thấy câu mới ngay | ✅ |

---

### TC-WZ-14: beforeunload warning

**Mục tiêu:** Cảnh báo khi user tháo ổng đóng tab

**Bước thực hiện:**

1. Đang ở Step 2 (có dữ liệu trong session)
2. Thử đóng tab / refresh trang

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Browser hiển thị confirmation dialog | ✅ |

---

### TC-WZ-15: Page refresh — recover session data

**Mục tiêu:** Wizard hồi phục dữ liệu khi refresh

**Bước thực hiện:**

1. Hoàn thành Step 1 + Step 2 (có 5 câu)
2. Refresh trang `/expert/questions/wizard`
3. Gọi API `/step-data`

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | Step 1 data được restore | ✅ |
| 2 | 5 câu từ Step 2 được restore | ✅ |
| 3 | Message "Wizard session recovered" hiển thị | ✅ |

---

### TC-WZ-16: Leave Wizard (abandon)

**Mục tiêu:** User rời wizard → session cleared

**Bước thực hiện:**

1. Đang ở Step 2
2. Click **"Leave Wizard"**

**Kết quả mong đợi:**

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 1 | API `/abandon` được gọi | ✅ |
| 2 | Session cleared | ✅ |
| 3 | Redirect đến `/expert/questions` | ✅ |

---

## Bảng Tổng Hợp Test Coverage

| Spec | Số TC | Loại coverage |
|---|---|---|
| SPEC 001 — Expert tạo Assignment | 12 | Happy path + validation + bảo mật |
| SPEC 002 — Teacher tạo Lesson Quiz | 11 | Happy path + validation + bảo mật |
| SPEC 003 — Expert duyệt câu hỏi | 10 | Approve/reject + notification + bảo mật |
| SPEC 004 — Student làm Assignment | 14 | 4 sections + timer + resume + bảo mật |
| SPEC 005 — Teacher chấm Assignment | 9 | Happy path + AI fail + re-grade + bảo mật |
| SPEC 006 — Teacher chấm Lesson Quiz | 5 | Dynamic tabs + edge cases + dashboard |
| QuestionGroup Wizard | 16 | 4 steps + AI/Excel/Manual + validation + bảo mật |
| **Tổng** | **77** | |

---

## Priority Test Matrix

| Priority | Mô tả | TC |
|---|---|---|
| P0 — Critical | Chức năng cốt lõi không có thì không dùng được | TC-001-08, TC-004-01, TC-004-08, TC-005-05, TC-WZ-12 |
| P1 — High | Happy path chính | Tất cả TC có ✅ ở "Kết quả mong đợi" #1 |
| P2 — Medium | Validation và edge cases | TC-001-03, TC-002-07, TC-003-05, TC-WZ-09 |
| P3 — Low | Bảo mật, notification, recovery | TC-001-12, TC-003-07, TC-WZ-14 |
