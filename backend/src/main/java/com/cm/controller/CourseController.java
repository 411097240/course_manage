package com.cm.controller;

import com.cm.common.Result;
import com.cm.dto.CourseBatchDTO;
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
        try {
            courseService.save(course);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/batch")
    public Result<?> saveBatch(@RequestBody CourseBatchDTO dto) {
        try {
            courseService.saveBatch(dto);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping
    public Result<?> update(@RequestBody Course course) {
        try {
            courseService.update(course);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        courseService.deleteById(id);
        return Result.ok();
    }
}
