package com.cm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cm.entity.Student;
import com.cm.mapper.StudentMapper;
import com.cm.service.CourseService;
import com.cm.service.RollcallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * H5 班级空间 - 免鉴权接口（通过 accessToken 校验学生身份）
 */
@RestController
@RequestMapping("/api/h5/space")
public class H5ClassSpaceController {

    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private CourseService courseService;
    @Autowired
    private RollcallService rollcallService;

    /**
     * 获取班级课程表
     */
    @GetMapping("/schedule")
    public Map<String, Object> schedule(@RequestParam String token, @RequestParam Long classId) {
        Map<String, Object> map = new HashMap<>();
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getAccessToken, token));
        if (student == null) {
            map.put("code", 400);
            map.put("msg", "非法的访问链接");
            return map;
        }
        map.put("code", 200);
        map.put("msg", "success");
        map.put("data", courseService.listByClassId(classId));
        return map;
    }

    /**
     * 获取学生在某班级的签到记录汇总
     */
    @GetMapping("/attendance")
    public Map<String, Object> attendance(@RequestParam String token, @RequestParam Long classId) {
        Map<String, Object> map = new HashMap<>();
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getAccessToken, token));
        if (student == null) {
            map.put("code", 400);
            map.put("msg", "非法的访问链接");
            return map;
        }
        map.put("code", 200);
        map.put("msg", "success");
        map.put("data", rollcallService.getStudentAttendanceInClass(student.getId(), classId));
        return map;
    }
}
