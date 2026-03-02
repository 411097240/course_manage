package com.cm.controller;

import com.cm.entity.StudentHomework;
import com.cm.service.HomeworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/h5/homework")
public class H5HomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam String token, @RequestParam Long classId) {
        Map<String, Object> result = homeworkService.getH5ListByToken(token, classId);
        Map<String, Object> map = new HashMap<>();
        if (result.containsKey("error")) {
            map.put("code", 400);
            map.put("msg", result.get("error"));
        } else {
            map.put("code", 200);
            map.put("msg", "success");
            map.put("data", result);
        }
        return map;
    }

    @GetMapping("/detail")
    public Map<String, Object> detail(@RequestParam Long homeworkId, @RequestParam String token) {
        Map<String, Object> result = homeworkService.getH5DetailByToken(homeworkId, token);
        Map<String, Object> map = new HashMap<>();
        if (result.containsKey("error")) {
            map.put("code", 400);
            map.put("msg", result.get("error"));
        } else {
            map.put("code", 200);
            map.put("msg", "success");
            map.put("data", result);
        }
        return map;
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestBody StudentHomework req, @RequestParam String token) {
        homeworkService.submitH5HomeworkByToken(req, token);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "Submitted successfully");
        return map;
    }
}
