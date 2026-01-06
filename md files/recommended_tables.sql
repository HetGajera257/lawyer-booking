-- =====================================================
-- RECOMMENDED ADDITIONAL TABLES FOR LAWYER BOOKING SYSTEM
-- =====================================================
-- This script adds essential tables to enhance the system functionality

USE legal_connect_db;

-- =====================================================
-- 1. APPOINTMENTS/BOOKINGS TABLE
-- =====================================================
-- Tracks appointments between users and lawyers
CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    lawyer_id BIGINT NOT NULL,
    appointment_date DATETIME NOT NULL,
    duration_minutes INT DEFAULT 60,
    status ENUM('pending', 'confirmed', 'completed', 'cancelled') DEFAULT 'pending',
    meeting_type ENUM('in-person', 'video', 'phone', 'audio') DEFAULT 'audio',
    description TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_appointment_date (appointment_date),
    INDEX idx_status (status)
);

-- =====================================================
-- 2. CASES/MATTERS TABLE
-- =====================================================
-- Organizes audio records and appointments by case
CREATE TABLE IF NOT EXISTS cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    lawyer_id BIGINT,
    case_title VARCHAR(255) NOT NULL,
    case_type VARCHAR(100),
    case_status ENUM('open', 'in-progress', 'closed', 'on-hold') DEFAULT 'open',
    description TEXT,
    priority ENUM('low', 'medium', 'high', 'urgent') DEFAULT 'medium',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_case_status (case_status)
);

-- =====================================================
-- 3. UPDATE CLIENT_AUDIO TABLE
-- =====================================================
-- Add foreign keys to link audio records to users and cases
ALTER TABLE client_audio 
ADD COLUMN IF NOT EXISTS user_id BIGINT,
ADD COLUMN IF NOT EXISTS case_id BIGINT,
ADD COLUMN IF NOT EXISTS lawyer_id BIGINT,
ADD COLUMN IF NOT EXISTS file_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS file_size BIGINT,
ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS status ENUM('processing', 'completed', 'failed') DEFAULT 'processing',
ADD FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
ADD FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE SET NULL,
ADD FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE SET NULL,
ADD INDEX idx_user_id (user_id),
ADD INDEX idx_case_id (case_id),
ADD INDEX idx_lawyer_id (lawyer_id);

-- =====================================================
-- 4. MESSAGES TABLE
-- =====================================================
-- Enables communication between users and lawyers
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT,
    sender_id BIGINT NOT NULL,
    sender_type ENUM('user', 'lawyer') NOT NULL,
    receiver_id BIGINT NOT NULL,
    receiver_type ENUM('user', 'lawyer') NOT NULL,
    subject VARCHAR(255),
    message_text TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE SET NULL,
    INDEX idx_case_id (case_id),
    INDEX idx_sender (sender_id, sender_type),
    INDEX idx_receiver (receiver_id, receiver_type),
    INDEX idx_created_at (created_at)
);

-- =====================================================
-- 5. LAWYER_AVAILABILITY TABLE
-- =====================================================
-- Tracks lawyer's available time slots
CREATE TABLE IF NOT EXISTS lawyer_availability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lawyer_id BIGINT NOT NULL,
    day_of_week TINYINT NOT NULL COMMENT '0=Sunday, 1=Monday, ..., 6=Saturday',
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE CASCADE,
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_day_of_week (day_of_week)
);

-- =====================================================
-- 6. LAWYER_BLOCKED_DATES TABLE
-- =====================================================
-- Tracks specific dates when lawyer is unavailable
CREATE TABLE IF NOT EXISTS lawyer_blocked_dates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lawyer_id BIGINT NOT NULL,
    blocked_date DATE NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE CASCADE,
    UNIQUE KEY unique_lawyer_date (lawyer_id, blocked_date),
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_blocked_date (blocked_date)
);

-- =====================================================
-- 7. NOTIFICATIONS TABLE
-- =====================================================
-- System notifications for users and lawyers
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    lawyer_id BIGINT,
    recipient_type ENUM('user', 'lawyer') NOT NULL,
    notification_type VARCHAR(50) NOT NULL COMMENT 'appointment, message, case_update, audio_processed, etc.',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    link_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_recipient_type (recipient_type),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
);

-- =====================================================
-- 8. LAWYER_REVIEWS TABLE
-- =====================================================
-- User reviews and ratings for lawyers
CREATE TABLE IF NOT EXISTS lawyer_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lawyer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    appointment_id BIGINT,
    rating TINYINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    UNIQUE KEY unique_user_lawyer_review (user_id, lawyer_id, appointment_id),
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_rating (rating)
);

-- =====================================================
-- 9. DOCUMENTS TABLE
-- =====================================================
-- Stores documents/files related to cases
CREATE TABLE IF NOT EXISTS documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT,
    user_id BIGINT NOT NULL,
    lawyer_id BIGINT,
    document_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(50),
    file_path VARCHAR(500),
    file_size BIGINT,
    uploaded_by ENUM('user', 'lawyer') NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE SET NULL,
    INDEX idx_case_id (case_id),
    INDEX idx_user_id (user_id),
    INDEX idx_lawyer_id (lawyer_id)
);

-- =====================================================
-- 10. PAYMENTS TABLE
-- =====================================================
-- Tracks payments for appointments and services
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT,
    user_id BIGINT NOT NULL,
    lawyer_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    payment_status ENUM('pending', 'completed', 'failed', 'refunded') DEFAULT 'pending',
    payment_method VARCHAR(50),
    transaction_id VARCHAR(255),
    payment_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE CASCADE,
    INDEX idx_appointment_id (appointment_id),
    INDEX idx_user_id (user_id),
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_payment_status (payment_status)
);

-- =====================================================
-- SAMPLE DATA FOR TESTING
-- =====================================================

-- Sample availability for lawyer1 (Monday-Friday, 9 AM - 5 PM)
INSERT INTO lawyer_availability (lawyer_id, day_of_week, start_time, end_time) VALUES
(1, 1, '09:00:00', '17:00:00'), -- Monday
(1, 2, '09:00:00', '17:00:00'), -- Tuesday
(1, 3, '09:00:00', '17:00:00'), -- Wednesday
(1, 4, '09:00:00', '17:00:00'), -- Thursday
(1, 5, '09:00:00', '17:00:00')  -- Friday
ON DUPLICATE KEY UPDATE start_time=VALUES(start_time);

-- Sample availability for lawyer2
INSERT INTO lawyer_availability (lawyer_id, day_of_week, start_time, end_time) VALUES
(2, 1, '10:00:00', '18:00:00'), -- Monday
(2, 2, '10:00:00', '18:00:00'), -- Tuesday
(2, 3, '10:00:00', '18:00:00'), -- Wednesday
(2, 4, '10:00:00', '18:00:00'), -- Thursday
(2, 5, '10:00:00', '18:00:00')  -- Friday
ON DUPLICATE KEY UPDATE start_time=VALUES(start_time);

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================
SELECT 'All recommended tables created successfully!' AS status;
SELECT TABLE_NAME FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'legal_connect_db' 
ORDER BY TABLE_NAME;

