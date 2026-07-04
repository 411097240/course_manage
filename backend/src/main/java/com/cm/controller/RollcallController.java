package com.cm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cm.entity.AttendanceDetail;
import com.cm.entity.AttendanceRecord;

import com.cm.service.RollcallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rollcall")
public class RollcallController {

    @Autowired
    private RollcallService rollcallService;

    @GetMapping("/today-courses")
    public Map<String, Object> todayCourses() {
        List<Map<String, Object>> result = rollcallService.listTodayCourses();
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        map.put("data", result);
        return map;
    }

    @GetMapping("/students")
    public Map<String, Object> getStudents(
            @RequestParam Long classId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long courseId) {
        LocalDate rollcallDate = date != null && !date.isEmpty() ? LocalDate.parse(date) : LocalDate.now();
        Map<String, Object> result = rollcallService.getRollcallFormInfo(classId, rollcallDate, courseId);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        map.put("data", result);
        return map;
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestBody SubmitRequest req) {
        rollcallService.submitRollcall(req.getRecord(), req.getDetails());
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "点名成功");
        return map;
    }

    @GetMapping("/page")
    public Map<String, Object> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long classId) {
        Page<Map<String, Object>> result = rollcallService.getRecords(classId, current, size);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        map.put("data", result);
        return map;
    }

    @GetMapping("/detail")
    public Map<String, Object> detail(@RequestParam Long recordId) {
        List<Map<String, Object>> result = rollcallService.getDetails(recordId);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        map.put("data", result);
        return map;
    }

    public static class SubmitRequest {
        private AttendanceRecord record;
        private List<AttendanceDetail> details;

        public AttendanceRecord getRecord() { return record; }
        public void setRecord(AttendanceRecord record) { this.record = record; }
        public List<AttendanceDetail> getDetails() { return details; }
        public void setDetails(List<AttendanceDetail> details) { this.details = details; }
    }
}
