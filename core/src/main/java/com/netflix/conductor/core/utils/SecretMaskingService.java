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

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Masks secret values in task output data by recursively walking the output map and replacing any
 * string value (or substring) that matches a secret with "***".
 */
@Component
@ConditionalOnBean(SecretProvider.class)
public class SecretMaskingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretMaskingService.class);
    private static final String MASK = "***";

    private final SecretProvider secretProvider;

    public SecretMaskingService(SecretProvider secretProvider) {
        this.secretProvider = secretProvider;
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

        Collection<String> secretValues =
                secrets.values().stream().filter(v -> v != null && !v.isEmpty()).toList();

        if (secretValues.isEmpty()) {
            return outputData;
        }

        return maskMap(outputData, secretValues);
    }

    @SuppressWarnings("unchecked")
    private Object maskValue(Object value, Collection<String> secretValues) {
        if (value instanceof String s) {
            for (String secret : secretValues) {
                s = s.replace(secret, MASK);
            }
            return s;
        } else if (value instanceof Map<?, ?> m) {
            return maskMap((Map<String, Object>) m, secretValues);
        } else if (value instanceof List<?> l) {
            return maskList(l, secretValues);
        }
        return value;
    }

    private Map<String, Object> maskMap(Map<String, Object> map, Collection<String> secretValues) {
        Map<String, Object> result = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), maskValue(entry.getValue(), secretValues));
        }
        return result;
    }

    private List<Object> maskList(List<?> list, Collection<String> secretValues) {
        List<Object> result = new ArrayList<>(list.size());
        for (Object item : list) {
            result.add(maskValue(item, secretValues));
        }
        return result;
    }
}
