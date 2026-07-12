package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cm.dto.CourseBatchDTO;
import com.cm.entity.ClassInfo;
import com.cm.entity.Classroom;
import com.cm.entity.Course;
import com.cm.mapper.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private ClassroomReservationService reservationService;

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

    @Transactional
    public void save(Course course) {
        validateCourseDate(course.getClassId(), course.getCourseDate());
        fillClassroomInfo(course);
        reservationService.checkConflict(
                course.getClassroomId(), course.getCourseDate(),
                course.getStartTime(), course.getEndTime(), null);
        courseMapper.insert(course);
        reservationService.upsertForCourse(course);
    }

    @Transactional
    public void saveBatch(CourseBatchDTO dto) {
        if (dto.getCourseDates() == null || dto.getCourseDates().isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个上课日期");
        }
        fillClassroomInfo(dto.getClassroomId(), dto);
        for (String dateStr : dto.getCourseDates()) {
            LocalDate date = LocalDate.parse(dateStr);
            validateCourseDate(dto.getClassId(), date);
            reservationService.checkConflict(
                    dto.getClassroomId(), date, dto.getStartTime(), dto.getEndTime(), null);

            Course course = new Course();
            course.setClassId(dto.getClassId());
            course.setCourseDate(date);
            course.setTeacherName(dto.getTeacherName());
            course.setStartTime(dto.getStartTime());
            course.setEndTime(dto.getEndTime());
            course.setClassroomId(dto.getClassroomId());
            course.setLocation(dto.getLocation());
            courseMapper.insert(course);
            reservationService.upsertForCourse(course);
        }
    }

    @Transactional
    public void update(Course course) {
        validateCourseDate(course.getClassId(), course.getCourseDate());
        fillClassroomInfo(course);
        reservationService.checkConflict(
                course.getClassroomId(), course.getCourseDate(),
                course.getStartTime(), course.getEndTime(), course.getId());
        courseMapper.updateById(course);
        reservationService.upsertForCourse(course);
    }

    @Transactional
    public void deleteById(Long id) {
        reservationService.deleteByCourseId(id);
        courseMapper.deleteById(id);
    }

    private void fillClassroomInfo(Course course) {
        if (course.getClassroomId() == null) {
            course.setLocation(null);
            return;
        }
        Classroom classroom = classroomService.getById(course.getClassroomId());
        if (classroom == null || classroom.getStatus() == null || classroom.getStatus() != 1) {
            throw new IllegalArgumentException("所选教室不存在或已禁用");
        }
        course.setLocation(classroom.getName());
    }

    private void fillClassroomInfo(Long classroomId, CourseBatchDTO dto) {
        if (classroomId == null) {
            dto.setLocation(null);
            return;
        }
        Classroom classroom = classroomService.getById(classroomId);
        if (classroom == null || classroom.getStatus() == null || classroom.getStatus() != 1) {
            throw new IllegalArgumentException("所选教室不存在或已禁用");
        }
        dto.setLocation(classroom.getName());
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
