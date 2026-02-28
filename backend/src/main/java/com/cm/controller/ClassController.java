package com.cm.controller;

import com.cm.common.Result;
import com.cm.config.JwtUserToken;
import com.cm.entity.ClassInfo;
import com.cm.service.ClassInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/class")
public class ClassController {

    @Autowired
    private ClassInfoService classInfoService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(classInfoService.page(current, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.ok(classInfoService.getById(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody ClassInfo classInfo) {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            JwtUserToken token = (JwtUserToken) auth;
            if (token.getRole() != null && token.getRole() == 2) {
                return Result.fail("无权限操作");
            }
        }
        classInfoService.save(classInfo);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody ClassInfo classInfo) {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            JwtUserToken token = (JwtUserToken) auth;
            if (token.getRole() != null && token.getRole() == 2) {
                return Result.fail("无权限操作");
            }
        }
        classInfoService.update(classInfo);
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
        classInfoService.deleteById(id);
        return Result.ok();
    }
}
