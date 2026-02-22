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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.retry.support.RetryTemplate;

import com.netflix.conductor.core.encryption.EncryptedEnvelope;
import com.netflix.conductor.core.encryption.FieldEncryptionService;
import com.netflix.conductor.dao.SecretDAO;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PostgresSecretDAO extends PostgresBaseDAO implements SecretDAO {

    private static final String UPSERT_SECRET =
            "INSERT INTO secret (secret_name, secret_value, created_by, description) "
                    + "VALUES (?, ?::jsonb, ?, ?) "
                    + "ON CONFLICT (secret_name) DO UPDATE SET "
                    + "secret_value = EXCLUDED.secret_value, "
                    + "modified_on = CURRENT_TIMESTAMP, "
                    + "created_by = EXCLUDED.created_by, "
                    + "description = EXCLUDED.description";

    private static final String GET_SECRET_VALUE =
            "SELECT secret_value FROM secret WHERE secret_name = ?";

    private static final String DELETE_SECRET = "DELETE FROM secret WHERE secret_name = ?";

    private static final String LIST_SECRET_NAMES =
            "SELECT secret_name FROM secret ORDER BY secret_name";

    private static final String GET_ALL_SECRETS =
            "SELECT secret_name, secret_value FROM secret ORDER BY secret_name";

    private static final String SECRET_EXISTS = "SELECT COUNT(*) FROM secret WHERE secret_name = ?";

    private final FieldEncryptionService encryptionService;

    public PostgresSecretDAO(
            RetryTemplate retryTemplate,
            ObjectMapper objectMapper,
            DataSource dataSource,
            FieldEncryptionService encryptionService) {
        super(retryTemplate, objectMapper, dataSource);
        this.encryptionService = encryptionService;
    }

    @Override
    public void putSecret(String name, String value, String createdBy, String description) {
        EncryptedEnvelope envelope = encryptionService.encrypt(value);
        String envelopeJson = toJson(envelope);
        executeWithTransaction(
                UPSERT_SECRET,
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
                GET_SECRET_VALUE,
                q -> {
                    q.addParameter(name);
                    var rs = q.executeQuery();
                    if (rs.next()) {
                        String envelopeJson = rs.getString(1);
                        EncryptedEnvelope envelope =
                                readValue(envelopeJson, EncryptedEnvelope.class);
                        return encryptionService.decrypt(envelope);
                    }
                    return null;
                });
    }

    @Override
    public void deleteSecret(String name) {
        executeWithTransaction(DELETE_SECRET, q -> q.addParameter(name).executeDelete());
    }

    @Override
    public List<String> listSecretNames() {
        return queryWithTransaction(LIST_SECRET_NAMES, q -> q.executeScalarList(String.class));
    }

    @Override
    public Map<String, String> getAllSecrets() {
        return queryWithTransaction(
                GET_ALL_SECRETS,
                q -> {
                    Map<String, String> secrets = new HashMap<>();
                    var rs = q.executeQuery();
                    while (rs.next()) {
                        String secretName = rs.getString(1);
                        String envelopeJson = rs.getString(2);
                        EncryptedEnvelope envelope =
                                readValue(envelopeJson, EncryptedEnvelope.class);
                        secrets.put(secretName, encryptionService.decrypt(envelope));
                    }
                    return secrets;
                });
    }

    @Override
    public boolean secretExists(String name) {
        return queryWithTransaction(SECRET_EXISTS, q -> q.addParameter(name).exists());
    }
}
