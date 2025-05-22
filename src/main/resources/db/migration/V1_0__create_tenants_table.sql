-- Create public schema tables
CREATE SCHEMA IF NOT EXISTS public;

-- Create tenants table
CREATE TABLE IF NOT EXISTS public.tenants (
    id UUID PRIMARY KEY,
    schema_name VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    owner_id UUID
);

-- Create authentication users table
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    firstname VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants(id)
);

-- Create verification tokens table
CREATE TABLE IF NOT EXISTS public.verification_tokens (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- Create employee invitations table
CREATE TABLE IF NOT EXISTS public.employee_invitations (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL,
    invited_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES public.tenants(id),
    CONSTRAINT fk_invited_by FOREIGN KEY (invited_by) REFERENCES public.users(id)
);

-- Create refresh tokens table (if implementing refresh tokens)
CREATE TABLE IF NOT EXISTS public.refresh_tokens (
    id UUID PRIMARY KEY,
    token TEXT NOT NULL,
    user_id UUID NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES public.users(id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON public.users(email);
CREATE INDEX IF NOT EXISTS idx_tokens_email_token ON public.verification_tokens(email, token);
CREATE INDEX IF NOT EXISTS idx_invitations_email ON public.employee_invitations(email);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON public.refresh_tokens(token);