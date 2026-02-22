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
package com.netflix.conductor.dao;

import java.util.List;
import java.util.Map;

/** Data access layer for workflow secrets. */
public interface SecretDAO {

    /** Create or update a secret. */
    void putSecret(String name, String value, String createdBy, String description);

    /** Get the decrypted value of a secret, or null if not found. */
    String getSecretValue(String name);

    /** Delete a secret by name. */
    void deleteSecret(String name);

    /** List all secret names. */
    List<String> listSecretNames();

    /** Get all secrets as a name→decryptedValue map. */
    Map<String, String> getAllSecrets();

    /** Check if a secret with the given name exists. */
    boolean secretExists(String name);

    // --- Workflow-scoped overloads ---

    /** Create or update a workflow-scoped secret. */
    void putSecret(String name, String value, String createdBy, String description, String workflowName);

    /** Get the decrypted value of a workflow-scoped secret, or null if not found. */
    String getSecretValue(String name, String workflowName);

    /** Delete a workflow-scoped secret by name. */
    void deleteSecret(String name, String workflowName);

    /** List secret names scoped to a specific workflow. */
    List<String> listSecretNames(String workflowName);

    /** Get all secrets for a specific workflow as a name→decryptedValue map. */
    Map<String, String> getAllSecrets(String workflowName);

    /** Check if a workflow-scoped secret exists. */
    boolean secretExists(String name, String workflowName);
}
