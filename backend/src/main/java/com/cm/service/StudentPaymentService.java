package com.cm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cm.dto.StudentPaymentSaveDTO;
import com.cm.dto.StudentPaymentVO;
import com.cm.entity.ClassInfo;
import com.cm.entity.Student;
import com.cm.entity.StudentClass;
import com.cm.entity.StudentClassPayment;
import com.cm.mapper.ClassInfoMapper;
import com.cm.mapper.StudentClassMapper;
import com.cm.mapper.StudentClassPaymentMapper;
import com.cm.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentPaymentService {

    public static final int STATUS_SURPLUS = 1;
    public static final int STATUS_SETTLED = 2;
    public static final int STATUS_ARREARS = 3;

    @Autowired
    private StudentClassMapper studentClassMapper;
    @Autowired
    private StudentClassPaymentMapper paymentMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private ClassInfoMapper classInfoMapper;

    public Page<StudentPaymentVO> page(long current, long size, String keyword, Integer paymentStatus, Long classId) {
        LambdaQueryWrapper<StudentClass> scWrapper = new LambdaQueryWrapper<>();
        if (classId != null) {
            scWrapper.eq(StudentClass::getClassId, classId);
        }
        scWrapper.orderByDesc(StudentClass::getJoinTime);

        List<StudentClass> scList = studentClassMapper.selectList(scWrapper);
        if (scList.isEmpty()) {
            return emptyPage(current, size);
        }

        List<StudentPaymentVO> all = buildVOList(scList);

        if (keyword != null && !keyword.isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            all = all.stream()
                    .filter(v -> (v.getStudentName() != null && v.getStudentName().toLowerCase().contains(kw))
                            || (v.getStudentNo() != null && v.getStudentNo().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }
        if (paymentStatus != null) {
            all = all.stream()
                    .filter(v -> paymentStatus.equals(v.getPaymentStatus()))
                    .collect(Collectors.toList());
        }

        return paginate(all, current, size);
    }

    public List<StudentPaymentVO> listByStudent(Long studentId) {
        List<StudentClass> scList = studentClassMapper.selectList(
                new LambdaQueryWrapper<StudentClass>()
                        .eq(StudentClass::getStudentId, studentId)
                        .orderByDesc(StudentClass::getJoinTime));
        if (scList.isEmpty()) {
            return Collections.emptyList();
        }
        return buildVOList(scList);
    }

    public String save(StudentPaymentSaveDTO dto) {
        if (dto.getStudentClassId() == null) {
            return "请选择班级记录";
        }
        StudentClass sc = studentClassMapper.selectById(dto.getStudentClassId());
        if (sc == null) {
            return "班级关联记录不存在";
        }

        BigDecimal amountDue = dto.getAmountDue() != null ? dto.getAmountDue() : BigDecimal.ZERO;
        BigDecimal amountReceived = dto.getAmountReceived() != null ? dto.getAmountReceived() : BigDecimal.ZERO;
        if (amountDue.compareTo(BigDecimal.ZERO) < 0 || amountReceived.compareTo(BigDecimal.ZERO) < 0) {
            return "金额不能为负数";
        }

        StudentClassPayment existing = paymentMapper.selectOne(
                new LambdaQueryWrapper<StudentClassPayment>()
                        .eq(StudentClassPayment::getStudentClassId, dto.getStudentClassId()));

        if (existing != null) {
            existing.setAmountDue(amountDue);
            existing.setAmountReceived(amountReceived);
            paymentMapper.updateById(existing);
        } else {
            StudentClassPayment payment = new StudentClassPayment();
            payment.setStudentClassId(dto.getStudentClassId());
            payment.setAmountDue(amountDue);
            payment.setAmountReceived(amountReceived);
            paymentMapper.insert(payment);
        }
        return null;
    }

    private List<StudentPaymentVO> buildVOList(List<StudentClass> scList) {
        List<Long> studentIds = scList.stream().map(StudentClass::getStudentId).distinct().collect(Collectors.toList());
        List<Long> classIds = scList.stream().map(StudentClass::getClassId).distinct().collect(Collectors.toList());
        List<Long> scIds = scList.stream().map(StudentClass::getId).collect(Collectors.toList());

        Map<Long, Student> studentMap = studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, ClassInfo> classMap = classInfoMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(ClassInfo::getId, c -> c));
        Map<Long, StudentClassPayment> paymentMap = paymentMapper.selectList(
                new LambdaQueryWrapper<StudentClassPayment>()
                        .in(StudentClassPayment::getStudentClassId, scIds))
                .stream()
                .collect(Collectors.toMap(StudentClassPayment::getStudentClassId, p -> p));

        List<StudentPaymentVO> result = new ArrayList<>();
        for (StudentClass sc : scList) {
            StudentPaymentVO vo = new StudentPaymentVO();
            vo.setStudentClassId(sc.getId());
            vo.setStudentId(sc.getStudentId());
            vo.setClassId(sc.getClassId());
            vo.setEnrollmentStatus(sc.getStatus());

            Student student = studentMap.get(sc.getStudentId());
            if (student != null) {
                vo.setStudentNo(student.getStudentNo());
                vo.setStudentName(student.getName());
            }
            ClassInfo classInfo = classMap.get(sc.getClassId());
            if (classInfo != null) {
                vo.setClassCode(classInfo.getClassCode());
                vo.setClassName(classInfo.getClassName());
            }

            StudentClassPayment payment = paymentMap.get(sc.getId());
            BigDecimal amountDue = payment != null ? payment.getAmountDue() : BigDecimal.ZERO;
            BigDecimal amountReceived = payment != null ? payment.getAmountReceived() : BigDecimal.ZERO;
            vo.setAmountDue(amountDue);
            vo.setAmountReceived(amountReceived);

            BigDecimal balance = calcBalance(amountReceived, amountDue);
            vo.setBalance(balance);
            vo.setPaymentStatus(calcStatus(balance));

            result.add(vo);
        }
        return result;
    }

    public static BigDecimal calcBalance(BigDecimal amountReceived, BigDecimal amountDue) {
        BigDecimal received = amountReceived != null ? amountReceived : BigDecimal.ZERO;
        BigDecimal due = amountDue != null ? amountDue : BigDecimal.ZERO;
        return received.subtract(due);
    }

    public static int calcStatus(BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            return STATUS_SURPLUS;
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            return STATUS_ARREARS;
        }
        return STATUS_SETTLED;
    }

    private Page<StudentPaymentVO> emptyPage(long current, long size) {
        Page<StudentPaymentVO> page = new Page<>(current, size);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        return page;
    }

    private Page<StudentPaymentVO> paginate(List<StudentPaymentVO> all, long current, long size) {
        Page<StudentPaymentVO> page = new Page<>(current, size);
        page.setTotal(all.size());
        int from = (int) ((current - 1) * size);
        if (from >= all.size()) {
            page.setRecords(Collections.emptyList());
            return page;
        }
        int to = (int) Math.min(from + size, all.size());
        page.setRecords(all.subList(from, to));
        return page;
    }
}
