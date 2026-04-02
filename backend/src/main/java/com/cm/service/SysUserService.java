package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cm.common.Result;
import com.cm.dto.LoginDTO;
import com.cm.entity.SysUser;
import com.cm.mapper.SysUserMapper;
import com.cm.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysUserService {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    public Result<?> login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            return Result.fail("账号已被禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        return Result.ok(data);
    }

    public Result<?> getUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setPassword(null);
        return Result.ok(user);
    }

    public List<SysUser> getTeachers() {
        return userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, 2)
                        .select(SysUser::getId, SysUser::getUsername, SysUser::getRealName, SysUser::getStatus));
    }

    /**
     * 创建教师账号
     */
    public String createTeacher(SysUser user) {
        // 检查用户名是否已存在
        SysUser existing = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (existing != null) {
            return "用户名已存在";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(2);
        user.setStatus(1);
        userMapper.insert(user);
        return null;
    }

    /**
     * 重置密码（默认重置为 123456）
     */
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    /**
     * 切换教师账号启用/停用状态
     */
    public void toggleTeacherStatus(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user != null && user.getRole() == 2) {
            SysUser update = new SysUser();
            update.setId(userId);
            update.setStatus(user.getStatus() == 1 ? 0 : 1);
            userMapper.updateById(update);
        }
    }

    /**
     * 用户修改自己的密码
     */
    public String changePassword(Long userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return "新密码长度不能少于6位";
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return "用户不存在";
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return "旧密码不正确";
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
        return null;
    }
}
