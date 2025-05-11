CREATE SCHEMA IF NOT EXISTS tenant1;

CREATE TABLE IF NOT EXISTS tenant1.users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    firstname VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL
);

