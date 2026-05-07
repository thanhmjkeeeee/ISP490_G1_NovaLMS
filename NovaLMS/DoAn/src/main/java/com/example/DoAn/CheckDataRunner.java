package com.example.DoAn;

import com.example.DoAn.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CheckDataRunner implements CommandLineRunner {
    private final QuestionRepository questionRepository;

    public CheckDataRunner(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        questionRepository.findAll().forEach(q -> {
            System.out.println("ID: " + q.getQuestionId() + ", CEFR: [" + q.getCefrLevel() + "]");
        });
    }
}
