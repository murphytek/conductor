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
package com.netflix.conductor.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import com.netflix.conductor.core.utils.SecretProvider;
import com.netflix.conductor.dao.SecretDAO;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

@Service
@ConditionalOnBean(SecretDAO.class)
public class SecretServiceImpl implements SecretService, SecretProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretServiceImpl.class);
    private static final String CACHE_KEY = "all";

    private final SecretDAO secretDAO;
    private final LoadingCache<String, Map<String, String>> secretsCache;

    public SecretServiceImpl(SecretDAO secretDAO) {
        this.secretDAO = secretDAO;
        this.secretsCache =
                Caffeine.newBuilder()
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .maximumSize(1)
                        .build(key -> secretDAO.getAllSecrets());
    }

    @Override
    public void putSecret(String name, String value, String createdBy, String description) {
        secretDAO.putSecret(name, value, createdBy, description);
        secretsCache.invalidateAll();
    }

    @Override
    public String getSecretValue(String name) {
        return secretDAO.getSecretValue(name);
    }

    @Override
    public void deleteSecret(String name) {
        secretDAO.deleteSecret(name);
        secretsCache.invalidateAll();
    }

    @Override
    public List<String> listSecretNames() {
        return secretDAO.listSecretNames();
    }

    @Override
    public boolean secretExists(String name) {
        return secretDAO.secretExists(name);
    }

    @Override
    public Map<String, String> getAllSecretsDecrypted() {
        try {
            return secretsCache.get(CACHE_KEY);
        } catch (Exception e) {
            LOGGER.error("Failed to load secrets from cache", e);
            return Collections.emptyMap();
        }
    }
}
