package com.cm.controller;

import com.cm.common.Result;
import com.cm.entity.Student;
import com.cm.config.JwtUserToken;
import com.cm.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(studentService.page(current, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.ok(studentService.getById(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody Student student) {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            JwtUserToken token = (JwtUserToken) auth;
            if (token.getRole() != null && token.getRole() == 2) {
                return Result.fail("无权限操作");
            }
        }
        String error = studentService.save(student);
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody Student student) {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            JwtUserToken token = (JwtUserToken) auth;
            if (token.getRole() != null && token.getRole() == 2) {
                return Result.fail("无权限操作");
            }
        }
        String error = studentService.update(student);
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            JwtUserToken token = (JwtUserToken) auth;
            if (token.getRole() != null && token.getRole() == 2) {
                return Result.fail("无权限操作");
            }
        }
        studentService.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/{id}/schedule")
    public Result<?> schedule(@PathVariable Long id) {
        return Result.ok(studentService.getStudentSchedule(id));
    }
}
