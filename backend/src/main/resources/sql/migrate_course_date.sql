-- 课程表：从 day_of_week 迁移到 course_date
-- 适用于已有 day_of_week 字段的数据库

USE course_manage;

ALTER TABLE `course` ADD COLUMN `course_date` DATE NULL COMMENT '上课日期' AFTER `class_id`;

-- 若有旧数据，根据班级 start_date 和 day_of_week 推算具体日期（取班级周期内第一个匹配的星期）
UPDATE `course` c
JOIN `class_info` ci ON c.class_id = ci.id
SET c.course_date = DATE_ADD(
    COALESCE(ci.start_date, CURDATE()),
    INTERVAL (c.day_of_week - DAYOFWEEK(COALESCE(ci.start_date, CURDATE())) + 7) % 7 DAY
)
WHERE c.course_date IS NULL AND c.day_of_week IS NOT NULL;

-- 无班级日期的记录，用当前周推算
UPDATE `course` c
SET c.course_date = DATE_ADD(
    CURDATE(),
    INTERVAL (c.day_of_week - DAYOFWEEK(CURDATE()) + 7) % 7 DAY
)
WHERE c.course_date IS NULL AND c.day_of_week IS NOT NULL;

ALTER TABLE `course` DROP COLUMN `day_of_week`;
ALTER TABLE `course` MODIFY `course_date` DATE NOT NULL COMMENT '上课日期';
ALTER TABLE `course` DROP INDEX `idx_class_id`;
ALTER TABLE `course` ADD KEY `idx_class_date` (`class_id`, `course_date`);
