package com.example.DoAn.service;

import com.example.DoAn.model.AIPromptConfig;
import java.util.List;
import java.util.Map;

public interface IAIPromptConfigService {
    List<AIPromptConfig> getAllConfigs();
    AIPromptConfig getConfigByBucket(String bucket);
    AIPromptConfig saveConfig(AIPromptConfig config);
    Map<String, Object> getBucketConfigAsMap(String bucket);
    void resetToDefault(String bucket);
}
