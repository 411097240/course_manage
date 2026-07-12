-- 手动教室预约迁移脚本
USE course_manage;

ALTER TABLE `classroom_reservation`
  MODIFY `course_id` BIGINT NULL COMMENT '课程ID(手动预约为空)',
  ADD COLUMN `title` VARCHAR(100) NULL COMMENT '手动预约事由' AFTER `course_id`;
