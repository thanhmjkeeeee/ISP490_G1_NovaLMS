-- Migration: Add created_method column to question table
-- Run this manually against the database before starting the app

ALTER TABLE `question`
ADD COLUMN `created_method` VARCHAR(20) DEFAULT 'MANUAL' AFTER `source`;

-- Set default for existing rows
UPDATE `question` SET `created_method` = 'MANUAL' WHERE `created_method` IS NULL;

-- Migration: Add description column to class table
ALTER TABLE `class`
ADD COLUMN `description` TEXT AFTER `meet_link`;

-- Migration: Add number_of_sessions to course table
ALTER TABLE `course`
ADD COLUMN `number_of_sessions` INT DEFAULT NULL AFTER `sale`;
