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
package com.netflix.conductor.rest.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.netflix.conductor.common.metadata.secrets.SecretRequest;
import com.netflix.conductor.service.SecretService;

import io.swagger.v3.oas.annotations.Operation;

import static com.netflix.conductor.rest.config.RequestMappingConstants.SECRETS;

@RestController
@RequestMapping(SECRETS)
@ConditionalOnBean(SecretService.class)
public class SecretResource {

    private final SecretService secretService;

    public SecretResource(SecretService secretService) {
        this.secretService = secretService;
    }

    @PutMapping("/{name}")
    @Operation(summary = "Create or update a secret")
    public void putSecret(
            @PathVariable("name") String name,
            @RequestBody SecretRequest request,
            @RequestParam(required = false) String workflowName) {
        if (workflowName != null) {
            secretService.putSecret(
                    name,
                    request.getValue(),
                    request.getCreatedBy(),
                    request.getDescription(),
                    workflowName);
        } else {
            secretService.putSecret(
                    name, request.getValue(), request.getCreatedBy(), request.getDescription());
        }
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get the value of a secret")
    public ResponseEntity<Map<String, String>> getSecret(
            @PathVariable("name") String name,
            @RequestParam(required = false) String workflowName) {
        String value =
                workflowName != null
                        ? secretService.getSecretValue(name, workflowName)
                        : secretService.getSecretValue(name);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("name", name, "value", value));
    }

    @GetMapping
    @Operation(summary = "List all secret names")
    public List<String> listSecretNames(@RequestParam(required = false) String workflowName) {
        return workflowName != null
                ? secretService.listSecretNames(workflowName)
                : secretService.listSecretNames();
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Delete a secret")
    public void deleteSecret(
            @PathVariable("name") String name,
            @RequestParam(required = false) String workflowName) {
        if (workflowName != null) {
            secretService.deleteSecret(name, workflowName);
        } else {
            secretService.deleteSecret(name);
        }
    }

    @GetMapping("/{name}/exists")
    @Operation(summary = "Check if a secret exists")
    public Map<String, Boolean> secretExists(
            @PathVariable("name") String name,
            @RequestParam(required = false) String workflowName) {
        boolean exists =
                workflowName != null
                        ? secretService.secretExists(name, workflowName)
                        : secretService.secretExists(name);
        return Map.of("exists", exists);
    }
}
