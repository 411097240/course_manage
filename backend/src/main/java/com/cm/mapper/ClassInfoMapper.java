package com.cm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cm.entity.ClassInfo;
import org.apache.ibatis.annotations.Select;

public interface ClassInfoMapper extends BaseMapper<ClassInfo> {
    @Select("SELECT MAX(class_code) FROM class_info WHERE class_code LIKE CONCAT('BJ', #{year}, '-%')")
    String getMaxCodeByYear(String year);
}
