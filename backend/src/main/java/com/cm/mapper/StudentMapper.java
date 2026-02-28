package com.cm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cm.entity.Student;
import org.apache.ibatis.annotations.Select;

public interface StudentMapper extends BaseMapper<Student> {
    @Select("SELECT MAX(student_no) FROM student WHERE student_no LIKE CONCAT('S', #{year}, '%')")
    String getMaxNoByYear(String year);
}
