package com.example.sms.service;

import com.example.sms.dto.DashboardStudentDto;
import com.example.sms.util.CategoryUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReportServiceTest {

    @Test
    void generateExcelReportShouldUseAverageMarksToSetCategoryAndColor() throws Exception {
        CategoryUtil categoryUtil = new CategoryUtil();
        ReflectionTestUtils.setField(categoryUtil, "level1Min", 75.0);
        ReflectionTestUtils.setField(categoryUtil, "level2Min", 50.0);

        ReportService reportService = new ReportService(categoryUtil);

        DashboardStudentDto dto = new DashboardStudentDto();
        dto.setName("Alice");
        dto.setRegNo("REG001");
        dto.setStream("CS");
        dto.setSpecialization("Core");
        dto.setEmail("alice@example.com");
        dto.setMobileNumber("1234567890");
        dto.setTotalMarks(100);
        dto.setAverageMarks(80.0);
        dto.setCategory("Level 3");

        byte[] excelBytes = reportService.generateExcelReport(List.of(dto));
        assertNotNull(excelBytes);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(1);
            Cell categoryCell = dataRow.getCell(6);
            assertEquals("Level 1", categoryCell.getStringCellValue());
        }
    }
}
