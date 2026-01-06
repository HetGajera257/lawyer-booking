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

-- Create Appointments table
CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    lawyer_id BIGINT NOT NULL,
    appointment_date DATETIME NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 60,
    status ENUM('pending', 'confirmed', 'cancelled', 'completed') NOT NULL DEFAULT 'pending',
    meeting_type ENUM('in-person', 'video', 'phone') DEFAULT 'video',
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

-- Create Lawyer Availability table
CREATE TABLE IF NOT EXISTS lawyer_availability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lawyer_id BIGINT NOT NULL,
    day_of_week INT NOT NULL COMMENT '0=Sunday, 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday',
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE CASCADE,
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_day_of_week (day_of_week)
);

-- Create Client Audio table (if not exists)
CREATE TABLE IF NOT EXISTS client_audio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    lawyer_id BIGINT,
    appointment_id BIGINT,
    language VARCHAR(50),
    original_english_text LONGTEXT,
    masked_english_text LONGTEXT,
    masked_text_audio LONGBLOB,
    masked_gujarati_text LONGTEXT,
    masked_gujarati_audio LONGBLOB
);

-- Add case_id column to client_audio table if it doesn't exist
ALTER TABLE client_audio 
ADD COLUMN IF NOT EXISTS case_id BIGINT;

-- Create Cases table
CREATE TABLE IF NOT EXISTS cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    lawyer_id BIGINT,
    case_title VARCHAR(255) NOT NULL,
    case_type VARCHAR(100),
    case_status VARCHAR(50) DEFAULT 'open',
    description LONGTEXT,
    solution LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_case_status (case_status)
);

-- Create Messages table
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT,
    sender_id BIGINT NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    receiver_id BIGINT NOT NULL,
    receiver_type VARCHAR(20) NOT NULL,
    message_text LONGTEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_case_id (case_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_created_at (created_at)
);

-- Add foreign key for case_id in client_audio if it doesn't exist
-- Note: This may fail if the constraint already exists

