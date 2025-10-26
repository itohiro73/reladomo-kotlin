-- ========================================
-- ChronoStaff: Bitemporal HR Management Demo
-- ========================================
-- This schema demonstrates the full capabilities of Reladomo's bitemporal data model
-- through practical HR scenarios including:
-- - Organizational restructuring
-- - Employee promotions and transfers
-- - Salary adjustments
-- - Future planning and plan corrections

-- Create sequence table for Reladomo
CREATE TABLE IF NOT EXISTS MITHRA_SEQUENCE (
    SEQUENCE_NAME VARCHAR(64) NOT NULL PRIMARY KEY,
    NEXT_ID BIGINT NOT NULL
);

-- ========================================
-- Table Definitions
-- ========================================

-- Positions table (Non-temporal - simple master data)
CREATE TABLE POSITIONS (
    ID BIGINT NOT NULL PRIMARY KEY,
    NAME VARCHAR(100) NOT NULL,
    LEVEL INT NOT NULL,
    DESCRIPTION VARCHAR(500)
);

-- Departments table (Unitemporal - tracks organizational structure changes)
CREATE TABLE DEPARTMENTS (
    ID BIGINT NOT NULL,
    NAME VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(1000),
    PARENT_DEPARTMENT_ID BIGINT,

    -- Unitemporal columns (processing time only)
    PROCESSING_FROM TIMESTAMP NOT NULL,
    PROCESSING_THRU TIMESTAMP NOT NULL,

    PRIMARY KEY (ID, PROCESSING_FROM)
);

-- Employees table (Unitemporal - tracks employee master data changes)
CREATE TABLE EMPLOYEES (
    ID BIGINT NOT NULL,
    EMPLOYEE_NUMBER VARCHAR(20) NOT NULL,
    NAME VARCHAR(100) NOT NULL,
    EMAIL VARCHAR(200) NOT NULL,
    HIRE_DATE TIMESTAMP NOT NULL,

    -- Unitemporal columns (processing time only)
    PROCESSING_FROM TIMESTAMP NOT NULL,
    PROCESSING_THRU TIMESTAMP NOT NULL,

    PRIMARY KEY (ID, PROCESSING_FROM)
);

-- Employee Assignments table (Bitemporal - tracks who works where and when)
CREATE TABLE EMPLOYEE_ASSIGNMENTS (
    ID BIGINT NOT NULL,
    EMPLOYEE_ID BIGINT NOT NULL,
    DEPARTMENT_ID BIGINT NOT NULL,
    POSITION_ID BIGINT NOT NULL,

    -- Audit trail
    UPDATED_BY VARCHAR(100),

    -- Bitemporal columns
    BUSINESS_FROM TIMESTAMP NOT NULL,
    BUSINESS_THRU TIMESTAMP NOT NULL,
    PROCESSING_FROM TIMESTAMP NOT NULL,
    PROCESSING_THRU TIMESTAMP NOT NULL,

    PRIMARY KEY (ID, BUSINESS_FROM, PROCESSING_FROM),
    CONSTRAINT UK_EMPLOYEE_ASSIGNMENTS_BITEMPORAL UNIQUE (EMPLOYEE_ID, BUSINESS_THRU, PROCESSING_THRU)
);

-- Salaries table (Bitemporal - tracks salary changes over time)
CREATE TABLE SALARIES (
    ID BIGINT NOT NULL,
    EMPLOYEE_ID BIGINT NOT NULL,
    AMOUNT DECIMAL(19, 2) NOT NULL,
    CURRENCY VARCHAR(3) NOT NULL,

    -- Audit trail
    UPDATED_BY VARCHAR(100),

    -- Bitemporal columns
    BUSINESS_FROM TIMESTAMP NOT NULL,
    BUSINESS_THRU TIMESTAMP NOT NULL,
    PROCESSING_FROM TIMESTAMP NOT NULL,
    PROCESSING_THRU TIMESTAMP NOT NULL,

    PRIMARY KEY (ID, BUSINESS_FROM, PROCESSING_FROM),
    CONSTRAINT UK_SALARIES_BITEMPORAL UNIQUE (EMPLOYEE_ID, BUSINESS_THRU, PROCESSING_THRU)
);

