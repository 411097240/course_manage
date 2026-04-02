package com.cm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("attendance_detail")
public class AttendanceDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private Long studentId;
    private Integer status; // 1:出勤, 2:迟到, 3:请假, 4:缺勤
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
