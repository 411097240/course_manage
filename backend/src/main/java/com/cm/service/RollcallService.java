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

    public Map<String, Object> getRollcallFormInfo(Long classId, LocalDate date) {
        if (date == null) date = LocalDate.now();
        
        List<StudentClass> scList = studentClassMapper.selectList(
            new LambdaQueryWrapper<StudentClass>().eq(StudentClass::getClassId, classId).eq(StudentClass::getStatus, 1)
        );
        List<Student> students = new ArrayList<>();
        if (!scList.isEmpty()) {
            List<Long> studentIds = scList.stream().map(StudentClass::getStudentId).collect(Collectors.toList());
            students = studentMapper.selectBatchIds(studentIds);
        }

        AttendanceRecord existing = recordMapper.selectOne(
            new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getClassId, classId)
                .eq(AttendanceRecord::getRecordDate, date)
        );

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

        AttendanceRecord existing = recordMapper.selectOne(
            new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getClassId, record.getClassId())
                .eq(AttendanceRecord::getRecordDate, record.getRecordDate())
        );

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
        // 查出该班级的所有签到记录
        List<AttendanceRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<AttendanceRecord>()
                        .eq(AttendanceRecord::getClassId, classId)
                        .orderByDesc(AttendanceRecord::getRecordDate)
        );

        // 查出该学生在这些记录中的签到详情
        List<Map<String, Object>> list = new ArrayList<>();
        int totalPresent = 0, totalLate = 0, totalLeave = 0, totalAbsent = 0;

        if (!records.isEmpty()) {
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
                    item.put("status", null); // 该次点名中没有该学生的记录
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
