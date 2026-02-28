package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cm.entity.UserClass;
import com.cm.entity.ClassInfo;
import com.cm.mapper.UserClassMapper;
import com.cm.mapper.ClassInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserClassService {

    @Autowired
    private UserClassMapper userClassMapper;
    @Autowired
    private ClassInfoMapper classInfoMapper;

    /**
     * 查询教师管理的班级
     */
    public List<Map<String, Object>> getByUserId(Long userId) {
        List<UserClass> ucList = userClassMapper.selectList(
                new LambdaQueryWrapper<UserClass>().eq(UserClass::getUserId, userId));
        if (ucList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> classIds = ucList.stream().map(UserClass::getClassId).collect(Collectors.toList());
        List<ClassInfo> classes = classInfoMapper.selectBatchIds(classIds);
        Map<Long, ClassInfo> classMap = classes.stream().collect(Collectors.toMap(ClassInfo::getId, c -> c));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserClass uc : ucList) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", uc.getId());
            item.put("userId", uc.getUserId());
            item.put("classId", uc.getClassId());
            ClassInfo ci = classMap.get(uc.getClassId());
            if (ci != null) {
                item.put("classCode", ci.getClassCode());
                item.put("className", ci.getClassName());
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 给教师分配班级
     */
    public String assign(Long userId, Long classId) {
        UserClass existing = userClassMapper.selectOne(
                new LambdaQueryWrapper<UserClass>()
                        .eq(UserClass::getUserId, userId)
                        .eq(UserClass::getClassId, classId));
        if (existing != null) {
            return "该教师已管理此班级";
        }
        UserClass uc = new UserClass();
        uc.setUserId(userId);
        uc.setClassId(classId);
        userClassMapper.insert(uc);
        return null;
    }

    /**
     * 移除教师的班级管理权限
     */
    public void remove(Long userId, Long classId) {
        userClassMapper.delete(
                new LambdaQueryWrapper<UserClass>()
                        .eq(UserClass::getUserId, userId)
                        .eq(UserClass::getClassId, classId));
    }

    /**
     * 获取教师管理的班级ID列表
     */
    public List<Long> getManagedClassIds(Long userId) {
        List<UserClass> list = userClassMapper.selectList(
                new LambdaQueryWrapper<UserClass>().eq(UserClass::getUserId, userId));
        return list.stream().map(UserClass::getClassId).collect(Collectors.toList());
    }
}
