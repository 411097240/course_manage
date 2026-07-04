package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cm.entity.Student;
import com.cm.entity.StudentClass;
import com.cm.entity.ClassInfo;
import com.cm.entity.Course;
import com.cm.mapper.StudentMapper;
import com.cm.mapper.StudentClassMapper;
import com.cm.mapper.ClassInfoMapper;
import com.cm.mapper.CourseMapper;
import com.cm.mapper.UserClassMapper;
import com.cm.entity.UserClass;
import com.cm.config.JwtUserToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class StudentService {

    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private StudentClassMapper studentClassMapper;
    @Autowired
    private ClassInfoMapper classInfoMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private UserClassMapper userClassMapper;

    public Page<Student> page(long current, long size, String keyword) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();

        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            JwtUserToken token = (JwtUserToken) auth;
            if (token.getRole() != null && token.getRole() == 2) {
                List<UserClass> userClasses = userClassMapper.selectList(
                        new LambdaQueryWrapper<UserClass>().eq(UserClass::getUserId, token.getUserId()));
                if (userClasses.isEmpty()) {
                    return new Page<>(current, size);
                }
                List<Long> classIds = userClasses.stream().map(UserClass::getClassId).collect(Collectors.toList());
                
                List<StudentClass> scList = studentClassMapper.selectList(
                        new LambdaQueryWrapper<StudentClass>().in(StudentClass::getClassId, classIds).eq(StudentClass::getStatus, 1));
                if (scList.isEmpty()) {
                    return new Page<>(current, size);
                }
                List<Long> studentIds = scList.stream().map(StudentClass::getStudentId).distinct().collect(Collectors.toList());
                wrapper.in(Student::getId, studentIds);
            }
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Student::getName, keyword)
                    .or().like(Student::getStudentNo, keyword)
                    .or().like(Student::getPhone, keyword));
        }
        wrapper.orderByDesc(Student::getCreateTime);
        Page<Student> pageResult = studentMapper.selectPage(new Page<>(current, size), wrapper);
        
        List<Student> records = pageResult.getRecords();
        if (!records.isEmpty()) {
            List<Long> sids = records.stream().map(Student::getId).collect(Collectors.toList());
            List<StudentClass> currentClasses = studentClassMapper.selectList(
                    new LambdaQueryWrapper<StudentClass>()
                            .in(StudentClass::getStudentId, sids)
                            .eq(StudentClass::getStatus, 1)
            );
            if (!currentClasses.isEmpty()) {
                List<Long> cids = currentClasses.stream().map(StudentClass::getClassId).distinct().collect(Collectors.toList());
                List<ClassInfo> activeClasses = classInfoMapper.selectList(
                        new LambdaQueryWrapper<ClassInfo>()
                                .in(ClassInfo::getId, cids)
                                .eq(ClassInfo::getStatus, 1)
                );
                Map<Long, String> classNameMap = activeClasses.stream()
                        .collect(Collectors.toMap(ClassInfo::getId, ClassInfo::getClassName));

                Map<Long, List<String>> studentClassMap = new HashMap<>();
                for (StudentClass sc : currentClasses) {
                    if (classNameMap.containsKey(sc.getClassId())) {
                        studentClassMap.computeIfAbsent(sc.getStudentId(), k -> new ArrayList<>())
                                .add(classNameMap.get(sc.getClassId()));
                    }
                }
                for (Student s : records) {
                    List<String> names = studentClassMap.get(s.getId());
                    if (names != null && !names.isEmpty()) {
                        s.setActiveClassNames(String.join("，", names));
                    }
                }
            }
        }
        return pageResult;
    }

    public Student getById(Long id) {
        return studentMapper.selectById(id);
    }

    public String save(Student student) {
        String error = validateStudentNo(student.getStudentNo(), null);
        if (error != null) {
            return error;
        }
        student.setStudentNo(student.getStudentNo().trim());
        student.setAccessToken(UUID.randomUUID().toString().replace("-", ""));
        studentMapper.insert(student);
        return null;
    }

    public String update(Student student) {
        String error = validateStudentNo(student.getStudentNo(), student.getId());
        if (error != null) {
            return error;
        }
        student.setStudentNo(student.getStudentNo().trim());
        studentMapper.updateById(student);
        return null;
    }

    private String validateStudentNo(String studentNo, Long excludeId) {
        if (studentNo == null || studentNo.trim().isEmpty()) {
            return "请输入学号";
        }
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, studentNo.trim());
        if (excludeId != null) {
            wrapper.ne(Student::getId, excludeId);
        }
        Long count = studentMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            return "学号已存在，请更换";
        }
        return null;
    }

    public void deleteById(Long id) {
        studentMapper.deleteById(id);
    }

    /**
     * 学生入班
     */
    public String joinClass(Long studentId, Long classId) {
        // 取出该学生在该班级的所有历史记录(升序)
        List<StudentClass> existingList = studentClassMapper.selectList(
                new LambdaQueryWrapper<StudentClass>()
                        .eq(StudentClass::getStudentId, studentId)
                        .eq(StudentClass::getClassId, classId)
                        .orderByAsc(StudentClass::getJoinTime));

        if (!existingList.isEmpty()) {
            for (StudentClass sc : existingList) {
                if (sc.getStatus() != null && sc.getStatus() == 1) {
                    return "该学生已在此班级中";
                }
            }
            // 复用最早的一条记录
            StudentClass oldestRecord = existingList.get(0);
            oldestRecord.setStatus(1);
            oldestRecord.setJoinTime(LocalDateTime.now());
            // 如果使用 leaveTime，复用时应将其置空（但基于数据库兼容性可暂留，只要状态能表明在读即可）
            studentClassMapper.updateById(oldestRecord);
            return null;
        }

        StudentClass sc = new StudentClass();
        sc.setStudentId(studentId);
        sc.setClassId(classId);
        sc.setStatus(1);
        sc.setJoinTime(LocalDateTime.now());
        studentClassMapper.insert(sc);
        return null;
    }

    /**
     * 学生出班
     */
    public String leaveClass(Long studentId, Long classId) {
        List<StudentClass> existingList = studentClassMapper.selectList(
                new LambdaQueryWrapper<StudentClass>()
                        .eq(StudentClass::getStudentId, studentId)
                        .eq(StudentClass::getClassId, classId)
                        .eq(StudentClass::getStatus, 1));
        if (existingList.isEmpty()) {
            return "该学生不在此班级中";
        }
        for (StudentClass existing : existingList) {
            existing.setStatus(0);
            existing.setLeaveTime(LocalDateTime.now());
            studentClassMapper.updateById(existing);
        }
        return null;
    }

    /**
     * 查询学生已加入的班级列表
     */
    public List<Map<String, Object>> getStudentClasses(Long studentId) {
        List<StudentClass> scList = studentClassMapper.selectList(
                new LambdaQueryWrapper<StudentClass>()
                        .eq(StudentClass::getStudentId, studentId)
                        .orderByDesc(StudentClass::getJoinTime));
        if (scList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> classIds = scList.stream().map(StudentClass::getClassId).collect(Collectors.toList());
        List<ClassInfo> classes = classInfoMapper.selectBatchIds(classIds);
        Map<Long, ClassInfo> classMap = classes.stream().collect(Collectors.toMap(ClassInfo::getId, c -> c));

        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentClass sc : scList) {
            Map<String, Object> item = new HashMap<>();
            item.put("studentClassId", sc.getId());
            item.put("classId", sc.getClassId());
            item.put("status", sc.getStatus());
            item.put("joinTime", sc.getJoinTime());
            item.put("leaveTime", sc.getLeaveTime());
            ClassInfo ci = classMap.get(sc.getClassId());
            if (ci != null) {
                item.put("classCode", ci.getClassCode());
                item.put("className", ci.getClassName());
                item.put("classStatus", ci.getStatus());
                item.put("startDate", ci.getStartDate());
                item.put("endDate", ci.getEndDate());
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 查询学生所有在读班级的课程表
     */
    public List<Map<String, Object>> getStudentSchedule(Long studentId) {
        List<StudentClass> scList = studentClassMapper.selectList(
                new LambdaQueryWrapper<StudentClass>()
                        .eq(StudentClass::getStudentId, studentId)
                        .eq(StudentClass::getStatus, 1));
        if (scList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> classIds = scList.stream().map(StudentClass::getClassId).collect(Collectors.toList());
        List<ClassInfo> classes = classInfoMapper.selectBatchIds(classIds);
        Map<Long, ClassInfo> classMap = classes.stream().collect(Collectors.toMap(ClassInfo::getId, c -> c));

        // Filter out classes that have already ended
        List<Long> activeClassIds = classIds.stream()
                .filter(id -> classMap.containsKey(id) && classMap.get(id).getStatus() == 1)
                .collect(Collectors.toList());

        if (activeClassIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .in(Course::getClassId, activeClassIds)
                        .orderByAsc(Course::getCourseDate)
                        .orderByAsc(Course::getStartTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Course course : courses) {
            Map<String, Object> item = new HashMap<>();
            item.put("courseId", course.getId());
            item.put("teacherName", course.getTeacherName());
            item.put("courseDate", course.getCourseDate());
            item.put("startTime", course.getStartTime());
            item.put("endTime", course.getEndTime());
            item.put("location", course.getLocation());
            item.put("classId", course.getClassId());
            ClassInfo ci = classMap.get(course.getClassId());
            if (ci != null) {
                item.put("classCode", ci.getClassCode());
                item.put("className", ci.getClassName());
                item.put("startDate", ci.getStartDate());
                item.put("endDate", ci.getEndDate());
            }
            result.add(item);
        }
        return result;
    }
}
