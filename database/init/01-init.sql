-- database/init/01-init.sql
CREATE DATABASE IF NOT EXISTS shelfsense;
USE shelfsense;

-- Grant permissions to root user
GRANT ALL PRIVILEGES ON shelfsense.* TO 'root'@'%';
FLUSH PRIVILEGES;

-- Create a simple test table
CREATE TABLE IF NOT EXISTS connection_test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    message VARCHAR(255) DEFAULT 'Database connection successful'
);

INSERT INTO connection_test (message) VALUES ('Database initialized successfully');