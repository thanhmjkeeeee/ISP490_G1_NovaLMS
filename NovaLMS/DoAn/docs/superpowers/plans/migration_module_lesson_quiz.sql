-- Add module_id and lesson_id columns to quiz table
ALTER TABLE quiz
    ADD COLUMN module_id INT,
    ADD COLUMN lesson_id INT,
    ADD CONSTRAINT fk_quiz_module FOREIGN KEY (module_id) REFERENCES module(module_id),
    ADD CONSTRAINT fk_quiz_lesson FOREIGN KEY (lesson_id) REFERENCES lesson(lesson_id);

-- Create quiz_assignment table
CREATE TABLE quiz_assignment (
    assignment_id INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id INT NOT NULL,
    lesson_id INT,
    module_id INT,
    order_index INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_qa_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(quiz_id),
    CONSTRAINT fk_qa_lesson FOREIGN KEY (lesson_id) REFERENCES lesson(lesson_id),
    CONSTRAINT fk_qa_module FOREIGN KEY (module_id) REFERENCES module(module_id),
    CONSTRAINT uq_qa_lesson_quiz UNIQUE (quiz_id, lesson_id),
    CONSTRAINT uq_qa_module_quiz UNIQUE (quiz_id, module_id)
);

-- Create lesson_quiz_progress table
CREATE TABLE lesson_quiz_progress (
    progress_id INT AUTO_INCREMENT PRIMARY KEY,
    lesson_id INT NOT NULL,
    user_id INT NOT NULL,
    quiz_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    best_score DOUBLE,
    best_passed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lqp_lesson FOREIGN KEY (lesson_id) REFERENCES lesson(lesson_id),
    CONSTRAINT fk_lqp_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_lqp_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(quiz_id),
    CONSTRAINT uq_lqp UNIQUE (lesson_id, user_id, quiz_id)
);

-- Remove obsolete quiz_id column from lesson (if column exists)
ALTER TABLE lesson DROP COLUMN IF EXISTS quiz_id;
