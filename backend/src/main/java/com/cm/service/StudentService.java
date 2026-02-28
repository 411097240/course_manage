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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        return studentMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public Student getById(Long id) {
        return studentMapper.selectById(id);
    }

    public void save(Student student) {
        student.setStudentNo(generateStudentNo());
        studentMapper.insert(student);
    }

    public void update(Student student) {
        studentMapper.updateById(student);
    }

    public void deleteById(Long id) {
        studentMapper.deleteById(id);
    }

    /**
     * 生成学号：S{年份}{5位序号}
     */
    private synchronized String generateStudentNo() {
        String year = String.valueOf(LocalDate.now().getYear());
        String maxNo = studentMapper.getMaxNoByYear(year);
        int seq = 1;
        if (maxNo != null) {
            // S202600001 -> 取后5位
            String seqStr = maxNo.substring(maxNo.length() - 5);
            seq = Integer.parseInt(seqStr) + 1;
        }
        return String.format("S%s%05d", year, seq);
    }

    /**
     * 学生入班
     */
    public String joinClass(Long studentId, Long classId) {
        // 检查是否已在读
        StudentClass existing = studentClassMapper.selectOne(
                new LambdaQueryWrapper<StudentClass>()
                        .eq(StudentClass::getStudentId, studentId)
                        .eq(StudentClass::getClassId, classId)
                        .eq(StudentClass::getStatus, 1));
        if (existing != null) {
            return "该学生已在此班级中";
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
        StudentClass existing = studentClassMapper.selectOne(
                new LambdaQueryWrapper<StudentClass>()
                        .eq(StudentClass::getStudentId, studentId)
                        .eq(StudentClass::getClassId, classId)
                        .eq(StudentClass::getStatus, 1));
        if (existing == null) {
            return "该学生不在此班级中";
        }
        existing.setStatus(0);
        existing.setLeaveTime(LocalDateTime.now());
        studentClassMapper.updateById(existing);
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
                        .orderByAsc(Course::getDayOfWeek)
                        .orderByAsc(Course::getStartTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Course course : courses) {
            Map<String, Object> item = new HashMap<>();
            item.put("courseId", course.getId());
            item.put("teacherName", course.getTeacherName());
            item.put("dayOfWeek", course.getDayOfWeek());
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
