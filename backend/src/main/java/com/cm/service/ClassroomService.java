package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cm.dto.ClassroomReservationVO;
import com.cm.entity.ClassInfo;
import com.cm.entity.Classroom;
import com.cm.entity.ClassroomReservation;
import com.cm.entity.Course;
import com.cm.mapper.ClassroomMapper;
import com.cm.mapper.ClassroomReservationMapper;
import com.cm.mapper.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassroomService {

    @Autowired
    private ClassroomMapper classroomMapper;

    @Autowired
    private ClassroomReservationMapper reservationMapper;

    @Autowired
    private ClassInfoService classInfoService;

    @Autowired
    private CourseMapper courseMapper;

    public Page<Classroom> page(long current, long size, String keyword) {
        LambdaQueryWrapper<Classroom> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Classroom::getName, keyword);
        }
        wrapper.orderByDesc(Classroom::getCreateTime);
        return classroomMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<Classroom> listAllEnabled() {
        return classroomMapper.selectList(
                new LambdaQueryWrapper<Classroom>()
                        .eq(Classroom::getStatus, 1)
                        .orderByAsc(Classroom::getName));
    }

    public Classroom getById(Long id) {
        return classroomMapper.selectById(id);
    }

    public String save(Classroom classroom) {
        String name = classroom.getName();
        if (name == null || name.trim().isEmpty()) {
            return "请输入教室名称";
        }
        name = name.trim();
        classroom.setName(name);

        Long count = classroomMapper.selectCount(
                new LambdaQueryWrapper<Classroom>().eq(Classroom::getName, name));
        if (count != null && count > 0) {
            return "教室名称已存在";
        }
        if (classroom.getStatus() == null) {
            classroom.setStatus(1);
        }
        classroomMapper.insert(classroom);
        return null;
    }

    public String update(Classroom classroom) {
        String name = classroom.getName();
        if (name == null || name.trim().isEmpty()) {
            return "请输入教室名称";
        }
        name = name.trim();
        classroom.setName(name);

        Long count = classroomMapper.selectCount(
                new LambdaQueryWrapper<Classroom>()
                        .eq(Classroom::getName, name)
                        .ne(Classroom::getId, classroom.getId()));
        if (count != null && count > 0) {
            return "教室名称已存在";
        }
        classroomMapper.updateById(classroom);
        return null;
    }

    public void deleteById(Long id) {
        classroomMapper.deleteById(id);
    }

    public List<ClassroomReservationVO> listReservations(Long classroomId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ClassroomReservation> wrapper = new LambdaQueryWrapper<ClassroomReservation>()
                .eq(ClassroomReservation::getClassroomId, classroomId)
                .ge(ClassroomReservation::getReserveDate, startDate)
                .le(ClassroomReservation::getReserveDate, endDate)
                .orderByAsc(ClassroomReservation::getReserveDate)
                .orderByAsc(ClassroomReservation::getStartTime);

        List<ClassroomReservation> reservations = reservationMapper.selectList(wrapper);
        if (reservations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> courseIds = reservations.stream()
                .map(ClassroomReservation::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Course> courseMap = courseIds.isEmpty() ? Collections.emptyMap()
                : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        Set<Long> classIds = courseMap.values().stream()
                .map(Course::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ClassInfo> classMap = classIds.stream()
                .map(classInfoService::getById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ClassInfo::getId, c -> c));

        List<ClassroomReservationVO> result = new ArrayList<>();
        for (ClassroomReservation r : reservations) {
            ClassroomReservationVO vo = new ClassroomReservationVO();
            vo.setId(r.getId());
            vo.setClassroomId(r.getClassroomId());
            vo.setCourseId(r.getCourseId());
            vo.setReserveDate(r.getReserveDate());
            vo.setStartTime(r.getStartTime());
            vo.setEndTime(r.getEndTime());

            if (r.getCourseId() == null) {
                vo.setManual(true);
                vo.setTitle(r.getTitle());
            } else {
                vo.setManual(false);
                Course course = courseMap.get(r.getCourseId());
                if (course != null) {
                    vo.setTeacherName(course.getTeacherName());
                    ClassInfo classInfo = classMap.get(course.getClassId());
                    if (classInfo != null) {
                        vo.setClassName(classInfo.getClassName());
                    }
                }
            }
            result.add(vo);
        }
        return result;
    }
}
