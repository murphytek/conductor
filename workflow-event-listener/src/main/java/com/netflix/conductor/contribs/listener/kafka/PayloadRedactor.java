/*
 * Copyright 2025 Conductor Authors.
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
package com.netflix.conductor.contribs.listener.kafka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Masks secrets in workflow and task input and output before they leave Conductor in an event. A
 * value whose key matches the pattern is replaced with {@value #REDACTED}, unless it is a number or
 * a boolean. Strings that hold a JSON object are parsed and masked the same way. Works on copies;
 * the maps passed in are not changed.
 */
public class PayloadRedactor {

    public static final String REDACTED = "[REDACTED]";

    /** Keys that end like a secret: tokens, API and other keys, secrets, passwords, credentials. */
    public static final String DEFAULT_KEY_PATTERN =
            "(?i)(secret|password|passwd|credentials?|authorization|cookie|tokens?"
                    + "|(api|access|private|secret|signing|encryption|client)[-_]?key)$";

    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};

    private final Pattern keyPattern;
    private final ObjectMapper objectMapper;

    /**
     * @param keyPattern regular expression searched for in each key; blank turns masking off
     */
    public PayloadRedactor(String keyPattern, ObjectMapper objectMapper) {
        this.keyPattern =
                keyPattern == null || keyPattern.isBlank() ? null : Pattern.compile(keyPattern);
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> redact(Map<String, Object> payload) {
        if (keyPattern == null || payload == null) {
            return payload;
        }
        Map<String, Object> masked = new LinkedHashMap<>();
        payload.forEach((key, value) -> masked.put(key, redactValue(key, value)));
        return masked;
    }

    private Object redactValue(String key, Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (key != null && keyPattern.matcher(key).find()) {
            return REDACTED;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> masked = new LinkedHashMap<>();
            map.forEach(
                    (k, v) -> {
                        String name = k == null ? null : k.toString();
                        masked.put(name, redactValue(name, v));
                    });
            return masked;
        }
        if (value instanceof Collection<?> items) {
            List<Object> masked = new ArrayList<>(items.size());
            items.forEach(item -> masked.add(redactValue(null, item)));
            return masked;
        }
        if (value instanceof String text) {
            return redactJsonObject(text);
        }
        return value;
    }

    /** An environment passed as a JSON string carries its secrets inside the string. */
    private String redactJsonObject(String text) {
        String trimmed = text.trim();
        if (trimmed.length() < 2
                || trimmed.charAt(0) != '{'
                || trimmed.charAt(trimmed.length() - 1) != '}') {
            return text;
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(trimmed, JSON_OBJECT);
            Map<String, Object> masked = redact(parsed);
            return masked.equals(parsed) ? text : objectMapper.writeValueAsString(masked);
        } catch (Exception notJson) {
            return text;
        }
    }
}
