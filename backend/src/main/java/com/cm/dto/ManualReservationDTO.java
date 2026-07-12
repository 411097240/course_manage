package com.cm.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ManualReservationDTO {
    private Long id;
    private Long classroomId;
    private LocalDate reserveDate;
    private String startTime;
    private String endTime;
    private String title;
}
