package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cm.entity.Course;
import com.cm.mapper.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseMapper courseMapper;

    public List<Course> listByClassId(Long classId) {
        return courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getClassId, classId)
                        .orderByAsc(Course::getDayOfWeek)
                        .orderByAsc(Course::getStartTime));
    }

    public void save(Course course) {
        courseMapper.insert(course);
    }

    public void update(Course course) {
        courseMapper.updateById(course);
    }

    public void deleteById(Long id) {
        courseMapper.deleteById(id);
    }
}
