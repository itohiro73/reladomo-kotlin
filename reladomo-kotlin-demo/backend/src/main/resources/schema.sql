-- Create sequence table for Reladomo
CREATE TABLE IF NOT EXISTS MITHRA_SEQUENCE (
    SEQUENCE_NAME VARCHAR(64) NOT NULL PRIMARY KEY,
    NEXT_ID BIGINT NOT NULL
);

-- Drop existing tables
DROP TABLE IF EXISTS PRODUCT_PRICES;
DROP TABLE IF EXISTS PRODUCTS;
DROP TABLE IF EXISTS CATEGORIES;

-- Create Categories table (Non-temporal)
CREATE TABLE CATEGORIES (
    ID BIGINT NOT NULL PRIMARY KEY,
    NAME VARCHAR(100) NOT NULL,
    DESCRIPTION VARCHAR(500)
);

-- Create Products table (Unitemporal - tracks master data change history)
CREATE TABLE PRODUCTS (
    ID BIGINT NOT NULL,
    CATEGORY_ID BIGINT NOT NULL,
    NAME VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(1000),

    -- Unitemporal columns (processing time only)
    PROCESSING_FROM TIMESTAMP NOT NULL,
    PROCESSING_THRU TIMESTAMP NOT NULL,

    PRIMARY KEY (ID, PROCESSING_FROM),
    FOREIGN KEY (CATEGORY_ID) REFERENCES CATEGORIES(ID)
);

-- Create Product Prices table (Bitemporal with audit trail)
CREATE TABLE PRODUCT_PRICES (
    ID BIGINT NOT NULL,
    PRODUCT_ID BIGINT NOT NULL,
    PRICE DECIMAL(19, 2) NOT NULL,

    -- Audit trail: who made the change
    UPDATED_BY VARCHAR(100),

    -- Bitemporal columns
    BUSINESS_FROM TIMESTAMP NOT NULL,
    BUSINESS_THRU TIMESTAMP NOT NULL,
    PROCESSING_FROM TIMESTAMP NOT NULL,
    PROCESSING_THRU TIMESTAMP NOT NULL,

    PRIMARY KEY (ID, BUSINESS_FROM, PROCESSING_FROM),
    -- Bitemporal unique constraint: no two records can have the same product, business end, and processing end
    CONSTRAINT UK_PRODUCT_PRICES_BITEMPORAL UNIQUE (PRODUCT_ID, BUSINESS_THRU, PROCESSING_THRU)
);

-- Insert sample categories (non-temporal)
INSERT INTO CATEGORIES (ID, NAME, DESCRIPTION) VALUES
(1, 'Electronics', 'Electronic devices and accessories'),
(2, 'Books', 'Physical and digital books'),
(3, 'Clothing', 'Apparel and fashion items');

-- Insert sample products (unitemporal - master data change history)
-- TIMEZONE POLICY: All timestamps are stored in UTC
-- Scenario: Track product information changes over time

-- Product ID 1: Laptop Pro 15 - demonstrating rebranding history
-- Version 1 (2025/06/30 15:00 UTC = 2025/07/01 00:00 JST): Initial registration
INSERT INTO PRODUCTS (ID, CATEGORY_ID, NAME, DESCRIPTION, PROCESSING_FROM, PROCESSING_THRU) VALUES
(1, 1, 'Laptop Pro 15', 'High-performance laptop', '2025-06-30 15:00:00', '2025-07-31 15:00:00');

-- Version 2 (2025/07/31 15:00 UTC = 2025/08/01 00:00 JST): Rebranding to Gen2
INSERT INTO PRODUCTS (ID, CATEGORY_ID, NAME, DESCRIPTION, PROCESSING_FROM, PROCESSING_THRU) VALUES
(1, 1, 'Laptop Pro 15 Gen2', 'High-performance laptop - 2nd generation', '2025-07-31 15:00:00', '2025-08-31 15:00:00');

-- Version 3 (2025/08/31 15:00 UTC = 2025/09/01 00:00 JST): Description update
INSERT INTO PRODUCTS (ID, CATEGORY_ID, NAME, DESCRIPTION, PROCESSING_FROM, PROCESSING_THRU) VALUES
(1, 1, 'Laptop Pro 15 Gen2', 'High-performance laptop with latest Intel processor', '2025-08-31 15:00:00', '9999-12-01 23:59:00');

-- Product ID 2: Summer T-Shirt - single version (no changes)
INSERT INTO PRODUCTS (ID, CATEGORY_ID, NAME, DESCRIPTION, PROCESSING_FROM, PROCESSING_THRU) VALUES
(2, 3, 'Summer T-Shirt', 'Limited edition summer collection', '2025-05-31 15:00:00', '9999-12-01 23:59:00');

