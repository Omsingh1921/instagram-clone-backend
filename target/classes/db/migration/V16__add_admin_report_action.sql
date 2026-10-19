-- Migration V16: Add moderation action to reports table
ALTER TABLE reports
ADD COLUMN action VARCHAR(30) NULL DEFAULT 'NO_ACTION';
