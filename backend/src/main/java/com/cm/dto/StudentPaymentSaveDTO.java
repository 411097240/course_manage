package com.cm.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentPaymentSaveDTO {
    private Long studentClassId;
    private BigDecimal amountDue;
    private BigDecimal amountReceived;
}