-- Product ID 3: Programming Guide 2025 - single version (annual publication)
INSERT INTO PRODUCTS (ID, CATEGORY_ID, NAME, DESCRIPTION, PROCESSING_FROM, PROCESSING_THRU) VALUES
(3, 2, 'Programming Guide 2025', 'Annual programming reference', '2025-06-30 15:00:00', '9999-12-01 23:59:00');

-- Insert sample product prices (bitemporal)
-- TIMEZONE POLICY: All timestamps are stored in UTC
-- JVM timezone is set to UTC (-Duser.timezone=UTC) for consistent TIMESTAMP interpretation
-- Scenario: Price changes for Laptop Pro 15 with proper bitemporal chaining
-- Key principle: 2D plane should have NO gaps and NO overlaps when properly chained

-- Step 1 (2025/06/30 15:00 UTC = 2025/07/01 00:00 JST): pricing-team registers "1000 from Jul 1 JST"
-- ID 1: Original registration - "From Jul 1 JST to Oct 1 JST, we thought Jul 1 JST onwards would be 1000"
-- When Step 2 happens, BUSINESS_THRU changes to 2025-10-31 15:00:00, PROCESSING_THRU changes to 2025-09-30 15:00:00
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(1, 1, 1000.00, 'pricing-team', '2025-06-30 15:00:00', '9999-12-01 23:59:00', '2025-06-30 15:00:00', '2025-09-30 15:00:00');

-- Step 2 (2025/09/30 15:00 UTC = 2025/10/01 00:00 JST): Alice registers "1200 from Nov 1 JST"
-- This splits pricing-team's record: BUSINESS_THRU becomes 2025-10-31 15:00:00
-- ID 2: Continuation of pricing-team - "From Oct 1 JST onwards, we know Jul 1 JST to Nov 1 JST was 1000"
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(2, 1, 1000.00, 'pricing-team', '2025-06-30 15:00:00', '2025-10-31 15:00:00', '2025-09-30 15:00:00', '9999-12-01 23:59:00');

-- ID 3: Alice's 1200 - "From Oct 1 JST to Oct 15 JST, we thought Nov 1 JST onwards would be 1200"
-- BUSINESS_THRU stays 9999! Bob's correction only changes PROCESSING_THRU
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(3, 1, 1200.00, 'alice@example.com', '2025-10-31 15:00:00', '9999-12-01 23:59:00', '2025-09-30 15:00:00', '2025-10-14 15:00:00');

-- Step 3 (2025/10/14 15:00 UTC = 2025/10/15 00:00 JST): Bob corrects to "1100 from Nov 1 JST"
-- Alice's PROCESSING_THRU becomes 2025-10-14 15:00:00, but BUSINESS_THRU stays 9999
-- ID 4: Bob's 1100 - "From Oct 15 JST to Oct 20 JST, we thought Nov 1 JST onwards would be 1100"
-- When Step 4 happens, BUSINESS_THRU changes to 2025-11-30 15:00:00
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(4, 1, 1100.00, 'bob@example.com', '2025-10-31 15:00:00', '9999-12-01 23:59:00', '2025-10-14 15:00:00', '2025-10-19 15:00:00');

-- Step 4 (2025/10/19 15:00 UTC = 2025/10/20 00:00 JST): Charlie registers "1250 from Dec 1 JST"
-- This splits Bob's record: BUSINESS_THRU becomes 2025-11-30 15:00:00
-- ID 5: Continuation of Bob - "From Oct 20 JST onwards, we know Nov 1 JST to Dec 1 JST is 1100"
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(5, 1, 1100.00, 'bob@example.com', '2025-10-31 15:00:00', '2025-11-30 15:00:00', '2025-10-19 15:00:00', '9999-12-01 23:59:00');

-- ID 6: Charlie's 1250 - "From Oct 20 JST onwards, we know Dec 1 JST onwards is 1250"
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(6, 1, 1250.00, 'charlie@example.com', '2025-11-30 15:00:00', '9999-12-01 23:59:00', '2025-10-19 15:00:00', '9999-12-01 23:59:00');

-- Summer T-Shirt price by marketing team (Jun 1 JST = May 31 15:00 UTC)
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(7, 2, 29.99, 'marketing-team', '2025-05-31 15:00:00', '9999-12-01 23:59:00', '2025-05-31 15:00:00', '9999-12-01 23:59:00');

-- Programming Guide price by content team (Jul 1 JST = Jun 30 15:00 UTC)
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(8, 3, 49.99, 'content-team', '2025-06-30 15:00:00', '9999-12-01 23:59:00', '2025-06-30 15:00:00', '9999-12-01 23:59:00');
