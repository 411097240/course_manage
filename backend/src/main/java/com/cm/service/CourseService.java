package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cm.dto.CourseBatchDTO;
import com.cm.entity.ClassInfo;
import com.cm.entity.Course;
import com.cm.mapper.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private ClassInfoService classInfoService;

    public List<Course> listByClassId(Long classId) {
        return courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getClassId, classId)
                        .orderByAsc(Course::getCourseDate)
                        .orderByAsc(Course::getStartTime));
    }

    public List<Course> listByClassIdsAndDate(List<Long> classIds, LocalDate date) {
        if (classIds == null || classIds.isEmpty()) {
            return Collections.emptyList();
        }
        return courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .in(Course::getClassId, classIds)
                        .eq(Course::getCourseDate, date)
                        .orderByAsc(Course::getStartTime));
    }

    public Map<Long, Course> mapByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return courseMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
    }

    public void save(Course course) {
        validateCourseDate(course.getClassId(), course.getCourseDate());
        courseMapper.insert(course);
    }

    public void saveBatch(CourseBatchDTO dto) {
        if (dto.getCourseDates() == null || dto.getCourseDates().isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个上课日期");
        }
        for (String dateStr : dto.getCourseDates()) {
            LocalDate date = LocalDate.parse(dateStr);
            validateCourseDate(dto.getClassId(), date);
            Course course = new Course();
            course.setClassId(dto.getClassId());
            course.setCourseDate(date);
            course.setTeacherName(dto.getTeacherName());
            course.setStartTime(dto.getStartTime());
            course.setEndTime(dto.getEndTime());
            course.setLocation(dto.getLocation());
            courseMapper.insert(course);
        }
    }

    public void update(Course course) {
        validateCourseDate(course.getClassId(), course.getCourseDate());
        courseMapper.updateById(course);
    }

    public void deleteById(Long id) {
        courseMapper.deleteById(id);
    }

    private void validateCourseDate(Long classId, LocalDate courseDate) {
        if (classId == null || courseDate == null) {
            throw new IllegalArgumentException("班级和上课日期不能为空");
        }
        ClassInfo classInfo = classInfoService.getById(classId);
        if (classInfo == null) {
            throw new IllegalArgumentException("班级不存在");
        }
        if (classInfo.getStartDate() != null && courseDate.isBefore(classInfo.getStartDate())) {
            throw new IllegalArgumentException("上课日期不能早于班级开始日期");
        }
        if (classInfo.getEndDate() != null && courseDate.isAfter(classInfo.getEndDate())) {
            throw new IllegalArgumentException("上课日期不能晚于班级结束日期");
        }
    }
}
