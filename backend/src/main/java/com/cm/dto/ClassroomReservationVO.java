package com.cm.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ClassroomReservationVO {
    private Long id;
    private Long classroomId;
    private Long courseId;
    private LocalDate reserveDate;
    private String startTime;
    private String endTime;
    private String title;
    private String className;
    private String teacherName;
    private Boolean manual;
}
