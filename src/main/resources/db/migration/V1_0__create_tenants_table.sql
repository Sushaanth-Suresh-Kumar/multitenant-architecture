CREATE TABLE IF NOT EXISTS public.tenants (
    id UUID PRIMARY KEY,
    schema_name VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,

    -- Add audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Add status field
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Add owner reference (will be populated during registration)
    owner_id UUID
);