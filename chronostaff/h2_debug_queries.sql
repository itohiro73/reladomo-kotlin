-- H2 Debug Queries for ChronoStaff Bitemporal Data
-- Access H2 Console: http://localhost:8082/h2-console
-- JDBC URL: jdbc:h2:mem:chronostaff
-- User: sa, Password: (empty)

-- =============================================================================
-- 1. COMPANIES (Non-temporal)
-- =============================================================================
SELECT
    ID,
    NAME,
    CREATED_AT
FROM COMPANIES
ORDER BY ID;

-- =============================================================================
-- 2. DEPARTMENTS (Unitemporal - PROCESSING_FROM/THRU)
-- =============================================================================
SELECT
    ID,
    COMPANY_ID,
    NAME,
    DESCRIPTION,
    PROCESSING_FROM,
    PROCESSING_THRU,
    CASE
        WHEN PROCESSING_THRU = '9999-12-01 23:59:00' THEN 'CURRENT'
        ELSE 'HISTORICAL'
    END AS STATUS
FROM DEPARTMENTS
ORDER BY COMPANY_ID, NAME, PROCESSING_FROM;

-- =============================================================================
-- 3. POSITIONS (Unitemporal - PROCESSING_FROM/THRU)
-- =============================================================================
SELECT
    ID,
    COMPANY_ID,
    NAME,
    LEVEL,
    DESCRIPTION,
    PROCESSING_FROM,
    PROCESSING_THRU,
    CASE
        WHEN PROCESSING_THRU = '9999-12-01 23:59:00' THEN 'CURRENT'
        ELSE 'HISTORICAL'
    END AS STATUS
FROM POSITIONS
ORDER BY COMPANY_ID, LEVEL DESC, PROCESSING_FROM;

-- =============================================================================
-- 4. EMPLOYEES (Unitemporal - PROCESSING_FROM/THRU)
-- =============================================================================
SELECT
    ID,
    COMPANY_ID,
    EMPLOYEE_NUMBER,
    NAME,
    EMAIL,
    HIRE_DATE,
    PROCESSING_FROM,
    PROCESSING_THRU,
    CASE
        WHEN PROCESSING_THRU = '9999-12-01 23:59:00' THEN 'CURRENT'
        ELSE 'HISTORICAL'
    END AS STATUS
FROM EMPLOYEES
ORDER BY COMPANY_ID, EMPLOYEE_NUMBER, PROCESSING_FROM;

-- =============================================================================
-- 5. EMPLOYEE_ASSIGNMENTS (Bitemporal - BUSINESS + PROCESSING)
-- =============================================================================
SELECT
    ID,
    EMPLOYEE_ID,
    DEPARTMENT_ID,
    POSITION_ID,
    UPDATED_BY,
    BUSINESS_FROM,
    BUSINESS_THRU,
    PROCESSING_FROM,
    PROCESSING_THRU,
    CASE
        WHEN BUSINESS_THRU = '9999-12-01 23:59:00' AND PROCESSING_THRU = '9999-12-01 23:59:00' THEN 'CURRENT'
        WHEN BUSINESS_THRU = '9999-12-01 23:59:00' AND PROCESSING_THRU < '9999-12-01 23:59:00' THEN 'HISTORICAL_VERSION'
        WHEN BUSINESS_THRU < '9999-12-01 23:59:00' AND PROCESSING_THRU = '9999-12-01 23:59:00' THEN 'PAST_BUSINESS_CURRENT_PROCESSING'
        ELSE 'HISTORICAL_ALL'
    END AS STATUS
FROM EMPLOYEE_ASSIGNMENTS
ORDER BY EMPLOYEE_ID, BUSINESS_FROM, PROCESSING_FROM;

-- =============================================================================
-- 6. SALARIES (Bitemporal - BUSINESS + PROCESSING)
-- =============================================================================
SELECT
    ID,
    EMPLOYEE_ID,
    AMOUNT,
    CURRENCY,
    UPDATED_BY,
    BUSINESS_FROM,
    BUSINESS_THRU,
    PROCESSING_FROM,
    PROCESSING_THRU,
    CASE
        WHEN BUSINESS_THRU = '9999-12-01 23:59:00' AND PROCESSING_THRU = '9999-12-01 23:59:00' THEN 'CURRENT'
        WHEN BUSINESS_THRU = '9999-12-01 23:59:00' AND PROCESSING_THRU < '9999-12-01 23:59:00' THEN 'HISTORICAL_VERSION'
        WHEN BUSINESS_THRU < '9999-12-01 23:59:00' AND PROCESSING_THRU = '9999-12-01 23:59:00' THEN 'PAST_BUSINESS_CURRENT_PROCESSING'
        ELSE 'HISTORICAL_ALL'
    END AS STATUS