-- ========================================
-- Master Data: Positions (Non-temporal)
-- ========================================
INSERT INTO POSITIONS (ID, NAME, LEVEL, DESCRIPTION) VALUES
(1, 'Junior Developer', 1, 'Entry-level software developer'),
(2, 'Senior Developer', 2, 'Experienced software developer'),
(3, 'Team Lead', 3, 'Technical team leader'),
(4, 'Engineering Manager', 4, 'Engineering department manager'),
(5, 'Sales Representative', 1, 'Sales team member'),
(6, 'Sales Manager', 3, 'Sales team leader');

-- ========================================
-- Organizational Structure: Departments (Unitemporal)
-- ========================================
-- TIMEZONE POLICY: All timestamps are stored in UTC
-- Scenario: Company reorganization - splitting Engineering into Backend and Frontend teams

-- Step 1 (2024/12/31 15:00 UTC = 2025/01/01 00:00 JST): Initial org structure
INSERT INTO DEPARTMENTS (ID, NAME, DESCRIPTION, PARENT_DEPARTMENT_ID, PROCESSING_FROM, PROCESSING_THRU) VALUES
(1, 'Engineering', 'Product development and engineering', NULL, '2024-12-31 15:00:00', '9999-12-01 23:59:00');

INSERT INTO DEPARTMENTS (ID, NAME, DESCRIPTION, PARENT_DEPARTMENT_ID, PROCESSING_FROM, PROCESSING_THRU) VALUES
(2, 'Sales', 'Sales and business development', NULL, '2024-12-31 15:00:00', '9999-12-01 23:59:00');

-- Step 2 (2025/03/31 15:00 UTC = 2025/04/01 00:00 JST): Split Engineering into teams
INSERT INTO DEPARTMENTS (ID, NAME, DESCRIPTION, PARENT_DEPARTMENT_ID, PROCESSING_FROM, PROCESSING_THRU) VALUES
(3, 'Backend Team', 'Backend development and APIs', 1, '2025-03-31 15:00:00', '9999-12-01 23:59:00');

INSERT INTO DEPARTMENTS (ID, NAME, DESCRIPTION, PARENT_DEPARTMENT_ID, PROCESSING_FROM, PROCESSING_THRU) VALUES
(4, 'Frontend Team', 'Frontend and user experience', 1, '2025-03-31 15:00:00', '9999-12-01 23:59:00');

-- ========================================
-- Employee Master Data: Employees (Unitemporal)
-- ========================================
-- Scenario: Track employee information changes (email updates, etc.)

-- Alice - hired Jan 1, 2024 (email change in March)
INSERT INTO EMPLOYEES (ID, EMPLOYEE_NUMBER, NAME, EMAIL, HIRE_DATE, PROCESSING_FROM, PROCESSING_THRU) VALUES
(1, 'EMP001', 'Alice Johnson', 'alice.johnson@company.com', '2023-12-31 15:00:00', '2023-12-31 15:00:00', '2025-02-28 15:00:00');

INSERT INTO EMPLOYEES (ID, EMPLOYEE_NUMBER, NAME, EMAIL, HIRE_DATE, PROCESSING_FROM, PROCESSING_THRU) VALUES
(1, 'EMP001', 'Alice Johnson', 'alice.j@newdomain.com', '2023-12-31 15:00:00', '2025-02-28 15:00:00', '9999-12-01 23:59:00');

-- Bob - hired Mar 1, 2024 (no changes)
INSERT INTO EMPLOYEES (ID, EMPLOYEE_NUMBER, NAME, EMAIL, HIRE_DATE, PROCESSING_FROM, PROCESSING_THRU) VALUES
(2, 'EMP002', 'Bob Smith', 'bob.smith@company.com', '2025-02-28 15:00:00', '2025-02-28 15:00:00', '9999-12-01 23:59:00');

