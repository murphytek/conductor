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
package com.netflix.conductor.postgres.dao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.retry.support.RetryTemplate;

import com.netflix.conductor.core.encryption.EncryptedEnvelope;
import com.netflix.conductor.core.encryption.FieldEncryptionService;
import com.netflix.conductor.dao.SecretDAO;

import com.netflix.conductor.postgres.util.Query;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PostgresSecretDAO extends PostgresBaseDAO implements SecretDAO {

    // --- Global secret queries (workflow_name IS NULL) ---

    private static final String UPSERT_GLOBAL_SECRET =
            "INSERT INTO secret (secret_name, secret_value, created_by, description) "
                    + "VALUES (?, ?::jsonb, ?, ?) "
                    + "ON CONFLICT (secret_name, COALESCE(workflow_name, '')) DO UPDATE SET "
                    + "secret_value = EXCLUDED.secret_value, "
                    + "modified_on = CURRENT_TIMESTAMP, "
                    + "created_by = EXCLUDED.created_by, "
                    + "description = EXCLUDED.description";

    private static final String GET_GLOBAL_SECRET_VALUE =
            "SELECT secret_value FROM secret WHERE secret_name = ? AND workflow_name IS NULL";

    private static final String DELETE_GLOBAL_SECRET =
            "DELETE FROM secret WHERE secret_name = ? AND workflow_name IS NULL";

    private static final String LIST_GLOBAL_SECRET_NAMES =
            "SELECT secret_name FROM secret WHERE workflow_name IS NULL ORDER BY secret_name";

    private static final String GET_ALL_GLOBAL_SECRETS =
            "SELECT secret_name, secret_value FROM secret WHERE workflow_name IS NULL ORDER BY secret_name";

    private static final String GLOBAL_SECRET_EXISTS =
            "SELECT COUNT(*) FROM secret WHERE secret_name = ? AND workflow_name IS NULL";

    // --- Workflow-scoped secret queries ---

    private static final String UPSERT_WORKFLOW_SECRET =
            "INSERT INTO secret (secret_name, secret_value, created_by, description, workflow_name) "
                    + "VALUES (?, ?::jsonb, ?, ?, ?) "
                    + "ON CONFLICT (secret_name, COALESCE(workflow_name, '')) DO UPDATE SET "
                    + "secret_value = EXCLUDED.secret_value, "
                    + "modified_on = CURRENT_TIMESTAMP, "
                    + "created_by = EXCLUDED.created_by, "
                    + "description = EXCLUDED.description";

    private static final String GET_WORKFLOW_SECRET_VALUE =
            "SELECT secret_value FROM secret WHERE secret_name = ? AND workflow_name = ?";

    private static final String DELETE_WORKFLOW_SECRET =
            "DELETE FROM secret WHERE secret_name = ? AND workflow_name = ?";

    private static final String LIST_WORKFLOW_SECRET_NAMES =
            "SELECT secret_name FROM secret WHERE workflow_name = ? ORDER BY secret_name";

    private static final String GET_ALL_WORKFLOW_SECRETS =
            "SELECT secret_name, secret_value FROM secret WHERE workflow_name = ? ORDER BY secret_name";

    private static final String WORKFLOW_SECRET_EXISTS =
            "SELECT COUNT(*) FROM secret WHERE secret_name = ? AND workflow_name = ?";

    private final FieldEncryptionService encryptionService;

    public PostgresSecretDAO(
            RetryTemplate retryTemplate,
            ObjectMapper objectMapper,
            DataSource dataSource,
            FieldEncryptionService encryptionService) {
        super(retryTemplate, objectMapper, dataSource);
        this.encryptionService = encryptionService;
    }

    // --- Global secret methods ---

    @Override
    public void putSecret(String name, String value, String createdBy, String description) {
        EncryptedEnvelope envelope = encryptionService.encrypt(value);
        String envelopeJson = toJson(envelope);
        executeWithTransaction(
                UPSERT_GLOBAL_SECRET,
                q ->
                        q.addParameter(name)
                                .addParameter(envelopeJson)
                                .addParameter(createdBy)
                                .addParameter(description)
                                .executeUpdate());
    }

    @Override
    public String getSecretValue(String name) {
        return queryWithTransaction(
                GET_GLOBAL_SECRET_VALUE,
                q -> {
                    q.addParameter(name);
                    var rs = q.executeQuery();
                    if (rs.next()) {
                        return decryptEnvelope(rs.getString(1));
                    }
                    return null;
                });
    }

    @Override
    public void deleteSecret(String name) {
        executeWithTransaction(DELETE_GLOBAL_SECRET, q -> q.addParameter(name).executeDelete());
    }

    @Override
    public List<String> listSecretNames() {
        return queryWithTransaction(
                LIST_GLOBAL_SECRET_NAMES, q -> q.executeScalarList(String.class));
    }

    @Override
    public Map<String, String> getAllSecrets() {
        return queryWithTransaction(GET_ALL_GLOBAL_SECRETS, q -> decryptResultSet(q));
    }

    @Override
    public boolean secretExists(String name) {
        return queryWithTransaction(GLOBAL_SECRET_EXISTS, q -> q.addParameter(name).exists());
    }

    // --- Workflow-scoped secret methods ---

    @Override
    public void putSecret(
            String name, String value, String createdBy, String description, String workflowName) {
        EncryptedEnvelope envelope = encryptionService.encrypt(value);
        String envelopeJson = toJson(envelope);
        executeWithTransaction(
                UPSERT_WORKFLOW_SECRET,
                q ->
                        q.addParameter(name)
                                .addParameter(envelopeJson)
                                .addParameter(createdBy)
                                .addParameter(description)
                                .addParameter(workflowName)
                                .executeUpdate());
    }

    @Override
    public String getSecretValue(String name, String workflowName) {
        return queryWithTransaction(
                GET_WORKFLOW_SECRET_VALUE,
                q -> {
                    q.addParameter(name).addParameter(workflowName);
                    var rs = q.executeQuery();
                    if (rs.next()) {
                        return decryptEnvelope(rs.getString(1));
                    }
                    return null;
                });
    }

    @Override
    public void deleteSecret(String name, String workflowName) {
        executeWithTransaction(
                DELETE_WORKFLOW_SECRET,
                q -> q.addParameter(name).addParameter(workflowName).executeDelete());
    }

    @Override
    public List<String> listSecretNames(String workflowName) {
        return queryWithTransaction(
                LIST_WORKFLOW_SECRET_NAMES,
                q -> {
                    q.addParameter(workflowName);
                    return q.executeScalarList(String.class);
                });
    }

    @Override
    public Map<String, String> getAllSecrets(String workflowName) {
        return queryWithTransaction(
                GET_ALL_WORKFLOW_SECRETS,
                q -> {
                    q.addParameter(workflowName);
                    return decryptResultSet(q);
                });
    }

    @Override
    public boolean secretExists(String name, String workflowName) {
        return queryWithTransaction(
                WORKFLOW_SECRET_EXISTS,
                q -> q.addParameter(name).addParameter(workflowName).exists());
    }

    // --- Helpers ---

    private String decryptEnvelope(String envelopeJson) {
        EncryptedEnvelope envelope = readValue(envelopeJson, EncryptedEnvelope.class);
        return encryptionService.decrypt(envelope);
    }

    private Map<String, String> decryptResultSet(Query q) throws SQLException {
        Map<String, String> secrets = new HashMap<>();
        var rs = q.executeQuery();
        while (rs.next()) {
            String secretName = rs.getString(1);
            secrets.put(secretName, decryptEnvelope(rs.getString(2)));
        }
        return secrets;
    }
}
