ALTER TABLE secret ADD COLUMN workflow_name VARCHAR(255);
DROP INDEX unique_secret_name;
CREATE UNIQUE INDEX unique_secret_name_workflow
    ON secret (secret_name, COALESCE(workflow_name, ''));
