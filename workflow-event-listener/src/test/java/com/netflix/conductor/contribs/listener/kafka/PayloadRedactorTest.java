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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.netflix.conductor.contribs.listener.kafka.PayloadRedactor.REDACTED;

import static org.junit.Assert.*;

public class PayloadRedactorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PayloadRedactor redactor =
            new PayloadRedactor(PayloadRedactor.DEFAULT_KEY_PATTERN, objectMapper);

    @Test
    public void masksSecretNamedValuesAtAnyDepth() {
        Map<String, Object> input =
                Map.of(
                        "catalogAuth",
                        Map.of("clientId", "id", "clientSecret", "s", "tokenUrl", "https://t"),
                        "environment",
                        Map.of(
                                "ANTHROPIC_API_KEY", "k",
                                "CLAUDE_CODE_OAUTH_TOKEN", "t",
                                "AWS_SECRET_ACCESS_KEY", "a",
                                "MODEL", "opus"),
                        "lines",
                        List.of(Map.of("password", "p", "line", "5.3")),
                        "_hd",
                        Map.of("security", Map.of("authorization", "Bearer x")));

        Map<String, Object> masked = redactor.redact(input);

        assertEquals(
                Map.of("clientId", "id", "clientSecret", REDACTED, "tokenUrl", "https://t"),
                masked.get("catalogAuth"));
        assertEquals(
                Map.of(
                        "ANTHROPIC_API_KEY", REDACTED,
                        "CLAUDE_CODE_OAUTH_TOKEN", REDACTED,
                        "AWS_SECRET_ACCESS_KEY", REDACTED,
                        "MODEL", "opus"),
                masked.get("environment"));
        assertEquals(List.of(Map.of("password", REDACTED, "line", "5.3")), masked.get("lines"));
        assertEquals(Map.of("security", Map.of("authorization", REDACTED)), masked.get("_hd"));
    }

    @Test
    public void keepsNumbersBooleansAndLookalikeKeys() {
        Map<String, Object> input =
                Map.of(
                        "inputTokens",
                        1200,
                        "withNesApiToken",
                        true,
                        "tokenUsage",
                        Map.of("model", "opus"),
                        "accessTokenScopeMappingIntroducedVersion",
                        "2.3.0",
                        "runKey",
                        "lane-1",
                        "passed_checks",
                        List.of("a"));

        assertEquals(input, redactor.redact(input));
    }

    @Test
    public void masksSecretsInsideJsonStrings() throws Exception {
        String env = objectMapper.writeValueAsString(Map.of("OPENAI_API_KEY", "k", "HOME", "/h"));

        Map<String, Object> masked = redactor.redact(Map.of("respawnEnvironmentJson", env));

        assertEquals(
                Map.of("OPENAI_API_KEY", REDACTED, "HOME", "/h"),
                objectMapper.readValue((String) masked.get("respawnEnvironmentJson"), Map.class));
    }

    @Test
    public void leavesOtherStringsAsTheyAre() {
        Map<String, Object> input = Map.of("a", "{not json}", "b", " {\"x\": 1} ", "c", "plain");

        assertEquals(input, redactor.redact(input));
    }

    @Test
    public void doesNotChangeTheMapItIsGiven() {
        Map<String, Object> env = new HashMap<>(Map.of("GH_TOKEN", "t"));
        Map<String, Object> input = new HashMap<>(Map.of("environment", env));

        redactor.redact(input);

        assertEquals("t", env.get("GH_TOKEN"));
    }

    @Test
    public void blankPatternTurnsMaskingOff() {
        Map<String, Object> input = Map.of("clientSecret", "s");

        assertSame(input, new PayloadRedactor(" ", objectMapper).redact(input));
    }
}
