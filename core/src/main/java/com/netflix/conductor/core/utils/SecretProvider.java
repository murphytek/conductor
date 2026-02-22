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

import java.util.Map;

/**
 * Provides decrypted workflow secrets for variable resolution. Implementations should cache
 * appropriately to avoid repeated decryption on every task evaluation.
 */
public interface SecretProvider {

    /** Returns all global secrets as a name→decryptedValue map. */
    Map<String, String> getAllSecretsDecrypted();

    /**
     * Returns merged secrets for a workflow: global secrets overlaid with workflow-scoped secrets.
     * Workflow-scoped secrets take priority over global secrets on name collision.
     */
    default Map<String, String> getSecretsForWorkflow(String workflowName) {
        return getAllSecretsDecrypted();
    }
}