-- Charlie - hired Jun 1, 2024 (no changes)
INSERT INTO EMPLOYEES (ID, EMPLOYEE_NUMBER, NAME, EMAIL, HIRE_DATE, PROCESSING_FROM, PROCESSING_THRU) VALUES
(3, 'EMP003', 'Charlie Davis', 'charlie.davis@company.com', '2025-05-31 15:00:00', '2025-05-31 15:00:00', '9999-12-01 23:59:00');

-- ========================================
-- Employee Assignments (Bitemporal)
-- ========================================
-- TIMEZONE POLICY: All timestamps are stored in UTC
-- Scenario: Complex career progression with promotions, team changes, and future planning
-- Demonstrates: Initial assignments → Promotions → Lateral moves → Future plans → Corrections
-- Key principle: 2D plane should have NO gaps and NO overlaps when properly chained

-- ALICE'S CAREER PROGRESSION
-- Alice: Junior Dev in Engineering → Promoted to Senior Dev → Moved to Frontend as Team Lead

-- Step 1 (2024/01/01 00:00 JST): Alice hired as Junior Developer in Engineering
INSERT INTO EMPLOYEE_ASSIGNMENTS (ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(1, 1, 1, 1, 'hr-system', '2023-12-31 15:00:00', '9999-12-01 23:59:00', '2023-12-31 15:00:00', '2025-03-31 15:00:00');

-- Step 2 (2025/04/01 00:00 JST): HR plans Alice's promotion to Senior Dev effective Jul 1
-- This splits ID 1's business period
INSERT INTO EMPLOYEE_ASSIGNMENTS (ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(2, 1, 1, 1, 'hr-system', '2023-12-31 15:00:00', '2025-06-30 15:00:00', '2025-03-31 15:00:00', '9999-12-01 23:59:00');

-- Future plan - planned to be Backend Senior Dev (corrected later)
INSERT INTO EMPLOYEE_ASSIGNMENTS (ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(3, 1, 3, 2, 'alice.manager@company.com', '2025-06-30 15:00:00', '9999-12-01 23:59:00', '2025-03-31 15:00:00', '2025-05-14 15:00:00');

-- Step 3 (2025/05/15 00:00 JST): Correction - Alice should go to Frontend Team Lead, not Backend Senior Dev
INSERT INTO EMPLOYEE_ASSIGNMENTS (ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(4, 1, 4, 3, 'alice.manager@company.com', '2025-06-30 15:00:00', '9999-12-01 23:59:00', '2025-05-14 15:00:00', '2025-05-31 15:00:00');

-- Step 4 (2025/06/01 00:00 JST): Plan another promotion for Alice to Engineering Manager effective Oct 1
INSERT INTO EMPLOYEE_ASSIGNMENTS (ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(5, 1, 4, 3, 'alice.manager@company.com', '2025-06-30 15:00:00', '2025-09-30 15:00:00', '2025-05-31 15:00:00', '9999-12-01 23:59:00');

INSERT INTO EMPLOYEE_ASSIGNMENTS (ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(6, 1, 1, 4, 'ceo@company.com', '2025-09-30 15:00:00', '9999-12-01 23:59:00', '2025-05-31 15:00:00', '9999-12-01 23:59:00');

-- BOB'S CAREER (Stable Senior Developer)
-- Bob: Hired as Senior Dev in Frontend, stays stable

INSERT INTO EMPLOYEE_ASSIGNMENTS (ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(7, 2, 1, 2, 'hr-system', '2025-02-28 15:00:00', '9999-12-01 23:59:00', '2025-02-28 15:00:00', '2025-03-31 15:00:00');

INSERT INTO EMPLOYEE_ASSIGNMENTS (ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(8, 2, 4, 2, 'hr-system', '2025-02-28 15:00:00', '9999-12-01 23:59:00', '2025-03-31 15:00:00', '9999-12-01 23:59:00');

-- CHARLIE'S CAREER (Career Change: Sales → Developer)
-- Charlie: Sales Rep → Moved to Backend as Junior Dev

INSERT INTO EMPLOYEE_ASSIGNMENTS (ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(9, 3, 2, 5, 'hr-system', '2025-05-31 15:00:00', '9999-12-01 23:59:00', '2025-05-31 15:00:00', '2025-06-30 15:00:00');

INSERT INTO EMPLOYEE_ASSIGNMENTS (ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(10, 3, 2, 5, 'hr-system', '2025-05-31 15:00:00', '2025-07-31 15:00:00', '2025-06-30 15:00:00', '9999-12-01 23:59:00');

INSERT INTO EMPLOYEE_ASSIGNMENTS (ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(11, 3, 3, 1, 'charlie.manager@company.com', '2025-07-31 15:00:00', '9999-12-01 23:59:00', '2025-06-30 15:00:00', '9999-12-01 23:59:00');

-- ========================================
-- Salaries (Bitemporal)
-- ========================================
-- TIMEZONE POLICY: All timestamps are stored in UTC
-- Scenario: Salary changes aligned with role changes and annual adjustments

-- ALICE'S SALARY PROGRESSION
-- Alice: 60000 → 75000 (Senior Dev promotion) → 90000 (Team Lead promotion) → 120000 (Manager promotion)

INSERT INTO SALARIES (ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(1, 1, 60000.00, 'USD', 'payroll-system', '2023-12-31 15:00:00', '9999-12-01 23:59:00', '2023-12-31 15:00:00', '2025-03-31 15:00:00');

INSERT INTO SALARIES (ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(2, 1, 60000.00, 'USD', 'payroll-system', '2023-12-31 15:00:00', '2025-06-30 15:00:00', '2025-03-31 15:00:00', '9999-12-01 23:59:00');

INSERT INTO SALARIES (ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(3, 1, 75000.00, 'USD', 'payroll-system', '2025-06-30 15:00:00', '9999-12-01 23:59:00', '2025-03-31 15:00:00', '2025-05-31 15:00:00');

INSERT INTO SALARIES (ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(4, 1, 75000.00, 'USD', 'payroll-system', '2025-06-30 15:00:00', '2025-09-30 15:00:00', '2025-05-31 15:00:00', '9999-12-01 23:59:00');

INSERT INTO SALARIES (ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(5, 1, 90000.00, 'USD', 'payroll-system', '2025-09-30 15:00:00', '9999-12-01 23:59:00', '2025-05-31 15:00:00', '9999-12-01 23:59:00');

-- BOB'S SALARY (Annual raise only)
-- Bob: 80000 → 85000 (annual adjustment)

INSERT INTO SALARIES (ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(6, 2, 80000.00, 'USD', 'payroll-system', '2025-02-28 15:00:00', '9999-12-01 23:59:00', '2025-02-28 15:00:00', '2025-05-31 15:00:00');

INSERT INTO SALARIES (ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(7, 2, 80000.00, 'USD', 'payroll-system', '2025-02-28 15:00:00', '2025-06-30 15:00:00', '2025-05-31 15:00:00', '9999-12-01 23:59:00');

INSERT INTO SALARIES (ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(8, 2, 85000.00, 'USD', 'payroll-system', '2025-06-30 15:00:00', '9999-12-01 23:59:00', '2025-05-31 15:00:00', '9999-12-01 23:59:00');

-- CHARLIE'S SALARY (Career change adjustment)
-- Charlie: 50000 (Sales) → 55000 (Junior Dev, lower than typical dev but higher than sales)

INSERT INTO SALARIES (ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(9, 3, 50000.00, 'USD', 'payroll-system', '2025-05-31 15:00:00', '9999-12-01 23:59:00', '2025-05-31 15:00:00', '2025-06-30 15:00:00');

INSERT INTO SALARIES (ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(10, 3, 50000.00, 'USD', 'payroll-system', '2025-05-31 15:00:00', '2025-07-31 15:00:00', '2025-06-30 15:00:00', '9999-12-01 23:59:00');

INSERT INTO SALARIES (ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(11, 3, 55000.00, 'USD', 'payroll-system', '2025-07-31 15:00:00', '9999-12-01 23:59:00', '2025-06-30 15:00:00', '9999-12-01 23:59:00');
