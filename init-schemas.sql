-- =============================================================================
-- HR Platform — Full Database Schema (generated from Liquibase XML migrations)
-- This file is mounted as PostgreSQL init script for Railway deployment.
-- Target: PostgreSQL 16
-- =============================================================================
-- WARNING: This is a reference SQL extracted from Liquibase XML changesets.
-- In production the app uses Liquibase itself (ddl-auto=validate).
-- This file ensures Railway's Postgres has the schema on the very first boot
-- even before the Java app runs. The app's Liquibase then validates against it.
-- =============================================================================

SET client_encoding = 'UTF8';

-- =============================================================================
-- SCHEMAS
-- =============================================================================
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

-- =============================================================================
-- AUTH SCHEMA
-- =============================================================================

-- 001-create-users-table
CREATE TABLE auth_schema.users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    oauth_provider VARCHAR(20),
    oauth_provider_id VARCHAR(255),
    personal_data_consent BOOLEAN NOT NULL DEFAULT FALSE,
    push_notification_consent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_email ON auth_schema.users(email);
CREATE INDEX idx_users_role ON auth_schema.users(role);

-- 003-add-photo-url-to-users
ALTER TABLE auth_schema.users ADD COLUMN photo_url VARCHAR(1000);

-- 004-add-last-login-at
ALTER TABLE auth_schema.users ADD COLUMN last_login_at TIMESTAMP;
CREATE INDEX idx_users_last_login_at ON auth_schema.users(last_login_at);

