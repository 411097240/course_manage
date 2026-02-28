package com.cm.controller;

import com.cm.common.Result;
import com.cm.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student-class")
public class StudentClassController {

    @Autowired
    private StudentService studentService;

    @PostMapping("/join")
    public Result<?> join(@RequestBody Map<String, Long> params) {
        String error = studentService.joinClass(params.get("studentId"), params.get("classId"));
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok();
    }

    @PostMapping("/leave")
    public Result<?> leave(@RequestBody Map<String, Long> params) {
        String error = studentService.leaveClass(params.get("studentId"), params.get("classId"));
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam Long studentId) {
        return Result.ok(studentService.getStudentClasses(studentId));
    }
}
