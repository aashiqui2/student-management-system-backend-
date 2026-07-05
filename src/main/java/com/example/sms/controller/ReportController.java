package com.example.sms.controller;

import com.example.sms.service.ReportService;
import com.example.sms.repository.StudentRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final StudentRepository studentRepository;

    public ReportController(ReportService reportService, StudentRepository studentRepository) {
        this.reportService = reportService;
        this.studentRepository = studentRepository;
    }

    @GetMapping("/detailed-excel")
    public ResponseEntity<byte[]> getDetailedExcelReport() {
        byte[] data = reportService.generateDetailedExcelReport(studentRepository.findAll());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Detailed_Student_Report.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
