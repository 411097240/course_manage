package com.cm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_class")
public class StudentClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long classId;
    /** 状态: 0已退出 1在读 */
    private Integer status;
    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;

    @TableLogic
    private Integer deleted;
}
