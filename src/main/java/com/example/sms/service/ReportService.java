package com.example.sms.service;

import com.example.sms.dto.DashboardStudentDto;
import com.example.sms.util.CategoryUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.example.sms.entity.Student;
import com.example.sms.entity.StudentAssessmentMark;

@Service
public class ReportService {

    private final CategoryUtil categoryUtil;

    public ReportService(CategoryUtil categoryUtil) {
        this.categoryUtil = categoryUtil;
    }

    private String resolveCategory(DashboardStudentDto dto) {
        if (dto.getAverageMarks() != null) {
            return categoryUtil.determineCategory(dto.getAverageMarks());
        }
        if (dto.getCategory() != null && !dto.getCategory().isBlank()) {
            return dto.getCategory();
        }
        return "Level 3";
    }

    public byte[] generateExcelReport(List<DashboardStudentDto> data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Dashboard Report");
            
            // Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Name", "RegNo", "Stream", "Specialization", "Email", "Total Marks", "Average Marks", "Category"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            // Data
            int rowIdx = 1;
            for (DashboardStudentDto dto : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getName());
                row.createCell(1).setCellValue(dto.getRegNo());
                String streamSection = dto.getStream() != null ? dto.getStream() : "";
                String specSection = dto.getSpecialization() != null ? dto.getSpecialization() : "";
                row.createCell(2).setCellValue(streamSection);
                row.createCell(3).setCellValue(specSection);
                row.createCell(4).setCellValue(dto.getEmail());
                row.createCell(5).setCellValue(dto.getTotalMarks());
                row.createCell(6).setCellValue(dto.getAverageMarks());
                
                String category = resolveCategory(dto);
                Cell categoryCell = row.createCell(7);
                categoryCell.setCellValue(category);
                
                CellStyle style = workbook.createCellStyle();
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                if ("Level 1".equals(category)) {
                    ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xC6, (byte) 0xEF, (byte) 0xCE}, null));
                } else if ("Level 2".equals(category)) {
                    ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xEB, (byte) 0x9C}, null));
                } else if ("Level 3".equals(category)) {
                    ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xC7, (byte) 0xCE}, null));
                }
                categoryCell.setCellStyle(style);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    public byte[] generateDetailedExcelReport(List<Student> students) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Detailed Student Performance");
            
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle level1Style = workbook.createCellStyle();
            level1Style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            level1Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font whiteFont = workbook.createFont();
            whiteFont.setColor(IndexedColors.WHITE.getIndex());
            level1Style.setFont(whiteFont);

            CellStyle level2Style = workbook.createCellStyle();
            level2Style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            level2Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font blackFont = workbook.createFont();
            blackFont.setColor(IndexedColors.BLACK.getIndex());
            level2Style.setFont(blackFont);

            CellStyle level3Style = workbook.createCellStyle();
            level3Style.setFillForegroundColor(IndexedColors.RED.getIndex());
            level3Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            level3Style.setFont(whiteFont);

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Registration No", "Student Name", "Stream", "Specialization", "Assessment", "Marks", "Percentage", "Level"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            int level1Count = 0;
            int level2Count = 0;
            int level3Count = 0;
            double totalPercentageSum = 0;
            int studentCountWithMarks = 0;
            double highestMarks = 0;
            double lowestMarks = 100;

            for (Student student : students) {
                if (student.getMarks() == null || student.getMarks().isEmpty()) continue;
                
                studentCountWithMarks++;
                double percentage = student.getAverageMarks() != null ? student.getAverageMarks() : 0.0;
                totalPercentageSum += percentage;
                if (percentage > highestMarks) highestMarks = percentage;
                if (percentage < lowestMarks) lowestMarks = percentage;
                
                String level = student.getCategory();
                if ("Level 1".equals(level)) level1Count++;
                else if ("Level 2".equals(level)) level2Count++;
                else level3Count++;
                
                CellStyle rowStyle = null;
                if ("Level 1".equals(level)) rowStyle = level1Style;
                else if ("Level 2".equals(level)) rowStyle = level2Style;
                else rowStyle = level3Style;

                for (StudentAssessmentMark mark : student.getMarks()) {
                    Row row = sheet.createRow(rowIdx++);
                    Cell cell0 = row.createCell(0); cell0.setCellValue(student.getRegNo());
                    Cell cell1 = row.createCell(1); cell1.setCellValue(student.getName());
                    String streamSectionDetail = student.getStream() != null ? student.getStream() : "";
                    String specSectionDetail = student.getSpecialization() != null ? student.getSpecialization() : "";
                    Cell cell2 = row.createCell(2); cell2.setCellValue(streamSectionDetail);
                    Cell cell3 = row.createCell(3); cell3.setCellValue(specSectionDetail);
                    Cell cell4 = row.createCell(4); cell4.setCellValue(mark.getAssessment().getAssessmentName());
                    Cell cell5 = row.createCell(5); cell5.setCellValue(mark.getMarksScored() + " / " + mark.getAssessment().getTotalMarks());
                    double markPercentage = mark.getAssessment().getTotalMarks() == 0 ? 0 : ((double)mark.getMarksScored() / mark.getAssessment().getTotalMarks()) * 100;
                    Cell cell6 = row.createCell(6); cell6.setCellValue(String.format("%.2f%%", markPercentage));
                    Cell cell7 = row.createCell(7); cell7.setCellValue(level);
                    
                    if (rowStyle != null) {
                        cell0.setCellStyle(rowStyle); cell1.setCellStyle(rowStyle); cell2.setCellStyle(rowStyle);
                        cell3.setCellStyle(rowStyle); cell4.setCellStyle(rowStyle); cell5.setCellStyle(rowStyle);
                        cell6.setCellStyle(rowStyle); cell7.setCellStyle(rowStyle);
                    }
                }
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            rowIdx += 2;
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Summary Report:");
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Total Students: " + students.size());
            
            double averageOverall = studentCountWithMarks == 0 ? 0 : totalPercentageSum / studentCountWithMarks;
            if (lowestMarks == 100 && studentCountWithMarks == 0) lowestMarks = 0;
            
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Average Marks (%): " + String.format("%.2f", averageOverall));
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Highest Marks (%): " + String.format("%.2f", highestMarks));
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Lowest Marks (%): " + String.format("%.2f", lowestMarks));
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Number of Level 1 Students: " + level1Count);
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Number of Level 2 Students: " + level2Count);
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Number of Level 3 Students: " + level3Count);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate detailed Excel report", e);
        }
    }

    public byte[] generatePdfReport(List<DashboardStudentDto> data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Student Performance Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);
            
            Paragraph date = new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            
            String[] headers = {"Name", "RegNo", "Stream", "Specialization", "Email", "Total", "Average", "Category"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
                table.addCell(cell);
            }

            for (DashboardStudentDto dto : data) {
                table.addCell(dto.getName());
                table.addCell(dto.getRegNo());
                String streamSectionPdf = dto.getStream() != null ? dto.getStream() : "";
                String specSectionPdf = dto.getSpecialization() != null ? dto.getSpecialization() : "";
                table.addCell(streamSectionPdf);
                table.addCell(specSectionPdf);
                table.addCell(dto.getEmail());
                table.addCell(String.valueOf(dto.getTotalMarks()));
                table.addCell(String.format("%.2f", dto.getAverageMarks()));
                table.addCell(dto.getCategory());
            }

            document.add(table);
            document.close();
            
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }
}
