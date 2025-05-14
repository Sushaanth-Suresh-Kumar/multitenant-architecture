-- Create tenants table in public schema for tenant management
CREATE TABLE IF NOT EXISTS public.tenants (
    id UUID PRIMARY KEY,
    schema_name VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    description TEXT
);

-- Insert default tenants (without creating schemas - this will be handled by the application)
INSERT INTO public.tenants (id, schema_name, display_name, description)
VALUES 
    ('f1b9a696-bca3-4d94-a3a6-e61265e8d83c', 'tenant_f1b9a696bca34d94a3a6e61265e8d83c', 'Tenant 1', 'Default tenant 1'),
    ('b0c2336c-d92b-4cc6-8566-2a0f65266b86', 'tenant_b0c2336cd92b4cc685662a0f65266b86', 'Tenant 2', 'Default tenant 2')
ON CONFLICT (schema_name) DO NOTHING;