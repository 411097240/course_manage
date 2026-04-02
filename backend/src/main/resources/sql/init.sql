-- 教学管理系统数据库初始化脚本
CREATE DATABASE IF NOT EXISTS course_manage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE course_manage;

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT '登录名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `role` TINYINT NOT NULL DEFAULT 2 COMMENT '角色: 1管理员 2教师',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 班级表
CREATE TABLE IF NOT EXISTS `class_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `class_code` VARCHAR(20) NOT NULL COMMENT '班级编码(如BJ2026-0001)',
    `class_name` VARCHAR(100) NOT NULL COMMENT '班级名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `start_date` DATE DEFAULT NULL COMMENT '开始日期',
    `end_date` DATE DEFAULT NULL COMMENT '结束日期',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0已结束 1进行中',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_code` (`class_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 用户-班级关联表(教师管理班级)
CREATE TABLE IF NOT EXISTS `user_class` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_class` (`user_id`, `class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-班级关联表';

-- 课程表
CREATE TABLE IF NOT EXISTS `course` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `teacher_name` VARCHAR(50) DEFAULT NULL COMMENT '授课教师',
    `day_of_week` TINYINT NOT NULL COMMENT '星期几(1-7)',
    `start_time` VARCHAR(10) NOT NULL COMMENT '开始时间(HH:mm)',
    `end_time` VARCHAR(10) NOT NULL COMMENT '结束时间(HH:mm)',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '上课地点',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程排课表';

-- 学生表
CREATE TABLE IF NOT EXISTS `student` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `student_no` VARCHAR(20) NOT NULL COMMENT '学号(如S202600001)',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `id_card` VARCHAR(20) DEFAULT NULL COMMENT '身份证号',
    `gender` TINYINT DEFAULT NULL COMMENT '性别: 1男 2女',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    `access_token` VARCHAR(64) DEFAULT NULL COMMENT 'H5凭证',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- 学生-班级关联表
CREATE TABLE IF NOT EXISTS `student_class` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0已退出 1在读',
    `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入班时间',
    `leave_time` DATETIME DEFAULT NULL COMMENT '出班时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生-班级关联表';

-- 作业主体表
CREATE TABLE IF NOT EXISTS `homework` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT NOT NULL COMMENT '所属班级ID',
    `title` VARCHAR(255) NOT NULL COMMENT '作业标题',
    `description` TEXT COMMENT '作业描述',
    `attachments` TEXT COMMENT '附件JSON',
    `answer_attachments` TEXT COMMENT '答案附件JSON',
    `is_answer_published` INT DEFAULT 0 COMMENT '答案是否公布 0未公布 1已公布',
    `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '发布人',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业主体表';

-- 学生作业提交表
CREATE TABLE IF NOT EXISTS `student_homework` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `homework_id` BIGINT NOT NULL COMMENT '作业ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `submit_attachments` TEXT COMMENT '提交附件JSON',
    `teacher_feedback_attachments` TEXT COMMENT '批改附件JSON',
    `teacher_comment` TEXT COMMENT '教师评语',
    `status` INT DEFAULT 0 COMMENT '0:待提交, 1:已提交, 2:待修正, 3:已通过',
    `submit_time` DATETIME DEFAULT NULL COMMENT '提交时间',
    `review_time` DATETIME DEFAULT NULL COMMENT '批改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生作业提交状态记录表';

-- 插入默认管理员 (密码: admin123, BCrypt加密)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`)
VALUES ('admin', '$2a$10$Hxj0NoyNPjCKJm/87tWCt.uFyuzkyNro/PeiuQOb2PQQYxrN9Pq16', '系统管理员', 1, 1);
