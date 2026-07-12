package com.cm.controller;

import com.cm.common.Result;
import com.cm.config.JwtUserToken;
import com.cm.dto.StudentPaymentSaveDTO;
import com.cm.dto.StudentPaymentVO;
import com.cm.service.StudentPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/student-payment")
public class StudentPaymentController {

    @Autowired
    private StudentPaymentService paymentService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer paymentStatus,
                          @RequestParam(required = false) Long classId) {
        if (!isAdmin()) {
            return Result.fail("无权限操作");
        }
        return Result.ok(paymentService.page(current, size, keyword, paymentStatus, classId));
    }

    @GetMapping("/student/{studentId}")
    public Result<?> listByStudent(@PathVariable Long studentId) {
        if (!isAdmin()) {
            return Result.fail("无权限操作");
        }
        return Result.ok(paymentService.listByStudent(studentId));
    }

    @PutMapping
    public Result<?> save(@RequestBody StudentPaymentSaveDTO dto) {
        if (!isAdmin()) {
            return Result.fail("无权限操作");
        }
        String error = paymentService.save(dto);
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer paymentStatus,
                                         @RequestParam(required = false) Long classId) throws Exception {
        if (!isAdmin()) {
            return ResponseEntity.status(403)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"code\":403,\"msg\":\"无权限操作\"}".getBytes(StandardCharsets.UTF_8));
        }
        List<StudentPaymentVO> all = paymentService.listFiltered(keyword, paymentStatus, classId);
        byte[] data = paymentService.exportCsv(all);
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String filename = "缴费记录_" + date + ".csv";
        String encodedFilename = URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .header("X-Export-Count", String.valueOf(all.size()))
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(data);
    }

    private boolean isAdmin() {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtUserToken) {
            JwtUserToken token = (JwtUserToken) auth;
            return token.getRole() != null && token.getRole() == 1;
        }
        return false;
    }
}
