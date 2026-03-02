package com.cm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_homework")
public class StudentHomework {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long homeworkId;
    private Long studentId;
    private String submitAttachments;
    private String teacherFeedbackAttachments;
    private String teacherComment;
    /** 0:待提交, 1:已提交, 2:待修正, 3:已通过 */
    private Integer status;
    private LocalDateTime submitTime;
    private LocalDateTime reviewTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
