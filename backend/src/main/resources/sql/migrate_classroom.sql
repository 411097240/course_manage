-- 教室管理与预约迁移脚本
USE course_manage;

-- 教室表
CREATE TABLE IF NOT EXISTS `classroom` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '教室名称',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室表';

-- 教室预约表
CREATE TABLE IF NOT EXISTS `classroom_reservation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `classroom_id` BIGINT NOT NULL COMMENT '教室ID',
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `reserve_date` DATE NOT NULL COMMENT '预约日期',
    `start_time` VARCHAR(10) NOT NULL COMMENT '开始时间(HH:mm)',
    `end_time` VARCHAR(10) NOT NULL COMMENT '结束时间(HH:mm)',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_id` (`course_id`),
    KEY `idx_classroom_date` (`classroom_id`, `reserve_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室预约表';

-- 课程表增加教室ID
ALTER TABLE `course` ADD COLUMN `classroom_id` BIGINT NULL COMMENT '教室ID' AFTER `end_time`;
