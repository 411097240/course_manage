-- 点名记录关联排课，支持同一班级当天多节课分别点名
USE course_manage;

ALTER TABLE `attendance_record` ADD COLUMN `course_id` BIGINT NULL COMMENT '排课ID' AFTER `class_id`;
ALTER TABLE `attendance_record` DROP INDEX `uk_class_date`;
ALTER TABLE `attendance_record` ADD UNIQUE KEY `uk_course_id` (`course_id`);
