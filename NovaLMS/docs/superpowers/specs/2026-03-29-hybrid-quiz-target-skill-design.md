# Spec: Quiz `targetSkill` cho Hybrid Placement Test

## 1. Mục tiêu

Quiz ENTRY_TEST hybrid phải có **đúng 1 kỹ năng** duy nhất (`targetSkill`). Mỗi quiz chỉ hiển thị ở skill card tương ứng trên trang hybrid-entry, tránh trùng lặp khi user chọn nhiều skills.

## 2. Thay đổi Data Model

### 2.1 `Quiz` Entity — thêm field

```java
// nullable — chỉ dùng khi isHybridEnabled = true
private String targetSkill;  // Grammar | Vocabulary | Listening | Reading | Writing | Speaking
```

### 2.2 `QuizRequestDTO` — thêm field

```java
private String targetSkill;  // nullable
```

## 3. Thay đổi Form UI

### 3.1 `expert/quiz-create.html`

Khi expert tick `isHybridEnabled = true` cho ENTRY_TEST:

→ Hiện thêm select **"Kỹ năng đích"**:

```html
<div id="hybridSkillSection" class="mb-3" style="display:none;">
    <label class="form-label fw-bold">Kỹ năng đích <span class="text-danger">*</span></label>
    <select id="targetSkill" class="form-select" required>
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

- Khi `isHybridEnabled` untick → ẩn `hybridSkillSection` và reset `targetSkill`
- Khi `isHybridEnabled` tick → hiện `hybridSkillSection`
- `targetSkill` gửi lên trong body của `submitQuiz()`

### 3.2 `expert/quiz-edit.html`

- Load `targetSkill` từ API, hiển thị trong select
- Khi `isHybridEnabled = true` → hiện `hybridSkillSection`
- `targetSkill` gửi lên trong body của `updateQuiz()`

## 4. Thay đổi Service (ExpertQuizServiceImpl)

### 4.1 `createQuiz()` / `updateQuiz()`

- Lưu `targetSkill` từ request vào `Quiz` entity
- Backend không cần validate gì thêm (nullable)

### 4.2 `addQuestionToQuiz()`

Thêm validation khi thêm question vào quiz ENTRY_TEST hybrid:

```java
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

## 5. Thay đổi Modal thêm câu hỏi (`expert/quiz-questions.html`)

Trong `openAddModal()`:

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

- Quiz hybrid có `targetSkill` → skill select bị khóa ở giá trị đúng
- Quiz thường → skill select hoạt động bình thường

## 6. Thay đổi HybridPlacementService

### 6.1 `getAvailableSkills()`

```java
List<Quiz> hybridQuizzes = quizRepository.findAll().stream()
    .filter(q -> "ENTRY_TEST".equals(q.getQuizCategory()))
    .filter(q -> "PUBLISHED".equals(q.getStatus()))
    .filter(q -> Boolean.TRUE.equals(q.getIsHybridEnabled()))
    .collect(Collectors.toList());

Map<String, Long> countBySkill = new HashMap<>();
for (Quiz q : hybridQuizzes) {
    String skill = q.getTargetSkill(); // ưu tiên targetSkill
    if (skill == null) {
        // Fallback: extract từ questions (quiz hybrid cũ chưa có targetSkill)
        skill = deriveSkillFromQuiz(q);
    }
    countBySkill.merge(skill, 1L, Long::sum);
}
```

### 6.2 `getQuizzesBySkills()`

```java
for (Quiz q : hybridQuizzes) {
    String skill = q.getTargetSkill();
    if (skill == null) {
        skill = deriveSkillFromQuiz(q); // fallback
    }
    if (skill == null) continue;

    for (String requestedSkill : skills) {
        if (skill.equals(requestedSkill)) {
            int totalQuestions = q.getQuizQuestions() != null ? q.getQuizQuestions().size() : 0;
            result.get(skill).add(HybridQuizSummaryDTO.builder()
                .quizId(q.getQuizId())
                .title(q.getTitle())
                .description(q.getDescription())
                .totalQuestions(totalQuestions)
                .timeLimitMinutes(q.getTimeLimitMinutes())
                .skill(skill)
                .build());
        }
    }
}
```

## 7. Tóm tắt các file cần thay đổi

| File | Thay đổi |
|---|---|
| `Quiz.java` | Thêm `targetSkill` field |
| `QuizRequestDTO.java` | Thêm `targetSkill` field |
| `ExpertQuizServiceImpl.java` | Lưu `targetSkill`; thêm validation trong `addQuestionToQuiz()` |
| `expert/quiz-create.html` | Thêm `hybridSkillSection`; gửi `targetSkill` |
| `expert/quiz-edit.html` | Load + hiển thị `targetSkill`; gửi khi update |
| `expert/quiz-questions.html` | `openAddModal()` auto-lock skill select cho hybrid |
| `HybridPlacementServiceImpl.java` | Dùng `targetSkill` làm primary skill |

## 8. Backwards Compatibility

- Quiz hybrid cũ chưa có `targetSkill` → fallback sang extract từ questions (vẫn hoạt động, nhưng khuyến nghị expert set lại khi edit)
- `targetSkill` là nullable → quiz ENTRY_TEST không hybrid không bị ảnh hưởng
