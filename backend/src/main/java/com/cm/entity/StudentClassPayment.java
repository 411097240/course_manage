package com.cm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_class_payment")
public class StudentClassPayment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentClassId;
    private BigDecimal amountDue;
    private BigDecimal amountReceived;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