FROM SALARIES
ORDER BY EMPLOYEE_ID, BUSINESS_FROM, PROCESSING_FROM;

-- =============================================================================
-- 7. DETAILED VIEW: Employee with Current Assignment and Salary
-- =============================================================================
SELECT
    e.EMPLOYEE_NUMBER,
    e.NAME AS EMPLOYEE_NAME,
    e.EMAIL,
    e.HIRE_DATE,
    d.NAME AS DEPARTMENT,
    p.NAME AS POSITION,
    p.LEVEL AS POSITION_LEVEL,
    s.AMOUNT AS SALARY,
    s.CURRENCY,
    ea.BUSINESS_FROM AS ASSIGNMENT_FROM,
    ea.BUSINESS_THRU AS ASSIGNMENT_THRU,
    s.BUSINESS_FROM AS SALARY_FROM,
    s.BUSINESS_THRU AS SALARY_THRU
FROM EMPLOYEES e
LEFT JOIN EMPLOYEE_ASSIGNMENTS ea ON e.ID = ea.EMPLOYEE_ID
    AND ea.PROCESSING_THRU = '9999-12-01 23:59:00'
    AND ea.BUSINESS_THRU = '9999-12-01 23:59:00'
LEFT JOIN DEPARTMENTS d ON ea.DEPARTMENT_ID = d.ID
    AND d.PROCESSING_THRU = '9999-12-01 23:59:00'
LEFT JOIN POSITIONS p ON ea.POSITION_ID = p.ID
    AND p.PROCESSING_THRU = '9999-12-01 23:59:00'
LEFT JOIN SALARIES s ON e.ID = s.EMPLOYEE_ID
    AND s.PROCESSING_THRU = '9999-12-01 23:59:00'
    AND s.BUSINESS_THRU = '9999-12-01 23:59:00'
WHERE e.PROCESSING_THRU = '9999-12-01 23:59:00'
ORDER BY e.EMPLOYEE_NUMBER;

-- =============================================================================
-- 8. TIMELINE VIEW: All Assignment History for an Employee
-- =============================================================================
-- Replace ? with actual employee ID
SELECT
    ea.ID,
    e.NAME AS EMPLOYEE_NAME,
    d.NAME AS DEPARTMENT,
    p.NAME AS POSITION,
    ea.UPDATED_BY,
    ea.BUSINESS_FROM,
    ea.BUSINESS_THRU,
    ea.PROCESSING_FROM,
    ea.PROCESSING_THRU,
    CASE
        WHEN ea.BUSINESS_THRU = '9999-12-01 23:59:00' AND ea.PROCESSING_THRU = '9999-12-01 23:59:00' THEN 'CURRENT'
        WHEN ea.BUSINESS_THRU = '9999-12-01 23:59:00' AND ea.PROCESSING_THRU < '9999-12-01 23:59:00' THEN 'HISTORICAL_VERSION'
        WHEN ea.BUSINESS_THRU < '9999-12-01 23:59:00' AND ea.PROCESSING_THRU = '9999-12-01 23:59:00' THEN 'PAST_BUSINESS'
        ELSE 'FULLY_HISTORICAL'
    END AS STATUS
FROM EMPLOYEE_ASSIGNMENTS ea
JOIN EMPLOYEES e ON ea.EMPLOYEE_ID = e.ID AND e.PROCESSING_THRU = '9999-12-01 23:59:00'
LEFT JOIN DEPARTMENTS d ON ea.DEPARTMENT_ID = d.ID AND d.PROCESSING_THRU = '9999-12-01 23:59:00'
LEFT JOIN POSITIONS p ON ea.POSITION_ID = p.ID AND p.PROCESSING_THRU = '9999-12-01 23:59:00'
-- WHERE ea.EMPLOYEE_ID = ?
ORDER BY ea.BUSINESS_FROM, ea.PROCESSING_FROM;

-- =============================================================================
-- 9. TIMELINE VIEW: All Salary History for an Employee
-- =============================================================================
-- Replace ? with actual employee ID
SELECT
    s.ID,
    e.NAME AS EMPLOYEE_NAME,
    s.AMOUNT,
    s.CURRENCY,
    s.UPDATED_BY,
    s.BUSINESS_FROM,
    s.BUSINESS_THRU,
    s.PROCESSING_FROM,
    s.PROCESSING_THRU,
    CASE
        WHEN s.BUSINESS_THRU = '9999-12-01 23:59:00' AND s.PROCESSING_THRU = '9999-12-01 23:59:00' THEN 'CURRENT'
        WHEN s.BUSINESS_THRU = '9999-12-01 23:59:00' AND s.PROCESSING_THRU < '9999-12-01 23:59:00' THEN 'HISTORICAL_VERSION'
        WHEN s.BUSINESS_THRU < '9999-12-01 23:59:00' AND s.PROCESSING_THRU = '9999-12-01 23:59:00' THEN 'PAST_BUSINESS'
        ELSE 'FULLY_HISTORICAL'
    END AS STATUS
