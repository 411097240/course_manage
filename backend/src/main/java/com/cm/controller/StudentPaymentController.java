package com.cm.controller;

import com.cm.common.Result;
import com.cm.config.JwtUserToken;
import com.cm.dto.StudentPaymentSaveDTO;
import com.cm.service.StudentPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student-payment")
public class StudentPaymentController {

    @Autowired
    private StudentPaymentService paymentService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer paymentStatus,
                          @RequestParam(required = false) Long classId) {
        if (!isAdmin()) {
            return Result.fail("无权限操作");
        }
        return Result.ok(paymentService.page(current, size, keyword, paymentStatus, classId));
    }

    @GetMapping("/student/{studentId}")
    public Result<?> listByStudent(@PathVariable Long studentId) {
        if (!isAdmin()) {
            return Result.fail("无权限操作");
        }
        return Result.ok(paymentService.listByStudent(studentId));
    }

    @PutMapping
    public Result<?> save(@RequestBody StudentPaymentSaveDTO dto) {
        if (!isAdmin()) {
            return Result.fail("无权限操作");
        }
        String error = paymentService.save(dto);
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok();
    }

    private boolean isAdmin() {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            JwtUserToken token = (JwtUserToken) auth;
            return token.getRole() != null && token.getRole() == 1;
        }
        return false;
    }
}
