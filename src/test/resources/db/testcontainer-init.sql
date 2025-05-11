-- Create tenant1 schema and users table
CREATE SCHEMA IF NOT EXISTS tenant1;

CREATE TABLE IF NOT EXISTS tenant1.users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    firstname VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL
);

-- Create tenant2 schema and users table
CREATE SCHEMA IF NOT EXISTS tenant2;

CREATE TABLE IF NOT EXISTS tenant2.users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    firstname VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL
);

-- Create flyway_admin schema for Flyway migrations
CREATE SCHEMA IF NOT EXISTS flyway_admin;

