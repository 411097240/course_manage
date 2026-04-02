package com.cm.controller;

import com.cm.common.Result;
import com.cm.config.JwtUserToken;
import com.cm.dto.LoginDTO;
import com.cm.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService userService;

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    @GetMapping("/info")
    public Result<?> info() {
        JwtUserToken token = (JwtUserToken) SecurityContextHolder.getContext().getAuthentication();
        return userService.getUserInfo(token.getUserId());
    }

    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestBody Map<String, String> params) {
        JwtUserToken token = (JwtUserToken) SecurityContextHolder.getContext().getAuthentication();
        String error = userService.changePassword(token.getUserId(), params.get("oldPassword"), params.get("newPassword"));
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok();
    }
}