-- 002-create-login-audit-log-table
CREATE TABLE auth_schema.login_audit_log (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth_schema.users(id) ON DELETE SET NULL,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    action VARCHAR(50) NOT NULL,
    success BOOLEAN NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_log_user_id ON auth_schema.login_audit_log(user_id);
CREATE INDEX idx_audit_log_email ON auth_schema.login_audit_log(email);
CREATE INDEX idx_audit_log_created_at ON auth_schema.login_audit_log(created_at);

-- 005-create-activity-log-table
CREATE TABLE auth_schema.activity_log (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    actor_id UUID NOT NULL,
    actor_email VARCHAR(255) NOT NULL,
    actor_role VARCHAR(30),
    action VARCHAR(50) NOT NULL,
    source VARCHAR(30) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_activity_log_actor_id ON auth_schema.activity_log(actor_id);
CREATE INDEX idx_activity_log_action ON auth_schema.activity_log(action);
CREATE INDEX idx_activity_log_entity_type ON auth_schema.activity_log(entity_type);
CREATE INDEX idx_activity_log_created_at ON auth_schema.activity_log(created_at);
CREATE INDEX idx_activity_log_action_created ON auth_schema.activity_log(action, created_at);

-- =============================================================================
-- CANDIDATE SCHEMA
-- =============================================================================

-- 001-create-positions (candidate)
CREATE TABLE candidate_schema.positions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 002-create-contract-types
CREATE TABLE candidate_schema.contract_types (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 002-contract-types seed data
INSERT INTO candidate_schema.contract_types (id, name) VALUES
    (gen_random_uuid(), 'Основной трудовой договор'),
    (gen_random_uuid(), 'Договор на стажировку'),
    (gen_random_uuid(), 'Дополнительное соглашение'),
    (gen_random_uuid(), 'Договор на испытательный срок'),
    (gen_random_uuid(), 'ГПХ'),
    (gen_random_uuid(), 'Договор о полной материальной ответственности');

-- 003-create-candidates
CREATE TABLE candidate_schema.candidates (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE,
    iin VARCHAR(12),
    phone VARCHAR(20),
    address TEXT,
    activity_type VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_candidates_user_id ON candidate_schema.candidates(user_id);
CREATE INDEX idx_candidates_email ON candidate_schema.candidates(email);

-- 004-create-vacancies
CREATE TABLE candidate_schema.vacancies (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    company_logo_url VARCHAR(500),
    location VARCHAR(30) NOT NULL,
    employment_type VARCHAR(30) NOT NULL,
    experience_level VARCHAR(30) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    short_description VARCHAR(500),
    skills TEXT,
    salary_min DECIMAL(12,2),
    salary_max DECIMAL(12,2),
    position_id UUID REFERENCES candidate_schema.positions(id),
    contract_type_id UUID REFERENCES candidate_schema.contract_types(id),
    requires_criminal_record BOOLEAN NOT NULL DEFAULT FALSE,
    requires_medical_cert BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_vacancies_active ON candidate_schema.vacancies(active);
CREATE INDEX idx_vacancies_category ON candidate_schema.vacancies(category);
CREATE INDEX idx_vacancies_location ON candidate_schema.vacancies(location);

-- 005-create-applications
CREATE TABLE candidate_schema.applications (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    candidate_id UUID NOT NULL REFERENCES candidate_schema.candidates(id),
    vacancy_id UUID NOT NULL REFERENCES candidate_schema.vacancies(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    iin VARCHAR(12) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    activity_type VARCHAR(255),
    contact_info TEXT,
    personal_data_consent BOOLEAN NOT NULL,
    interview_type VARCHAR(20),
    interview_date TIMESTAMP,
    interview_message TEXT,
    rejection_message TEXT,
    revision_message TEXT,
    revision_comment TEXT,
    documents_ready BOOLEAN NOT NULL DEFAULT FALSE,
    documents_signed BOOLEAN NOT NULL DEFAULT FALSE,
    promoted_at TIMESTAMP,
    promoted_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_applications_candidate ON candidate_schema.applications(candidate_id);
CREATE INDEX idx_applications_vacancy ON candidate_schema.applications(vacancy_id);
CREATE INDEX idx_applications_status ON candidate_schema.applications(status);

-- 006-create-application-documents
CREATE TABLE candidate_schema.application_documents (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES candidate_schema.applications(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    file_id VARCHAR(255) NOT NULL,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_app_docs_application ON candidate_schema.application_documents(application_id);

-- 007-create-notifications (candidate)
CREATE TABLE candidate_schema.notifications (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    reference_id UUID,
    reference_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notifications_user ON candidate_schema.notifications(user_id);
CREATE INDEX idx_notifications_read ON candidate_schema.notifications(is_read);

-- =============================================================================
-- DOCUMENT SCHEMA
-- =============================================================================

-- 001-create-documents
CREATE TABLE document_schema.documents (
    id UUID PRIMARY KEY,
    application_id UUID,
    candidate_id UUID,
    vacancy_id UUID,
    document_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    file_name VARCHAR(500) NOT NULL,
    minio_object_key VARCHAR(1000) NOT NULL,
    content_type VARCHAR(200),
    file_size BIGINT,
    signed_at TIMESTAMP,
    signed_by UUID,
    signature_data TEXT,
    qr_code_url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_documents_application_id ON document_schema.documents(application_id);
CREATE INDEX idx_documents_candidate_id ON document_schema.documents(candidate_id);

-- 003-add-dual-qr-and-signature-meta
ALTER TABLE document_schema.documents ADD COLUMN verification_qr_url VARCHAR(1000);
ALTER TABLE document_schema.documents ADD COLUMN signature_qr_url VARCHAR(1000);
ALTER TABLE document_schema.documents ADD COLUMN signature_hash VARCHAR(128);
ALTER TABLE document_schema.documents ADD COLUMN signer_iin VARCHAR(12);

-- 004-make-application-id-nullable-add-uploaded-by
ALTER TABLE document_schema.documents ADD COLUMN uploaded_by UUID;

-- 002-create-document-templates
CREATE TABLE document_schema.document_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    description TEXT,
    file_name VARCHAR(500) NOT NULL,
    minio_object_key VARCHAR(1000) NOT NULL,
    content_type VARCHAR(200),
    file_size BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_templates_document_type ON document_schema.document_templates(document_type);

-- =============================================================================
-- EMPLOYEE SCHEMA
-- =============================================================================

-- 001-create-departments
CREATE TABLE employee_schema.departments (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    parent_id UUID,
    manager_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 002-create-work-schedules
CREATE TABLE employee_schema.work_schedules (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    start_time TIME,
    end_time TIME,
    working_days VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 003-create-employees
CREATE TABLE employee_schema.employees (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    candidate_id UUID,
    application_id UUID,
    department_id UUID REFERENCES employee_schema.departments(id),
    work_schedule_id UUID REFERENCES employee_schema.work_schedules(id),
    position_name VARCHAR(200) NOT NULL,
    contract_type VARCHAR(50),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    iin VARCHAR(12),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    birth_date DATE,
    hire_date DATE NOT NULL,
    fire_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PROBATION',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_employees_user_id ON employee_schema.employees(user_id);
CREATE INDEX idx_employees_department_id ON employee_schema.employees(department_id);

-- 004-create-leaves
CREATE TABLE employee_schema.leaves (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee_schema.employees(id),
    leave_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_count INT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason TEXT,
    reject_reason TEXT,
    approved_by UUID,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_leaves_employee_id ON employee_schema.leaves(employee_id);

-- 005-create-time-entries
CREATE TABLE employee_schema.time_entries (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee_schema.employees(id),
    date DATE NOT NULL,
    check_in TIME,
    check_out TIME,
    hours_worked DECIMAL(4,2),
    status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_time_entries_employee_date ON employee_schema.time_entries(employee_id, date);

-- 006-create-attendance-records
CREATE TABLE employee_schema.attendance_records (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee_schema.employees(id),
    date DATE NOT NULL,
    present BOOLEAN NOT NULL DEFAULT FALSE,
    check_in TIME,
    check_out TIME,
    location VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_attendance_employee_date ON employee_schema.attendance_records(employee_id, date);

-- 007-create-news
CREATE TABLE employee_schema.news (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(1000),
    author_id UUID NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_news_published ON employee_schema.news(published);

-- 008-create-employee-notifications
CREATE TABLE employee_schema.employee_notifications (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'INFO',
    read BOOLEAN NOT NULL DEFAULT FALSE,
    reference_id UUID,
    reference_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_emp_notif_employee_id ON employee_schema.employee_notifications(employee_id);

-- 009-create-certificate-requests
CREATE TABLE employee_schema.certificate_requests (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee_schema.employees(id),
    certificate_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    generated_document_id UUID,
    reject_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cert_requests_employee_id ON employee_schema.certificate_requests(employee_id);

-- 010-create-positions (employee)
CREATE TABLE employee_schema.positions (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 011-create-document-requests
CREATE TABLE employee_schema.document_requests (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee_schema.employees(id),
    document_type VARCHAR(30) NOT NULL,
    comment TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    file_url VARCHAR(1000),
    reject_reason TEXT,
    signed_by UUID,
    signed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_doc_requests_employee_id ON employee_schema.document_requests(employee_id);
CREATE INDEX idx_doc_requests_status ON employee_schema.document_requests(status);

-- 012-create-attendance-ingestion-events
CREATE TABLE employee_schema.attendance_ingestion_events (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee_schema.employees(id),
    source VARCHAR(20) NOT NULL,
    external_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    location VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL,
    ingestion_status VARCHAR(20) NOT NULL,
    raw_payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (source, external_event_id)
);
CREATE INDEX idx_attendance_ingestion_employee_id ON employee_schema.attendance_ingestion_events(employee_id);
CREATE INDEX idx_attendance_ingestion_occurred_at ON employee_schema.attendance_ingestion_events(occurred_at);

-- 013-create-employee-payroll-profiles
CREATE TABLE employee_schema.employee_payroll_profiles (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL UNIQUE REFERENCES employee_schema.employees(id),
    monthly_salary DECIMAL(14,2) NOT NULL,
    monthly_bonus DECIMAL(14,2) NOT NULL DEFAULT 0,
    effective_from DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payroll_profiles_employee_id ON employee_schema.employee_payroll_profiles(employee_id);

-- 014-create-holiday-calendar-days
CREATE TABLE employee_schema.holiday_calendar_days (
    id UUID PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    is_paid BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_holiday_calendar_date ON employee_schema.holiday_calendar_days(holiday_date);

-- 015-create-payroll-calculations
CREATE TABLE employee_schema.payroll_calculations (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee_schema.employees(id),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    working_days INT NOT NULL,
    worked_days INT NOT NULL,
    sick_days INT NOT NULL,
    unpaid_days INT NOT NULL,
    vacation_days INT NOT NULL,
    business_trip_days INT NOT NULL,
    weekend_days INT NOT NULL,
    holiday_days INT NOT NULL,
    absent_days INT NOT NULL,
    monthly_salary DECIMAL(14,2) NOT NULL,
    monthly_bonus DECIMAL(14,2) NOT NULL,
    gross_salary DECIMAL(14,2) NOT NULL,
    opv_amount DECIMAL(14,2) NOT NULL,
    osms_amount DECIMAL(14,2) NOT NULL,
    iit_amount DECIMAL(14,2) NOT NULL,
    net_salary DECIMAL(14,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payroll_calculations_employee_created_at ON employee_schema.payroll_calculations(employee_id, created_at);
