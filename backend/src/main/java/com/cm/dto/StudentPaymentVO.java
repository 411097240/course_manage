package com.cm.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentPaymentVO {
    private Long studentClassId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long classId;
    private String classCode;
    private String className;
    private Integer enrollmentStatus;
    private BigDecimal amountDue;
    private BigDecimal amountReceived;
    private BigDecimal balance;
    /** 缴费状态: 1结余 2结清 3欠费 */
    private Integer paymentStatus;
}
