# Quiz `targetSkill` cho Hybrid Placement Test — Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thêm field `targetSkill` vào Quiz entity để mỗi quiz ENTRY_TEST hybrid chỉ gắn 1 kỹ năng duy nhất, tránh trùng lặp khi user chọn nhiều skills trên hybrid-entry.

**Architecture:** Thêm nullable `targetSkill` field vào Quiz, dùng làm primary skill label thay vì extract từ questions. Service validate khi thêm question vào quiz hybrid. Frontend lock skill select trong modal khi quiz có `targetSkill`.

**Tech Stack:** Spring Boot (JPA), Thymeleaf, Bootstrap 5, JavaScript

---

## Chunk 1: Data Model — Quiz entity + DTO

### Task 1: Thêm `targetSkill` vào Quiz entity

**Files:**
- Modify: `NovaLMS\DoAn\src\main\java\com\example\DoAn\model\Quiz.java`

- [ ] **Step 1: Đọc file Quiz.java để tìm vị trí thêm field**

Tìm các field String/Boolean gần `isHybridEnabled` để thêm ngay cạnh.

- [ ] **Step 2: Thêm field `targetSkill` sau `isHybridEnabled`**

```java
/**
 * Kỹ năng đích cho quiz ENTRY_TEST hybrid.
 * Nullable — chỉ dùng khi isHybridEnabled = true.
 * Giá trị: Grammar | Vocabulary | Listening | Reading | Writing | Speaking
 */
@Column(name = "target_skill", length = 20)
private String targetSkill;
```

- [ ] **Step 3: Verify file compiles**

Build project: `mvn compile -q` (hoặc build qua IDE). Không có lỗi compile.

---

### Task 2: Thêm `targetSkill` vào QuizRequestDTO

**Files:**
- Modify: `NovaLMS\DoAn\src\main\java\com\example\DoAn\dto\request\QuizRequestDTO.java`

- [ ] **Step 1: Thêm field sau `isHybridEnabled`**

```java
private String targetSkill;  // nullable — Grammar|Vocabulary|Listening|Reading|Writing|Speaking
```

- [ ] **Step 2: Verify file compiles**

---

## Chunk 2: Service — ExpertQuizServiceImpl

### Task 3: Lưu `targetSkill` trong createQuiz/updateQuiz

**Files:**
- Modify: `NovaLMS\DoAn\src\main\java\com\example\DoAn\service\impl\ExpertQuizServiceImpl.java`
  - Tìm `createQuiz()` — thêm `.targetSkill(request.getTargetSkill())` vào builder
  - Tìm `updateQuiz()` — thêm `quiz.setTargetSkill(request.getTargetSkill())` trước save

- [ ] **Step 1: Đọc createQuiz() — tìm dòng `.isHybridEnabled` trong builder**

```java
.isHybridEnabled(request.getIsHybridEnabled() != null ? request.getIsHybridEnabled() : false)
```

- [ ] **Step 2: Thêm `.targetSkill(request.getTargetSkill())` sau dòng trên**

```java
.targetSkill(request.getTargetSkill())
```

- [ ] **Step 3: Đọc updateQuiz() — tìm chỗ set các field trước save**

Thêm sau dòng set `isHybridEnabled`:
```java
quiz.setTargetSkill(request.getTargetSkill());
```

- [ ] **Step 4: Verify file compiles**

---

### Task 4: Thêm validation trong addQuestionToQuiz()

**Files:**
- Modify: `NovaLMS\DoAn\src\main\java\com\example\DoAn\service\impl\ExpertQuizServiceImpl.java`
  - Trong `addQuestionToQuiz()`, thêm check sau block ENTRY_TEST type validation

- [ ] **Step 1: Đọc addQuestionToQuiz() — tìm vị trí sau check ENTRY_TEST type (dòng ~254)**

```java
if ("ENTRY_TEST".equals(quiz.getQuizCategory())) {
    String qType = question.getQuestionType();
    if (!(...)) {
        throw new InvalidDataException("ENTRY_TEST chỉ cấu hình...");
    }
}
// ← thêm validation targetSkill vào đây
```

- [ ] **Step 2: Thêm validation sau block ENTRY_TEST type check**

```java
// Validate targetSkill cho quiz hybrid
if (Boolean.TRUE.equals(quiz.getIsHybridEnabled())
        && quiz.getTargetSkill() != null) {
    if (!quiz.getTargetSkill().equals(question.getSkill())) {
        throw new InvalidDataException(
            "Quiz hybrid này chỉ chấp nhận câu hỏi kỹ năng ["
            + quiz.getTargetSkill()
            + "]. Câu hỏi bạn thêm có kỹ năng ["
            + question.getSkill() + "]."
        );
    }
}
```

- [ ] **Step 3: Verify file compiles**

---

## Chunk 3: Frontend — quiz-create + quiz-edit

### Task 5: Thêm targetSkill select vào quiz-create.html

