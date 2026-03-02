package com.cm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentNo;
    private String name;
    private String phone;
    private String idCard;
    /** 性别: 1男 2女 */
    private Integer gender;
    /** 状态: 0禁用 1启用 */
    private Integer status;
    private String accessToken;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private String activeClassNames;

    @TableLogic
    private Integer deleted;
}
