-- Migration: Add created_method column to question table
-- Run this manually against the database before starting the app

ALTER TABLE `question`
ADD COLUMN `created_method` VARCHAR(20) DEFAULT 'MANUAL' AFTER `source`;

-- Set default for existing rows
UPDATE `question` SET `created_method` = 'MANUAL' WHERE `created_method` IS NULL;