**Files:**
- Modify: `NovaLMS\DoAn\src\main\resources\templates\expert\quiz-create.html`
  - Thêm `hybridSkillSection` div sau `hybridSection`
  - Gửi `targetSkill` trong body `submitQuiz()`

- [ ] **Step 1: Thêm HTML section sau `hybridSection` (sau dòng ~89)**

```html
<!-- Target Skill — chỉ hiện khi isHybridEnabled = true -->
<div class="mb-3" id="hybridSkillSection" style="display:none;">
    <label class="form-label fw-bold">Kỹ năng đích <span class="text-danger">*</span></label>
    <select id="targetSkill" class="form-select">
        <option value="">-- Chọn kỹ năng --</option>
        <option value="Grammar">Grammar</option>
        <option value="Vocabulary">Vocabulary</option>
        <option value="Listening">Listening</option>
        <option value="Reading">Reading</option>
        <option value="Writing">Writing</option>
        <option value="Speaking">Speaking</option>
    </select>
</div>
```

- [ ] **Step 2: Cập nhật `onCategoryChange()` — hiện hybridSkillSection khi isHybridEnabled tick**

Thêm vào khối `if (cat === 'ENTRY_TEST')` sau dòng `document.getElementById('hybridSection').style.display = 'block';`:
```javascript
document.getElementById('hybridSkillSection').style.display = 'block';
```

Thêm vào khối `else` (reset):
```javascript
document.getElementById('hybridSkillSection').style.display = 'none';
document.getElementById('targetSkill').value = '';
```

- [ ] **Step 3: Thêm event listener cho isHybridEnabled checkbox**

Thêm sau `onCategoryChange()`:
```javascript
document.getElementById('isHybridEnabled').addEventListener('change', function() {
    const skillSection = document.getElementById('hybridSkillSection');
    if (this.checked) {
        skillSection.style.display = 'block';
    } else {
        skillSection.style.display = 'none';
        document.getElementById('targetSkill').value = '';
    }
});
```

- [ ] **Step 4: Gửi targetSkill trong body submitQuiz()**

Thêm vào object body:
```javascript
targetSkill: document.getElementById('isHybridEnabled').checked
    ? (document.getElementById('targetSkill').value || null)
    : null,
```

---

### Task 6: Thêm targetSkill select vào quiz-edit.html

**Files:**
- Modify: `NovaLMS\DoAn\src\main\resources\templates\expert\quiz-edit.html`
  - Thêm `hybridSkillSection` HTML
  - Load + hiển thị `targetSkill` từ API trong `loadQuiz()`
  - Gửi `targetSkill` trong `updateQuiz()`

- [ ] **Step 1: Thêm HTML section sau hybridSection (sau dòng ~93)**

```html
<div class="mb-3" id="hybridSkillSection" style="display:none;">
    <label class="form-label fw-bold">Kỹ năng đích <span class="text-danger">*</span></label>
    <select id="targetSkill" class="form-select">
        <option value="">-- Chọn kỹ năng --</option>
        <option value="Grammar">Grammar</option>
        <option value="Vocabulary">Vocabulary</option>
        <option value="Listening">Listening</option>
        <option value="Reading">Reading</option>
        <option value="Writing">Writing</option>
        <option value="Speaking">Speaking</option>
    </select>
</div>
```

- [ ] **Step 2: Cập nhật loadQuiz() — load targetSkill + hiện hybridSkillSection**

Thêm vào khối `if (q.quizCategory === 'ENTRY_TEST')` sau `document.getElementById('hybridSection').style.display = 'block';`:
```javascript
document.getElementById('hybridSkillSection').style.display = 'block';
document.getElementById('targetSkill').value = q.targetSkill || '';
```

- [ ] **Step 3: Thêm event listener cho isHybridEnabled trong quiz-edit**

Thêm sau `loadQuiz()` call:
```javascript
document.getElementById('isHybridEnabled').addEventListener('change', function() {
    const skillSection = document.getElementById('hybridSkillSection');
    if (this.checked) {
        skillSection.style.display = 'block';
    } else {
        skillSection.style.display = 'none';
        document.getElementById('targetSkill').value = '';
    }
});
```

- [ ] **Step 4: Gửi targetSkill trong updateQuiz() body**

```javascript
targetSkill: document.getElementById('isHybridEnabled').checked
    ? (document.getElementById('targetSkill').value || null)
    : null,
```

---

## Chunk 4: Frontend — quiz-questions.html modal

### Task 7: Auto-lock skill select trong modal thêm câu hỏi

**Files:**
- Modify: `NovaLMS\DoAn\src\main\resources\templates\expert\quiz-questions.html`

- [ ] **Step 1: Cập nhật openAddModal() — lock skill nếu quiz hybrid có targetSkill**

