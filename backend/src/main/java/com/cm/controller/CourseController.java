package com.cm.controller;

import com.cm.common.Result;
import com.cm.entity.Course;
import com.cm.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/course")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam Long classId) {
        return Result.ok(courseService.listByClassId(classId));
    }

    @PostMapping
    public Result<?> save(@RequestBody Course course) {
        courseService.save(course);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody Course course) {
        courseService.update(course);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        courseService.deleteById(id);
        return Result.ok();
    }
}
