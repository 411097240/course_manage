package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cm.config.JwtUserToken;
import com.cm.entity.*;
import com.cm.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RollcallService {
    @Autowired private AttendanceRecordMapper recordMapper;
    @Autowired private AttendanceDetailMapper detailMapper;
    @Autowired private StudentClassMapper studentClassMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private ClassInfoMapper classInfoMapper;
    @Autowired private UserClassMapper userClassMapper;
    @Autowired private SysUserMapper sysUserMapper;
    @Autowired private CourseService courseService;

    public List<Map<String, Object>> listTodayCourses() {
        List<Long> classIds = resolveAccessibleClassIds();
        if (classIds.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        List<Course> courses = courseService.listByClassIdsAndDate(classIds, today);
        if (courses.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> courseClassIds = courses.stream().map(Course::getClassId).distinct().collect(Collectors.toList());
        List<ClassInfo> classes = classInfoMapper.selectBatchIds(courseClassIds);
        Map<Long, ClassInfo> classMap = classes.stream().collect(Collectors.toMap(ClassInfo::getId, c -> c));

        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
        List<AttendanceRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<AttendanceRecord>().in(AttendanceRecord::getCourseId, courseIds));
        Set<Long> recordedCourseIds = records.stream()
                .map(AttendanceRecord::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Course course : courses) {
            ClassInfo classInfo = classMap.get(course.getClassId());
            Map<String, Object> item = new HashMap<>();
            item.put("courseId", course.getId());
            item.put("classId", course.getClassId());
            item.put("className", classInfo != null ? classInfo.getClassName() : "");
            item.put("classCode", classInfo != null ? classInfo.getClassCode() : "");
            item.put("startTime", course.getStartTime());
            item.put("endTime", course.getEndTime());
            item.put("location", course.getLocation());
            item.put("hasExisting", recordedCourseIds.contains(course.getId()));
            result.add(item);
        }
        return result;
    }

    private List<Long> resolveAccessibleClassIds() {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            JwtUserToken token = (JwtUserToken) auth;
            if (token.getRole() != null && token.getRole() == 2) {
                List<UserClass> userClasses = userClassMapper.selectList(
                        new LambdaQueryWrapper<UserClass>().eq(UserClass::getUserId, token.getUserId()));
                return userClasses.stream().map(UserClass::getClassId).collect(Collectors.toList());
            }
        }
        List<ClassInfo> allClasses = classInfoMapper.selectList(new LambdaQueryWrapper<>());
        return allClasses.stream().map(ClassInfo::getId).collect(Collectors.toList());
    }

    public Map<String, Object> getRollcallFormInfo(Long classId, LocalDate date, Long courseId) {
        if (date == null) date = LocalDate.now();
        
        List<StudentClass> scList = studentClassMapper.selectList(
            new LambdaQueryWrapper<StudentClass>().eq(StudentClass::getClassId, classId).eq(StudentClass::getStatus, 1)
        );
        List<Student> students = new ArrayList<>();
        if (!scList.isEmpty()) {
            List<Long> studentIds = scList.stream().map(StudentClass::getStudentId).collect(Collectors.toList());
            students = studentMapper.selectBatchIds(studentIds);
        }

        AttendanceRecord existing;
        if (courseId != null) {
            existing = recordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecord>().eq(AttendanceRecord::getCourseId, courseId));
        } else {
            existing = recordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecord>()
                    .eq(AttendanceRecord::getClassId, classId)
                    .eq(AttendanceRecord::getRecordDate, date)
                    .isNull(AttendanceRecord::getCourseId));
        }

        Map<Long, Integer> statusMap = new HashMap<>();
        if (existing != null) {
            List<AttendanceDetail> existingDetails = detailMapper.selectList(
                new LambdaQueryWrapper<AttendanceDetail>().eq(AttendanceDetail::getRecordId, existing.getId())
            );
            for (AttendanceDetail d : existingDetails) {
                statusMap.put(d.getStudentId(), d.getStatus());
            }
        }

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Student s : students) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getName());
            map.put("studentNo", s.getStudentNo());
            map.put("status", statusMap.getOrDefault(s.getId(), 1)); // Default 1
            resultList.add(map);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("students", resultList);
        res.put("hasExisting", existing != null);
        return res;
    }

    @Transactional
    public void submitRollcall(AttendanceRecord record, List<AttendanceDetail> details) {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            record.setTeacherId(((JwtUserToken) auth).getUserId());
        }
        if (record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }

        AttendanceRecord existing;
        if (record.getCourseId() != null) {
            existing = recordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecord>()
                    .eq(AttendanceRecord::getCourseId, record.getCourseId()));
        } else {
            existing = recordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecord>()
                    .eq(AttendanceRecord::getClassId, record.getClassId())
                    .eq(AttendanceRecord::getRecordDate, record.getRecordDate())
                    .isNull(AttendanceRecord::getCourseId));
        }

        if (existing != null) {
            record.setId(existing.getId());
            recordMapper.updateById(record);
            detailMapper.delete(new LambdaQueryWrapper<AttendanceDetail>().eq(AttendanceDetail::getRecordId, existing.getId()));
        } else {
            recordMapper.insert(record);
        }

        for (AttendanceDetail detail : details) {
            detail.setId(null); // Ensure insert creates new records
            detail.setRecordId(record.getId());
            detailMapper.insert(detail);
        }
    }

    public Page<Map<String, Object>> getRecords(Long classId, long current, long size) {
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            JwtUserToken token = (JwtUserToken) auth;
            if (token.getRole() != null && token.getRole() == 2) {
                // 教师只能看自己教的班级
                List<UserClass> userClasses = userClassMapper.selectList(
                        new LambdaQueryWrapper<UserClass>().eq(UserClass::getUserId, token.getUserId()));
                if (userClasses.isEmpty()) {
                    return new Page<>(current, size);
                }
                List<Long> classIds = userClasses.stream().map(UserClass::getClassId).collect(Collectors.toList());
                wrapper.in(AttendanceRecord::getClassId, classIds);
            }
        }
        if (classId != null) {
            wrapper.eq(AttendanceRecord::getClassId, classId);
        }
        wrapper.orderByDesc(AttendanceRecord::getCreateTime);

        Page<AttendanceRecord> page = recordMapper.selectPage(new Page<>(current, size), wrapper);
        Page<Map<String, Object>> result = new Page<>(current, size, page.getTotal());
        if (page.getRecords().isEmpty()) return result;

        List<Long> classIds = page.getRecords().stream().map(AttendanceRecord::getClassId).collect(Collectors.toList());
        List<ClassInfo> classes = classInfoMapper.selectBatchIds(classIds);
        Map<Long, String> classMap = classes.stream().collect(Collectors.toMap(ClassInfo::getId, ClassInfo::getClassName));

        List<Long> teacherIds = page.getRecords().stream().map(AttendanceRecord::getTeacherId).collect(Collectors.toList());
        Map<Long, String> tempTeacherMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            List<SysUser> teachers = sysUserMapper.selectBatchIds(teacherIds);
            tempTeacherMap = teachers.stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        }
        final Map<Long, String> teacherMap = tempTeacherMap;

        List<Map<String, Object>> mappedRecords = page.getRecords().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("classId", r.getClassId());
            map.put("className", classMap.get(r.getClassId()));
            map.put("teacherId", r.getTeacherId());
            map.put("teacherName", teacherMap.get(r.getTeacherId()));
            map.put("recordDate", r.getRecordDate());
            map.put("createTime", r.getCreateTime());
            return map;
        }).collect(Collectors.toList());
        
        result.setRecords(mappedRecords);
        return result;
    }

    public List<Map<String, Object>> getDetails(Long recordId) {
        List<AttendanceDetail> details = detailMapper.selectList(
            new LambdaQueryWrapper<AttendanceDetail>().eq(AttendanceDetail::getRecordId, recordId)
        );
        if (details.isEmpty()) return Collections.emptyList();
        
        List<Long> studentIds = details.stream().map(AttendanceDetail::getStudentId).collect(Collectors.toList());
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Map<Long, Student> stuMap = students.stream().collect(Collectors.toMap(Student::getId, s -> s));

        return details.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("studentId", d.getStudentId());
            map.put("status", d.getStatus());
            Student s = stuMap.get(d.getStudentId());
            if (s != null) {
                map.put("studentNo", s.getStudentNo());
                map.put("studentName", s.getName());
            }
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * H5端：查询某学生在某班级的签到汇总
     */
    public Map<String, Object> getStudentAttendanceInClass(Long studentId, Long classId) {
        List<AttendanceRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<AttendanceRecord>()
                        .eq(AttendanceRecord::getClassId, classId)
                        .orderByDesc(AttendanceRecord::getRecordDate)
        );

        List<Map<String, Object>> list = new ArrayList<>();
        int totalPresent = 0, totalLate = 0, totalLeave = 0, totalAbsent = 0;

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        String className = classInfo != null ? classInfo.getClassName() : "";

        if (!records.isEmpty()) {
            List<Long> courseIds = records.stream()
                    .map(AttendanceRecord::getCourseId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, Course> courseMap = courseService.mapByIds(courseIds);

            records.sort((a, b) -> {
                int dateCmp = b.getRecordDate().compareTo(a.getRecordDate());
                if (dateCmp != 0) return dateCmp;
                String timeA = courseMap.containsKey(a.getCourseId()) ? courseMap.get(a.getCourseId()).getStartTime() : "";
                String timeB = courseMap.containsKey(b.getCourseId()) ? courseMap.get(b.getCourseId()).getStartTime() : "";
                return timeB.compareTo(timeA);
            });

            List<Long> recordIds = records.stream().map(AttendanceRecord::getId).collect(Collectors.toList());
            List<AttendanceDetail> details = detailMapper.selectList(
                    new LambdaQueryWrapper<AttendanceDetail>()
                            .in(AttendanceDetail::getRecordId, recordIds)
                            .eq(AttendanceDetail::getStudentId, studentId)
            );
            Map<Long, AttendanceDetail> detailMap = details.stream()
                    .collect(Collectors.toMap(AttendanceDetail::getRecordId, d -> d));

            for (AttendanceRecord rec : records) {
                Map<String, Object> item = new HashMap<>();
                item.put("recordDate", rec.getRecordDate());
                item.put("className", className);
                Course course = rec.getCourseId() != null ? courseMap.get(rec.getCourseId()) : null;
                if (course != null) {
                    item.put("startTime", course.getStartTime());
                    item.put("endTime", course.getEndTime());
                }
                AttendanceDetail d = detailMap.get(rec.getId());
                if (d != null) {
                    item.put("status", d.getStatus());
                    switch (d.getStatus()) {
                        case 1: totalPresent++; break;
                        case 2: totalLate++; break;
                        case 3: totalLeave++; break;
                        case 4: totalAbsent++; break;
                    }
                } else {
                    item.put("status", null);
                }
                list.add(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("totalPresent", totalPresent);
        result.put("totalLate", totalLate);
        result.put("totalLeave", totalLeave);
        result.put("totalAbsent", totalAbsent);
        result.put("totalCount", records.size());
        return result;
    }
}