Thay toàn bộ function `openAddModal()`:
```javascript
function openAddModal() {
    currentBankPage = 0;
    new bootstrap.Modal(document.getElementById('addQuestionModal')).show();

    const quiz = window.currentQuiz;
    const skillSelect = document.getElementById('modalSkill');

    if (quiz && quiz.isHybridEnabled && quiz.targetSkill) {
        // Auto-lock: chỉ hiện questions đúng skill
        skillSelect.value = quiz.targetSkill;
        skillSelect.disabled = true;

        // Hiện note thông báo
        const existingNote = document.getElementById('modalSkillLockedNote');
        if (!existingNote) {
            const note = document.createElement('div');
            note.id = 'modalSkillLockedNote';
            note.className = 'alert alert-info py-1 px-2 small mb-3';
            note.innerHTML = `<i class="bi bi-lock me-1"></i>Quiz hybrid kỹ năng <strong>${quiz.targetSkill}</strong> — chỉ hiển thị câu hỏi phù hợp.`;
            skillSelect.closest('.col-md-2').after(note);
        }
    } else {
        skillSelect.disabled = false;
        const note = document.getElementById('modalSkillLockedNote');
        if (note) note.remove();
    }

    loadBankQuestions(0);
}
```

- [ ] **Step 2: Verify changes**

---

## Chunk 5: Backend — HybridPlacementServiceImpl

### Task 8: Cập nhật getAvailableSkills() — dùng targetSkill

**Files:**
- Modify: `NovaLMS\DoAn\src\main\java\com\example\DoAn\service\impl\HybridPlacementServiceImpl.java`

- [ ] **Step 1: Đọc getAvailableSkills() — tìm block extractSkillsFromQuiz**

Tìm đoạn:
```java
Set<String> skills = extractSkillsFromQuiz(q);
for (String s : skills) {
    countBySkill.merge(s, 1L, Long::sum);
}
```

- [ ] **Step 2: Thay thế bằng targetSkill + fallback**

```java
String skill = q.getTargetSkill();
if (skill == null) {
    // Fallback: extract từ questions (quiz hybrid cũ chưa có targetSkill)
    Set<String> extracted = extractSkillsFromQuiz(q);
    skill = extracted.isEmpty() ? null : extracted.iterator().next();
}
if (skill != null) {
    countBySkill.merge(skill, 1L, Long::sum);
}
```

- [ ] **Step 3: Verify file compiles**

---

### Task 9: Cập nhật getQuizzesBySkills() — dùng targetSkill

**Files:**
- Modify: `NovaLMS\DoAn\src\main\java\com\example\DoAn\service\impl\HybridPlacementServiceImpl.java`

- [ ] **Step 1: Đọc getQuizzesBySkills() — tìm block extractSkillsFromQuiz**

Tìm đoạn:
```java
Set<String> quizSkills = extractSkillsFromQuiz(q);
for (String skill : skills) {
    if (quizSkills.contains(skill)) {
```

- [ ] **Step 2: Thay thế bằng targetSkill + fallback**

Thay toàn bộ block:
```java
String quizSkill = q.getTargetSkill();
if (quizSkill == null) {
    Set<String> extracted = extractSkillsFromQuiz(q);
    quizSkill = extracted.isEmpty() ? null : extracted.iterator().next();
}
if (quizSkill == null) continue;

for (String requestedSkill : skills) {
    if (quizSkill.equals(requestedSkill)) {
        int totalQuestions = q.getQuizQuestions() != null ? q.getQuizQuestions().size() : 0;
        result.get(requestedSkill).add(HybridQuizSummaryDTO.builder()
                .quizId(q.getQuizId())
                .title(q.getTitle())
                .description(q.getDescription())
                .totalQuestions(totalQuestions)
                .timeLimitMinutes(q.getTimeLimitMinutes())
                .skill(quizSkill)
                .build());
    }
}
```

- [ ] **Step 3: Verify file compiles**

---

## Chunk 6: Database Migration

### Task 10: Thêm column target_skill vào bảng quiz

**Files:**
- Modify: SQL migration script hoặc chạy trực tiếp trên DB

- [ ] **Step 1: Chạy SQL**

```sql
ALTER TABLE quiz ADD COLUMN target_skill VARCHAR(20) NULL;
```

Kiểm tra: `DESCRIBE quiz;` — cột `target_skill` xuất hiện, nullable.

---

## Verification Checklist

- [ ] Restart server
- [ ] Truy cập `/expert/quiz-management/create`, chọn Entry Test → tick Hybrid → thấy dropdown "Kỹ năng đích"
- [ ] Tạo quiz hybrid Grammar, thêm câu hỏi → skill select bị lock ở Grammar trong modal
- [ ] Thử thêm câu hỏi Listening vào quiz Grammar hybrid → bị reject với message rõ ràng
- [ ] Truy cập `/hybrid-entry` → quiz chỉ hiện ở đúng skill card
- [ ] Quiz hybrid cũ (chưa có targetSkill) → vẫn hoạt động (fallback extract từ questions)
