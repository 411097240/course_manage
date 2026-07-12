package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cm.dto.ManualReservationDTO;
import com.cm.entity.ClassInfo;
import com.cm.entity.Classroom;
import com.cm.entity.ClassroomReservation;
import com.cm.entity.Course;
import com.cm.mapper.ClassroomReservationMapper;
import com.cm.mapper.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ClassroomReservationService {

    @Autowired
    private ClassroomReservationMapper reservationMapper;

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private ClassInfoService classInfoService;

    @Autowired
    private CourseMapper courseMapper;

    public void checkConflict(Long classroomId, LocalDate date, String startTime, String endTime, Long excludeCourseId) {
        checkConflict(classroomId, date, startTime, endTime, excludeCourseId, null);
    }

    public void checkConflict(Long classroomId, LocalDate date, String startTime, String endTime,
                              Long excludeCourseId, Long excludeReservationId) {
        if (classroomId == null) {
            return;
        }
        validateTimeRange(date, startTime, endTime);

        LambdaQueryWrapper<ClassroomReservation> wrapper = new LambdaQueryWrapper<ClassroomReservation>()
                .eq(ClassroomReservation::getClassroomId, classroomId)
                .eq(ClassroomReservation::getReserveDate, date)
                .lt(ClassroomReservation::getStartTime, endTime)
                .gt(ClassroomReservation::getEndTime, startTime);

        if (excludeCourseId != null) {
            wrapper.and(w -> w.isNull(ClassroomReservation::getCourseId)
                    .or()
                    .ne(ClassroomReservation::getCourseId, excludeCourseId));
        }
        if (excludeReservationId != null) {
            wrapper.ne(ClassroomReservation::getId, excludeReservationId);
        }

        List<ClassroomReservation> conflicts = reservationMapper.selectList(wrapper);
        if (!conflicts.isEmpty()) {
            ClassroomReservation conflict = conflicts.get(0);
            Classroom classroom = classroomService.getById(classroomId);
            String classroomName = classroom != null ? classroom.getName() : "教室";
            throw new IllegalArgumentException(buildConflictMessage(classroomName, conflict));
        }
    }

    private void validateTimeRange(LocalDate date, String startTime, String endTime) {
        if (date == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("预约日期和时间不能为空");
        }
        if (!startTime.matches("\\d{2}:\\d{2}") || !endTime.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("时间格式不正确");
        }
        if (startTime.compareTo(endTime) >= 0) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
    }

    private String buildConflictMessage(String classroomName, ClassroomReservation conflict) {
        String suffix = "";
        if (conflict.getCourseId() != null) {
            Course course = courseMapper.selectById(conflict.getCourseId());
            if (course != null) {
                ClassInfo classInfo = classInfoService.getById(course.getClassId());
                if (classInfo != null) {
                    suffix = "（" + classInfo.getClassName() + "）";
                }
            }
        } else if (conflict.getTitle() != null && !conflict.getTitle().isEmpty()) {
            suffix = "（" + conflict.getTitle() + "）";
        }
        return String.format("教室「%s」在 %s %s-%s 已被占用%s",
                classroomName, conflict.getReserveDate(), conflict.getStartTime(), conflict.getEndTime(), suffix);
    }

    public void saveManual(ManualReservationDTO dto) {
        validateManualDto(dto, false);
        checkConflict(dto.getClassroomId(), dto.getReserveDate(), dto.getStartTime(), dto.getEndTime(), null, null);

        ClassroomReservation reservation = new ClassroomReservation();
        reservation.setClassroomId(dto.getClassroomId());
        reservation.setReserveDate(dto.getReserveDate());
        reservation.setStartTime(dto.getStartTime());
        reservation.setEndTime(dto.getEndTime());
        reservation.setTitle(dto.getTitle().trim());
        reservationMapper.insert(reservation);
    }

    public void updateManual(ManualReservationDTO dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("预约ID不能为空");
        }
        ClassroomReservation existing = reservationMapper.selectById(dto.getId());
        if (existing == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        if (existing.getCourseId() != null) {
            throw new IllegalArgumentException("课程预约不可在此修改，请在课程管理中操作");
        }

        validateManualDto(dto, true);
        checkConflict(dto.getClassroomId(), dto.getReserveDate(), dto.getStartTime(), dto.getEndTime(),
                null, dto.getId());

        existing.setClassroomId(dto.getClassroomId());
        existing.setReserveDate(dto.getReserveDate());
        existing.setStartTime(dto.getStartTime());
        existing.setEndTime(dto.getEndTime());
        existing.setTitle(dto.getTitle().trim());
        reservationMapper.updateById(existing);
    }

    public void deleteManual(Long id) {
        ClassroomReservation existing = reservationMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        if (existing.getCourseId() != null) {
            throw new IllegalArgumentException("课程预约不可在此删除，请在课程管理中操作");
        }
        reservationMapper.deleteById(id);
    }

    private void validateManualDto(ManualReservationDTO dto, boolean isUpdate) {
        if (dto.getClassroomId() == null) {
            throw new IllegalArgumentException("教室不能为空");
        }
        Classroom classroom = classroomService.getById(dto.getClassroomId());
        if (classroom == null || classroom.getStatus() == null || classroom.getStatus() != 1) {
            throw new IllegalArgumentException("所选教室不存在或已禁用");
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("请输入预约事由");
        }
        validateTimeRange(dto.getReserveDate(), dto.getStartTime(), dto.getEndTime());
    }

    public void upsertForCourse(Course course) {
        ClassroomReservation existing = reservationMapper.selectOne(
                new LambdaQueryWrapper<ClassroomReservation>()
                        .eq(ClassroomReservation::getCourseId, course.getId()));

        if (course.getClassroomId() == null) {
            if (existing != null) {
                reservationMapper.deleteById(existing.getId());
            }
            return;
        }

        if (existing != null) {
            existing.setClassroomId(course.getClassroomId());
            existing.setReserveDate(course.getCourseDate());
            existing.setStartTime(course.getStartTime());
            existing.setEndTime(course.getEndTime());
            reservationMapper.updateById(existing);
        } else {
            ClassroomReservation reservation = new ClassroomReservation();
            reservation.setClassroomId(course.getClassroomId());
            reservation.setCourseId(course.getId());
            reservation.setReserveDate(course.getCourseDate());
            reservation.setStartTime(course.getStartTime());
            reservation.setEndTime(course.getEndTime());
            reservationMapper.insert(reservation);
        }
    }

    public void deleteByCourseId(Long courseId) {
        ClassroomReservation existing = reservationMapper.selectOne(
                new LambdaQueryWrapper<ClassroomReservation>()
                        .eq(ClassroomReservation::getCourseId, courseId));
        if (existing != null) {
            reservationMapper.deleteById(existing.getId());
        }
    }
}
