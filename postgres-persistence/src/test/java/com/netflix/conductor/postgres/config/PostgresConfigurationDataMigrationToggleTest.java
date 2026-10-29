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
package com.netflix.conductor.postgres.config;

import java.util.Arrays;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.Assert.assertTrue;

public class PostgresConfigurationDataMigrationToggleTest {

    // A database of this test's own, kept up between the two starts (TC_DAEMON).
    private static final String URL =
            "jdbc:tc:postgresql:11.15-alpine:///conductordatamigrationtoggle?TC_DAEMON=true";

    @Test
    public void startsWithDataMigrationsDisabledAfterTheyWereApplied() {
        DataSource dataSource = new DriverManagerDataSource(URL);
        PostgresProperties properties = new PostgresProperties();

        properties.setApplyDataMigrations(true);
        new PostgresConfiguration(dataSource, properties).flywayForPrimaryDb().migrate();

        // An existing deployment turns data migrations off: the applied V13.2 and V18.1 are no
        // longer on the locations, which must not fail validation.
        properties.setApplyDataMigrations(false);
        Flyway flyway = new PostgresConfiguration(dataSource, properties).flywayForPrimaryDb();
        flyway.migrate();

        assertTrue(
                Arrays.stream(flyway.info().applied())
                        .anyMatch(m -> "13.2".equals(m.getVersion().getVersion())));
    }
}
