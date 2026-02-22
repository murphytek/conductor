/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.core.utils;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Masks secret values in task output data. Serializes the output to JSON, replaces all occurrences
 * of each secret value with "***", then deserializes back. This catches secrets in nested
 * structures and concatenated strings.
 */
@Component
public class SecretMaskingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretMaskingService.class);
    private static final String MASK = "***";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final SecretProvider secretProvider;
    private final ObjectMapper objectMapper;

    public SecretMaskingService(SecretProvider secretProvider, ObjectMapper objectMapper) {
        this.secretProvider = secretProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * Mask all secret values found in the given output data map.
     *
     * @param outputData the task output data (may be null)
     * @return the masked output data, or the original if no secrets or on error
     */
    public Map<String, Object> maskSecrets(Map<String, Object> outputData) {
        if (outputData == null || outputData.isEmpty()) {
            return outputData;
        }

        Map<String, String> secrets = secretProvider.getAllSecretsDecrypted();
        if (secrets == null || secrets.isEmpty()) {
            return outputData;
        }

        // Filter to only non-empty secret values worth masking
        Collection<String> secretValues =
                secrets.values().stream().filter(v -> v != null && !v.isEmpty()).toList();

        if (secretValues.isEmpty()) {
            return outputData;
        }

        try {
            String json = objectMapper.writeValueAsString(outputData);
            for (String secretValue : secretValues) {
                json = json.replace(secretValue, MASK);
            }
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            LOGGER.warn("Failed to mask secrets in task output, returning original", e);
            return outputData;
        }
    }
}
