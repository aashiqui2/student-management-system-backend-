package com.example.sms.controller;

import com.example.sms.dto.DashboardStudentDto;
import com.example.sms.service.DashboardService;
import com.example.sms.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReportService reportService;

    public DashboardController(DashboardService dashboardService, ReportService reportService) {
        this.dashboardService = dashboardService;
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<List<DashboardStudentDto>> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardData());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportDashboard(@RequestParam String format, @RequestParam(required = false) String filter) {
        List<DashboardStudentDto> data = dashboardService.getDashboardData();
        
        if (filter != null && !filter.isEmpty() && !"All".equalsIgnoreCase(filter)) {
            data = data.stream().filter(s -> filter.equalsIgnoreCase(s.getCategory())).collect(Collectors.toList());
        }

        byte[] report;
        String filename;
        MediaType mediaType;
        
        if ("pdf".equalsIgnoreCase(format)) {
            report = reportService.generatePdfReport(data);
            filename = "dashboard_report.pdf";
            mediaType = MediaType.APPLICATION_PDF;
        } else {
            report = reportService.generateExcelReport(data);
            filename = "dashboard_report.xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(report);
    }

    @GetMapping("/category/{level}")
    public ResponseEntity<List<DashboardStudentDto>> getDashboardByCategory(@PathVariable String level) {
        return ResponseEntity.ok(dashboardService.getDashboardDataByCategory(level));
    }
}
