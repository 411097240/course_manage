package com.cm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("attendance_record")
public class AttendanceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classId;
    private Long teacherId;
    private LocalDate recordDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
