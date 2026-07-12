package com.cm.dto;

import lombok.Data;
import java.util.List;

@Data
public class CourseBatchDTO {
    private Long classId;
    private List<String> courseDates;
    private String teacherName;
    private String startTime;
    private String endTime;
    private Long classroomId;
    private String location;
}
