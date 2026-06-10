CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS candidate_schema;
CREATE SCHEMA IF NOT EXISTS document_schema;
CREATE SCHEMA IF NOT EXISTS employee_schema;
CREATE SCHEMA IF NOT EXISTS accounting_schema;

GRANT ALL PRIVILEGES ON SCHEMA auth_schema      TO hr_admin;
GRANT ALL PRIVILEGES ON SCHEMA candidate_schema  TO hr_admin;
GRANT ALL PRIVILEGES ON SCHEMA document_schema   TO hr_admin;
GRANT ALL PRIVILEGES ON SCHEMA employee_schema   TO hr_admin;
GRANT ALL PRIVILEGES ON SCHEMA accounting_schema TO hr_admin;

ALTER ROLE hr_admin SET search_path TO public, auth_schema, candidate_schema, document_schema, employee_schema, accounting_schema;

