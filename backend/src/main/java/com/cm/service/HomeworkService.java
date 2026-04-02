package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cm.config.JwtUserToken;
import com.cm.entity.ClassInfo;
import com.cm.entity.Homework;
import com.cm.entity.Student;
import com.cm.entity.StudentClass;
import com.cm.entity.StudentHomework;
import com.cm.entity.UserClass;
import com.cm.mapper.ClassInfoMapper;
import com.cm.mapper.HomeworkMapper;
import com.cm.mapper.StudentClassMapper;
import com.cm.mapper.StudentHomeworkMapper;
import com.cm.mapper.StudentMapper;
import com.cm.mapper.UserClassMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HomeworkService {

    @Autowired
    private HomeworkMapper homeworkMapper;
    @Autowired
    private StudentHomeworkMapper studentHomeworkMapper;
    @Autowired
    private StudentClassMapper studentClassMapper;
    @Autowired
    private UserClassMapper userClassMapper;
    @Autowired
    private ClassInfoMapper classInfoMapper;
    @Autowired
    private StudentMapper studentMapper;

    public Page<Map<String, Object>> pageHomework(long current, long size, Long classId, String keyword) {
        LambdaQueryWrapper<Homework> wrapper = new LambdaQueryWrapper<>();
        
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
                wrapper.in(Homework::getClassId, classIds);
            }
        }

        if (classId != null) {
            wrapper.eq(Homework::getClassId, classId);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Homework::getTitle, keyword);
        }
        wrapper.orderByDesc(Homework::getCreateTime);

        Page<Homework> page = homeworkMapper.selectPage(new Page<>(current, size), wrapper);
        
        // 装配 classInfo 数据
        Page<Map<String, Object>> resultPage = new Page<>(current, size, page.getTotal());
        if (page.getRecords().isEmpty()) return resultPage;

        List<Long> classIds = page.getRecords().stream().map(Homework::getClassId).collect(Collectors.toList());
        List<ClassInfo> classes = classInfoMapper.selectBatchIds(classIds);
        Map<Long, ClassInfo> classMap = classes.stream().collect(Collectors.toMap(ClassInfo::getId, c -> c));

        List<Map<String, Object>> records = page.getRecords().stream().map(hw -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", hw.getId());
            map.put("classId", hw.getClassId());
            map.put("title", hw.getTitle());
            map.put("description", hw.getDescription());
            map.put("attachments", hw.getAttachments());
            map.put("answerAttachments", hw.getAnswerAttachments());
            map.put("isAnswerPublished", hw.getIsAnswerPublished() != null ? hw.getIsAnswerPublished() : 0);
            map.put("deadline", hw.getDeadline());
            map.put("createTime", hw.getCreateTime());
            ClassInfo ci = classMap.get(hw.getClassId());
            if (ci != null) {
                map.put("className", ci.getClassName());
            }
            return map;
        }).collect(Collectors.toList());

        resultPage.setRecords(records);
        return resultPage;
    }

    @Transactional
    public void createHomework(Homework homework) {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            homework.setCreateBy(((JwtUserToken) auth).getUserId());
        }
        
        homeworkMapper.insert(homework);

        // 为该班级的在读学生发布作业
        List<StudentClass> scList = studentClassMapper.selectList(
                new LambdaQueryWrapper<StudentClass>()
                        .eq(StudentClass::getClassId, homework.getClassId())
                        .eq(StudentClass::getStatus, 1) // 1为正常在读
        );

        for (StudentClass sc : scList) {
            StudentHomework sh = new StudentHomework();
            sh.setHomeworkId(homework.getId());
            sh.setStudentId(sc.getStudentId());
            sh.setStatus(0); // 0: 待提交
            studentHomeworkMapper.insert(sh);
        }
    }

    public void updateHomework(Homework homework) {
        homeworkMapper.updateById(homework);
    }

    @Transactional
    public void deleteHomework(Long id) {
        homeworkMapper.deleteById(id);
        studentHomeworkMapper.delete(new LambdaQueryWrapper<StudentHomework>().eq(StudentHomework::getHomeworkId, id));
    }

    /**
     * 查询某次作业的所有学生提交情况
     */
    public List<Map<String, Object>> getStudentHomeworks(Long homeworkId) {
        List<StudentHomework> shList = studentHomeworkMapper.selectList(
                new LambdaQueryWrapper<StudentHomework>().eq(StudentHomework::getHomeworkId, homeworkId)
        );
        if (shList.isEmpty()) return Collections.emptyList();

        List<Long> studentIds = shList.stream().map(StudentHomework::getStudentId).collect(Collectors.toList());
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Map<Long, Student> stuMap = students.stream().collect(Collectors.toMap(Student::getId, s -> s));

        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentHomework sh : shList) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", sh.getId());
            item.put("homeworkId", sh.getHomeworkId());
            item.put("studentId", sh.getStudentId());
            item.put("submitAttachments", sh.getSubmitAttachments());
            item.put("teacherFeedbackAttachments", sh.getTeacherFeedbackAttachments());
            item.put("teacherComment", sh.getTeacherComment());
            item.put("status", sh.getStatus());
            item.put("submitTime", sh.getSubmitTime());
            item.put("reviewTime", sh.getReviewTime());

            Student s = stuMap.get(sh.getStudentId());
            if (s != null) {
                item.put("studentName", s.getName());
                item.put("studentNo", s.getStudentNo());
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 老师批改作业
     */
    public void reviewStudentHomework(StudentHomework req) {
        StudentHomework sh = studentHomeworkMapper.selectById(req.getId());
        if (sh != null) {
            sh.setTeacherFeedbackAttachments(req.getTeacherFeedbackAttachments());
            sh.setTeacherComment(req.getTeacherComment());
            sh.setStatus(req.getStatus()); // 2: 待修正, 3: 通过
            sh.setReviewTime(LocalDateTime.now());
            studentHomeworkMapper.updateById(sh);
        }
    }

    // ==========================================
    // 学端使用的免鉴权接口
    // ==========================================

    public Map<String, Object> getH5ListByToken(String token, Long classId) {
        Map<String, Object> result = new HashMap<>();

        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getAccessToken, token));
        if (student == null) {
            result.put("error", "非法的访问链接");
            return result;
        }

        List<Homework> hwList = homeworkMapper.selectList(
                new LambdaQueryWrapper<Homework>().eq(Homework::getClassId, classId).orderByDesc(Homework::getCreateTime)
        );

        if (hwList.isEmpty()) {
            result.put("list", Collections.emptyList());
            return result;
        }

        List<Long> hwIds = hwList.stream().map(Homework::getId).collect(Collectors.toList());
        List<StudentHomework> shList = studentHomeworkMapper.selectList(
                new LambdaQueryWrapper<StudentHomework>()
                        .in(StudentHomework::getHomeworkId, hwIds)
                        .eq(StudentHomework::getStudentId, student.getId())
        );
        Map<Long, StudentHomework> shMap = shList.stream().collect(Collectors.toMap(StudentHomework::getHomeworkId, s -> s));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Homework hw : hwList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", hw.getId());
            map.put("title", hw.getTitle());
            map.put("deadline", hw.getDeadline());
            StudentHomework sh = shMap.get(hw.getId());
            if (sh != null) {
                map.put("status", sh.getStatus());
            } else {
                continue; // Student not assigned this homework? Unlikely but skip
            }
            list.add(map);
        }

        result.put("list", list);
        ClassInfo ci = classInfoMapper.selectById(classId);
        result.put("className", ci != null ? ci.getClassName() : "");
        result.put("studentName", student.getName());
        return result;
    }

    public Map<String, Object> getH5DetailByToken(Long homeworkId, String token) {
        Map<String, Object> result = new HashMap<>();
        
        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getAccessToken, token));
        if (student == null) {
            result.put("error", "非法的访问链接");
            return result;
        }

        Homework hw = homeworkMapper.selectById(homeworkId);
        if (hw == null) {
            result.put("error", "作业不存在");
            return result;
        }

        StudentHomework sh = studentHomeworkMapper.selectOne(
                new LambdaQueryWrapper<StudentHomework>()
                        .eq(StudentHomework::getHomeworkId, homeworkId)
                        .eq(StudentHomework::getStudentId, student.getId())
        );

        if (sh == null) {
            result.put("error", "您不在此次作业的提交名单中");
            return result;
        }

        // 学生端访问：如果老师没有公布答案，则屏蔽答案附件内容
        if (hw.getIsAnswerPublished() == null || hw.getIsAnswerPublished() != 1) {
            hw.setAnswerAttachments(null);
        }

        result.put("homework", hw);
        result.put("studentHomework", sh);
        result.put("studentInfo", student);
        
        ClassInfo ci = classInfoMapper.selectById(hw.getClassId());
        result.put("className", ci != null ? ci.getClassName() : "");

        return result;
    }

    public void submitH5HomeworkByToken(StudentHomework req, String token) {
        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getAccessToken, token));
        if (student == null) return;

        StudentHomework sh = studentHomeworkMapper.selectById(req.getId());
        if (sh != null && sh.getStudentId().equals(student.getId())) {
            sh.setSubmitAttachments(req.getSubmitAttachments());
            sh.setStatus(1); // 1: 已提交
            sh.setSubmitTime(LocalDateTime.now());
            studentHomeworkMapper.updateById(sh);
        }
    }
}
