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

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.netflix.conductor.core.exception.NonTransientException;

public class FieldEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final String ENVELOPE_ALG = "AES256GCM";
    private static final int ENVELOPE_VERSION = 1;

    private final EncryptionProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public FieldEncryptionService(EncryptionProperties properties) {
        this.properties = properties;
    }

    public EncryptedEnvelope encrypt(String plaintext) {
        var keyId = properties.activeKeyId();
        var keyBytes = resolveKey(keyId);
        var iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            cipher.updateAAD(keyId.getBytes(StandardCharsets.UTF_8));
            var ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new EncryptedEnvelope(
                    ENVELOPE_VERSION,
                    keyId,
                    ENVELOPE_ALG,
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(ciphertext));
        } catch (Exception e) {
            throw new NonTransientException("Failed to encrypt value", e);
        }
    }

    public String decrypt(EncryptedEnvelope envelope) {
        var keyBytes = resolveKey(envelope.kid());
        var iv = Base64.getDecoder().decode(envelope.iv());
        var ciphertext = Base64.getDecoder().decode(envelope.data());

        try {
            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            cipher.updateAAD(envelope.kid().getBytes(StandardCharsets.UTF_8));
            var plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new NonTransientException("Failed to decrypt value", e);
        }
    }

    private byte[] resolveKey(String keyId) {
        var keyProps = properties.keys().get(keyId);
        if (keyProps == null) {
            throw new NonTransientException("Unknown encryption key id: " + keyId);
        }
        return Base64.getDecoder().decode(keyProps.secret());
    }
}
