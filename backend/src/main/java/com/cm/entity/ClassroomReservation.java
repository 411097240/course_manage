package com.cm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("classroom_reservation")
public class ClassroomReservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classroomId;
    private Long courseId;
    private String title;
    private LocalDate reserveDate;
    private String startTime;
    private String endTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
