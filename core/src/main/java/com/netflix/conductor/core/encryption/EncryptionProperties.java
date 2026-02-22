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
package com.netflix.conductor.core.encryption;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.resources.database.encryption")
public record EncryptionProperties(
        boolean enabled, String activeKeyId, Map<String, EncryptionKeyProperties> keys) {

    public EncryptionProperties {
        if (keys == null) {
            keys = Map.of();
        }
        if (enabled) {
            if (activeKeyId == null || activeKeyId.isBlank()) {
                throw new IllegalArgumentException(
                        "active-key-id must be set when encryption is enabled");
            }
            if (keys.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one encryption key must be configured when encryption is enabled");
            }
            if (!keys.containsKey(activeKeyId)) {
                throw new IllegalArgumentException(
                        "active-key-id '%s' must exist in the keys map".formatted(activeKeyId));
            }
        }
    }
}
