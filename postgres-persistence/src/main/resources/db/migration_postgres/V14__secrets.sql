CREATE TABLE secret (
    id SERIAL PRIMARY KEY,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    secret_name VARCHAR(255) NOT NULL,
    secret_value JSONB NOT NULL,
    created_by VARCHAR(255),
    description VARCHAR(1024)
);
CREATE UNIQUE INDEX unique_secret_name ON secret (secret_name);
