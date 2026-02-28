package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cm.entity.ClassInfo;
import com.cm.mapper.ClassInfoMapper;
import com.cm.config.JwtUserToken;
import com.cm.entity.UserClass;
import com.cm.mapper.UserClassMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassInfoService {

    @Autowired
    private ClassInfoMapper classInfoMapper;
    
    @Autowired
    private UserClassMapper userClassMapper;

    public Page<ClassInfo> page(long current, long size, String keyword) {
        LambdaQueryWrapper<ClassInfo> wrapper = new LambdaQueryWrapper<>();

        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            JwtUserToken token = (JwtUserToken) auth;
            if (token.getRole() != null && token.getRole() == 2) {
                List<UserClass> userClasses = userClassMapper.selectList(
                        new LambdaQueryWrapper<UserClass>().eq(UserClass::getUserId, token.getUserId()));
                if (userClasses.isEmpty()) {
                    return new Page<>(current, size);
                }
                List<Long> classIds = userClasses.stream().map(UserClass::getClassId).collect(Collectors.toList());
                wrapper.in(ClassInfo::getId, classIds);
            }
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(ClassInfo::getClassName, keyword)
                    .or().like(ClassInfo::getClassCode, keyword));
        }
        wrapper.orderByDesc(ClassInfo::getCreateTime);
        return classInfoMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public ClassInfo getById(Long id) {
        return classInfoMapper.selectById(id);
    }

    public void save(ClassInfo classInfo) {
        classInfo.setClassCode(generateClassCode());
        classInfoMapper.insert(classInfo);
    }

    public void update(ClassInfo classInfo) {
        classInfoMapper.updateById(classInfo);
    }

    public void deleteById(Long id) {
        classInfoMapper.deleteById(id);
    }

    /**
     * 生成班级编码：BJ{年份}-{4位序号}
     */
    private synchronized String generateClassCode() {
        String year = String.valueOf(LocalDate.now().getYear());
        String maxCode = classInfoMapper.getMaxCodeByYear(year);
        int seq = 1;
        if (maxCode != null) {
            // BJ2026-0001 -> 取最后4位
            String seqStr = maxCode.substring(maxCode.lastIndexOf('-') + 1);
            seq = Integer.parseInt(seqStr) + 1;
        }
        return String.format("BJ%s-%04d", year, seq);
    }
}
