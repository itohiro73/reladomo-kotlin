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

-- Create Products table (Non-temporal for MVP)
CREATE TABLE PRODUCTS (
    ID BIGINT NOT NULL PRIMARY KEY,
    CATEGORY_ID BIGINT NOT NULL,
    NAME VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(1000),
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

-- Insert sample products (non-temporal)
INSERT INTO PRODUCTS (ID, CATEGORY_ID, NAME, DESCRIPTION) VALUES
-- Regular product
(1, 1, 'Laptop Pro 15', 'High-performance laptop'),
-- Seasonal product (summer 2025)
(2, 3, 'Summer T-Shirt', 'Limited edition summer collection'),
-- Annual reference book
(3, 2, 'Programming Guide 2025', 'Annual programming reference');

-- Insert sample product prices (bitemporal)
-- Scenario: Price changes for Laptop Pro 15 with proper bitemporal chaining
-- Key principle: 2D plane should have NO gaps and NO overlaps when properly chained

-- Step 1 (2025/07/01): pricing-team registers "1000 from Jul 1"
-- ID 1: Original registration - "From Jul 1 to Oct 1, we thought Jul 1 onwards would be 1000"
-- When Step 2 happens, BUSINESS_THRU changes to 2025-11-01, PROCESSING_THRU changes to 2025-10-01
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(1, 1, 1000.00, 'pricing-team', '2025-07-01 00:00:00', '9999-12-01 00:00:00', '2025-07-01 00:00:00', '2025-10-01 00:00:00');

-- Step 2 (2025/10/01): Alice registers "1200 from Nov 1"
-- This splits pricing-team's record: BUSINESS_THRU becomes 2025-11-01
-- ID 2: Continuation of pricing-team - "From Oct 1 onwards, we know Jul 1 to Nov 1 was 1000"
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(2, 1, 1000.00, 'pricing-team', '2025-07-01 00:00:00', '2025-11-01 00:00:00', '2025-10-01 00:00:00', '9999-12-01 00:00:00');

-- ID 3: Alice's 1200 - "From Oct 1 to Oct 15, we thought Nov 1 onwards would be 1200"
-- BUSINESS_THRU stays 9999! Bob's correction only changes PROCESSING_THRU
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(3, 1, 1200.00, 'alice@example.com', '2025-11-01 00:00:00', '9999-12-01 00:00:00', '2025-10-01 00:00:00', '2025-10-15 00:00:00');

-- Step 3 (2025/10/15): Bob corrects to "1100 from Nov 1"
-- Alice's PROCESSING_THRU becomes 2025-10-15, but BUSINESS_THRU stays 9999
-- ID 4: Bob's 1100 - "From Oct 15 to Oct 20, we thought Nov 1 onwards would be 1100"
-- When Step 4 happens, BUSINESS_THRU changes to 2025-12-01
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(4, 1, 1100.00, 'bob@example.com', '2025-11-01 00:00:00', '9999-12-01 00:00:00', '2025-10-15 00:00:00', '2025-10-20 00:00:00');

-- Step 4 (2025/10/20): Charlie registers "1250 from Dec 1"
-- This splits Bob's record: BUSINESS_THRU becomes 2025-12-01
-- ID 5: Continuation of Bob - "From Oct 20 onwards, we know Nov 1 to Dec 1 is 1100"
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(5, 1, 1100.00, 'bob@example.com', '2025-11-01 00:00:00', '2025-12-01 00:00:00', '2025-10-20 00:00:00', '9999-12-01 00:00:00');

-- ID 6: Charlie's 1250 - "From Oct 20 onwards, we know Dec 1 onwards is 1250"
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(6, 1, 1250.00, 'charlie@example.com', '2025-12-01 00:00:00', '9999-12-01 00:00:00', '2025-10-20 00:00:00', '9999-12-01 00:00:00');

-- Summer T-Shirt price by marketing team
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(7, 2, 29.99, 'marketing-team', '2025-06-01 00:00:00', '9999-12-01 00:00:00', '2025-06-01 00:00:00', '9999-12-01 00:00:00');

-- Programming Guide price by content team
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(8, 3, 49.99, 'content-team', '2025-07-01 00:00:00', '9999-12-01 00:00:00', '2025-07-01 00:00:00', '9999-12-01 00:00:00');
