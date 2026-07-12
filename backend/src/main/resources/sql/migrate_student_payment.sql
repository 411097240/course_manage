-- 学生班级缴费信息迁移脚本
USE course_manage;

CREATE TABLE IF NOT EXISTS `student_class_payment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `student_class_id` BIGINT NOT NULL COMMENT '学生班级关联ID',
    `amount_due` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '应缴金额',
    `amount_received` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实收金额',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_class_id` (`student_class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生班级缴费表';
