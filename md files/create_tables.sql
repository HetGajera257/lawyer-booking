-- SQL Script to create User and Lawyer tables in legal_connect_db database
-- Run this script in MySQL to create the tables

USE legal_connect_db;

-- Create Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    full_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create Lawyers table
CREATE TABLE IF NOT EXISTS lawyers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    full_name VARCHAR(255),
    bar_number VARCHAR(100),
    specialization VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert sample users (password: password123 - should be hashed in production)
-- Note: These are plain text for testing. In production, use BCrypt or similar
INSERT INTO users (username, password, email, full_name) VALUES
('user1', 'password123', 'user1@example.com', 'John Doe'),
('user2', 'password123', 'user2@example.com', 'Jane Smith')
ON DUPLICATE KEY UPDATE username=username;

-- Insert sample lawyers (password: password123 - should be hashed in production)
INSERT INTO lawyers (username, password, email, full_name, bar_number, specialization) VALUES
('lawyer1', 'password123', 'lawyer1@example.com', 'Attorney Smith', 'BAR001', 'Criminal Law'),
('lawyer2', 'password123', 'lawyer2@example.com', 'Attorney Johnson', 'BAR002', 'Family Law')
ON DUPLICATE KEY UPDATE username=username;

-- Verify tables were created
SELECT 'Users table created successfully' AS status;
SELECT COUNT(*) AS user_count FROM users;
SELECT COUNT(*) AS lawyer_count FROM lawyers;