FROM SALARIES s
JOIN EMPLOYEES e ON s.EMPLOYEE_ID = e.ID AND e.PROCESSING_THRU = '9999-12-01 23:59:00'
-- WHERE s.EMPLOYEE_ID = ?
ORDER BY s.BUSINESS_FROM, s.PROCESSING_FROM;

-- =============================================================================
-- 10. VALIDATION: Check for Gaps or Overlaps in Bitemporal Data
-- =============================================================================
-- This query checks if there are any gaps in the business timeline
-- For a given employee, business periods should be continuous
SELECT
    ea1.EMPLOYEE_ID,
    ea1.BUSINESS_THRU AS FIRST_THRU,
    ea2.BUSINESS_FROM AS NEXT_FROM,
    CASE
        WHEN ea1.BUSINESS_THRU < ea2.BUSINESS_FROM THEN 'GAP'
        WHEN ea1.BUSINESS_THRU > ea2.BUSINESS_FROM THEN 'OVERLAP'
        ELSE 'OK'
    END AS STATUS
FROM EMPLOYEE_ASSIGNMENTS ea1
JOIN EMPLOYEE_ASSIGNMENTS ea2 ON ea1.EMPLOYEE_ID = ea2.EMPLOYEE_ID
WHERE ea1.PROCESSING_THRU = '9999-12-01 23:59:00'
  AND ea2.PROCESSING_THRU = '9999-12-01 23:59:00'
  AND ea1.BUSINESS_THRU != '9999-12-01 23:59:00'
  AND ea2.BUSINESS_FROM > ea1.BUSINESS_FROM
  AND NOT EXISTS (
      SELECT 1 FROM EMPLOYEE_ASSIGNMENTS ea3
      WHERE ea3.EMPLOYEE_ID = ea1.EMPLOYEE_ID
        AND ea3.PROCESSING_THRU = '9999-12-01 23:59:00'
        AND ea3.BUSINESS_FROM > ea1.BUSINESS_FROM
        AND ea3.BUSINESS_FROM < ea2.BUSINESS_FROM
  )
ORDER BY ea1.EMPLOYEE_ID, ea1.BUSINESS_THRU;

-- =============================================================================
-- 11. COUNT SUMMARY
-- =============================================================================
SELECT
    'Companies' AS TABLE_NAME,
    COUNT(*) AS TOTAL_ROWS
FROM COMPANIES
UNION ALL
SELECT
    'Departments (Current)' AS TABLE_NAME,
    COUNT(*) AS TOTAL_ROWS
FROM DEPARTMENTS
WHERE PROCESSING_THRU = '9999-12-01 23:59:00'
UNION ALL
SELECT
    'Positions (Current)' AS TABLE_NAME,
    COUNT(*) AS TOTAL_ROWS
FROM POSITIONS
WHERE PROCESSING_THRU = '9999-12-01 23:59:00'
UNION ALL
SELECT
    'Employees (Current)' AS TABLE_NAME,
    COUNT(*) AS TOTAL_ROWS
FROM EMPLOYEES
WHERE PROCESSING_THRU = '9999-12-01 23:59:00'
UNION ALL
SELECT
    'Employee Assignments (All)' AS TABLE_NAME,
    COUNT(*) AS TOTAL_ROWS
FROM EMPLOYEE_ASSIGNMENTS
UNION ALL
SELECT
    'Employee Assignments (Current Business & Processing)' AS TABLE_NAME,
    COUNT(*) AS TOTAL_ROWS
FROM EMPLOYEE_ASSIGNMENTS
WHERE BUSINESS_THRU = '9999-12-01 23:59:00'
  AND PROCESSING_THRU = '9999-12-01 23:59:00'
UNION ALL
SELECT
    'Salaries (All)' AS TABLE_NAME,
    COUNT(*) AS TOTAL_ROWS
FROM SALARIES
UNION ALL
SELECT
    'Salaries (Current Business & Processing)' AS TABLE_NAME,
    COUNT(*) AS TOTAL_ROWS
FROM SALARIES
WHERE BUSINESS_THRU = '9999-12-01 23:59:00'
  AND PROCESSING_THRU = '9999-12-01 23:59:00';
