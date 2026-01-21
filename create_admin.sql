-- SQL Script to create the first admin account
-- Run this script in your MySQL database

-- Create admin with username: admin, password: admin123
-- Note: The password will be hashed on first login
INSERT INTO admins (username, password, email, full_name, created_at, updated_at)
VALUES ('admin', 'admin123', 'admin@legalconnect.com', 'System Administrator', NOW(), NOW());

-- Alternative: If you want a different username/password, modify the values below
-- INSERT INTO admins (username, password, email, full_name, created_at, updated_at)
-- VALUES ('yourusername', 'yourpassword', 'youremail@example.com', 'Your Full Name', NOW(), NOW());
