package com.cm.controller;

import com.cm.common.Result;
import com.cm.entity.SysUser;
import com.cm.service.SysUserService;
import com.cm.service.UserClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user-class")
public class UserClassController {

    @Autowired
    private UserClassService userClassService;
    @Autowired
    private SysUserService userService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam Long userId) {
        return Result.ok(userClassService.getByUserId(userId));
    }

    @PostMapping("/assign")
    public Result<?> assign(@RequestBody Map<String, Long> params) {
        String error = userClassService.assign(params.get("userId"), params.get("classId"));
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok();
    }

    @PostMapping("/remove")
    public Result<?> remove(@RequestBody Map<String, Long> params) {
        userClassService.remove(params.get("userId"), params.get("classId"));
        return Result.ok();
    }

    @GetMapping("/teachers")
    public Result<?> teachers() {
        return Result.ok(userService.getTeachers());
    }

    @PostMapping("/teacher")
    public Result<?> createTeacher(@RequestBody SysUser user) {
        String error = userService.createTeacher(user);
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok();
    }

    @PostMapping("/reset-password")
    public Result<?> resetPassword(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String newPassword = params.getOrDefault("newPassword", "123456").toString();
        userService.resetPassword(userId, newPassword);
        return Result.ok();
    }
}
