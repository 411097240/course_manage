package com.cm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cm.entity.Homework;
import com.cm.entity.StudentHomework;
import com.cm.service.HomeworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/homework")
public class HomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    @GetMapping("/page")
    public Map<String, Object> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String keyword) {
        Page<Map<String, Object>> result = homeworkService.pageHomework(current, size, classId, keyword);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        map.put("data", result);
        return map;
    }

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Homework homework) {
        homeworkService.createHomework(homework);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "Created successfully, distributed to class students automatically!");
        return map;
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody Homework homework) {
        homeworkService.updateHomework(homework);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        return map;
    }

    @PostMapping("/delete")
    public Map<String, Object> delete(@RequestParam Long id) {
        homeworkService.deleteHomework(id);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        return map;
    }

    @GetMapping("/studentHomeworkList")
    public Map<String, Object> studentHomeworkList(@RequestParam Long homeworkId) {
        List<Map<String, Object>> result = homeworkService.getStudentHomeworks(homeworkId);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        map.put("data", result);
        return map;
    }

    @PostMapping("/review")
    public Map<String, Object> review(@RequestBody StudentHomework studentHomework) {
        homeworkService.reviewStudentHomework(studentHomework);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "Review recorded");
        return map;
    }
}
