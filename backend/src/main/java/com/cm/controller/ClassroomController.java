package com.cm.controller;

import com.cm.common.Result;
import com.cm.config.JwtUserToken;
import com.cm.dto.ManualReservationDTO;
import com.cm.entity.Classroom;
import com.cm.service.ClassroomReservationService;
import com.cm.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/classroom")
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private ClassroomReservationService reservationService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(classroomService.page(current, size, keyword));
    }

    @GetMapping("/all")
    public Result<?> listAll() {
        return Result.ok(classroomService.listAllEnabled());
    }

    @GetMapping("/{id}/reservations")
    public Result<?> reservations(@PathVariable Long id,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(classroomService.listReservations(id, startDate, endDate));
    }

    @PostMapping("/reservation")
    public Result<?> saveReservation(@RequestBody ManualReservationDTO dto) {
        try {
            reservationService.saveManual(dto);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/reservation")
    public Result<?> updateReservation(@RequestBody ManualReservationDTO dto) {
        try {
            reservationService.updateManual(dto);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/reservation/{id}")
    public Result<?> deleteReservation(@PathVariable Long id) {
        try {
            reservationService.deleteManual(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping
    public Result<?> save(@RequestBody Classroom classroom) {
        if (!isAdmin()) {
            return Result.fail("无权限操作");
        }
        String error = classroomService.save(classroom);
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody Classroom classroom) {
        if (!isAdmin()) {
            return Result.fail("无权限操作");
        }
        String error = classroomService.update(classroom);
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (!isAdmin()) {
            return Result.fail("无权限操作");
        }
        classroomService.deleteById(id);
        return Result.ok();
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
