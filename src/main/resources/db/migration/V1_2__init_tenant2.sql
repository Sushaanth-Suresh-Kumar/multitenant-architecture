CREATE SCHEMA IF NOT EXISTS tenant2;

CREATE TABLE IF NOT EXISTS tenant2.users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    firstname VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL
);
