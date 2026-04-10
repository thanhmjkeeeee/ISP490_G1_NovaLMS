package com.example.DoAn.service.impl;

import com.example.DoAn.model.AIPromptConfig;
import com.example.DoAn.repository.AIPromptConfigRepository;
import com.example.DoAn.service.IAIPromptConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIPromptConfigServiceImpl implements IAIPromptConfigService {

    private final AIPromptConfigRepository repository;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_CONFIG_PATH = "config/ai-prompt-advanced.yaml";

    @PostConstruct
    public void init() {
        if (repository.count() == 0) {
            log.info("AI Prompt Config table is empty. Initializing from default YAML...");
            loadFromYaml();
        }
    }

    private void loadFromYaml() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PATH)) {
            if (is == null) {
                log.warn("Default AI config file not found: {}", DEFAULT_CONFIG_PATH);
                return;
            }
            Map<String, Object> yamlData = new Yaml().load(is);
            Map<String, Object> advanced = (Map<String, Object>) yamlData.get("advanced");
            if (advanced != null) {
                for (String level : List.of("beginner", "intermediate", "advanced")) {
                    Map<String, Object> cfg = (Map<String, Object>) advanced.get(level);
                    if (cfg != null) {
                        saveYamlBucket(level, cfg);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize AI Prompt Config from YAML: {}", e.getMessage());
        }
    }

    private void saveYamlBucket(String bucket, Map<String, Object> cfg) throws Exception {
        AIPromptConfig entity = AIPromptConfig.builder()
                .bucket(bucket)
                .bloomInstruction((String) cfg.get("bloom_instruction"))
                .grammarFocus(objectMapper.writeValueAsString(cfg.get("grammar_focus")))
                .questionTypesRatio(objectMapper.writeValueAsString(cfg.get("question_types_ratio")))
                .skills(objectMapper.writeValueAsString(cfg.get("skills")))
                .lexicalComplexity((String) cfg.get("lexical_complexity"))
                .writingConstraint((String) cfg.get("writing_constraint"))
                .speakingConstraint((String) cfg.get("speaking_constraint"))
                .build();
        repository.save(entity);
    }

    @Override
    public List<AIPromptConfig> getAllConfigs() {
        return repository.findAll();
    }

    @Override
    public AIPromptConfig getConfigByBucket(String bucket) {
        return repository.findByBucket(bucket).orElse(null);
    }

    @Override
    public AIPromptConfig saveConfig(AIPromptConfig config) {
        return repository.save(config);
    }

    @Override
    public Map<String, Object> getBucketConfigAsMap(String bucket) {
        AIPromptConfig entity = getConfigByBucket(bucket);
        if (entity == null) return null;

        Map<String, Object> map = new HashMap<>();
        try {
            map.put("bloom_instruction", entity.getBloomInstruction());
            map.put("grammar_focus", objectMapper.readValue(entity.getGrammarFocus(), new TypeReference<List<String>>() {}));
            map.put("question_types_ratio", objectMapper.readValue(entity.getQuestionTypesRatio(), new TypeReference<Map<String, Double>>() {}));
            map.put("skills", objectMapper.readValue(entity.getSkills(), new TypeReference<List<String>>() {}));
            map.put("lexical_complexity", entity.getLexicalComplexity());
            map.put("writing_constraint", entity.getWritingConstraint());
            map.put("speaking_constraint", entity.getSpeakingConstraint());
        } catch (Exception e) {
            log.error("Error parsing AI config from DB for bucket {}: {}", bucket, e.getMessage());
        }
        return map;
    }

    @Override
    public void resetToDefault(String bucket) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PATH)) {
            if (is == null) return;
            Map<String, Object> yamlData = new Yaml().load(is);
            Map<String, Object> advanced = (Map<String, Object>) yamlData.get("advanced");
            if (advanced != null) {
                Map<String, Object> cfg = (Map<String, Object>) advanced.get(bucket);
                if (cfg != null) {
                    Optional<AIPromptConfig> existing = repository.findByBucket(bucket);
                    AIPromptConfig entity = existing.orElse(new AIPromptConfig());
                    entity.setBucket(bucket);
                    entity.setBloomInstruction((String) cfg.get("bloom_instruction"));
                    entity.setGrammarFocus(objectMapper.writeValueAsString(cfg.get("grammar_focus")));
                    entity.setQuestionTypesRatio(objectMapper.writeValueAsString(cfg.get("question_types_ratio")));
                    entity.setSkills(objectMapper.writeValueAsString(cfg.get("skills")));
                    entity.setLexicalComplexity((String) cfg.get("lexical_complexity"));
                    entity.setWritingConstraint((String) cfg.get("writing_constraint"));
                    entity.setSpeakingConstraint((String) cfg.get("speaking_constraint"));
                    repository.save(entity);
                }
            }
        } catch (Exception e) {
            log.error("Failed to reset AI Prompt Config to default: {}", e.getMessage());
        }
    }
}
