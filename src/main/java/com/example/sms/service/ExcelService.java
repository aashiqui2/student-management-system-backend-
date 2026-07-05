package com.example.sms.service;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.sms.entity.Assessment;
import com.example.sms.entity.Student;
import com.example.sms.entity.User;
import com.example.sms.enums.Role;
import com.example.sms.repository.StudentRepository;
import com.example.sms.repository.UserRepository;
import com.example.sms.repository.AssessmentRepository;

@Service
public class ExcelService {

    private static final Logger log = LoggerFactory.getLogger(ExcelService.class);

    private final StudentRepository studentRepository;
    private final AssessmentRepository assessmentRepository;
    private final MarksService marksService;
    private final StudentService studentService;
    private final com.example.sms.repository.StudentAssessmentMarkRepository markRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ExcelService(StudentRepository studentRepository, AssessmentRepository assessmentRepository, MarksService marksService, StudentService studentService, com.example.sms.repository.StudentAssessmentMarkRepository markRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.assessmentRepository = assessmentRepository;
        this.marksService = marksService;
        this.studentService = studentService;
        this.markRepository = markRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private String normalizeRegNo(String value) {
        return value == null ? "" : value.trim().replaceAll("[^a-zA-Z0-9]", "");
    }

    private String normalizeCellValue(Cell cell) {
        return getCellValueAsString(cell).trim();
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "");
    }

    private Map<String, Integer> readHeaderIndexes(Row headerRow) {
        Map<String, Integer> indexes = new HashMap<>();
        if (headerRow == null) return indexes;

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;
            String header = normalizeHeader(getCellValueAsString(cell));
            if (!header.isEmpty()) {
                indexes.put(header, i);
            }
        }
        return indexes;
    }

    private String getCellValue(Row row, Map<String, Integer> headerIndexes, int fallbackIndex, String... aliases) {
        if (row == null) return "";

        for (String alias : aliases) {
            Integer index = headerIndexes.get(normalizeHeader(alias));
            if (index != null && index >= 0) {
                return normalizeCellValue(row.getCell(index));
            }
        }

        if (fallbackIndex >= 0) {
            return normalizeCellValue(row.getCell(fallbackIndex));
        }
        return "";
    }

    private Student mergeStudentFields(Student existing, String name, String email, String mobileNumber, String stream, String specialization, String hackerRank) {
        if (name != null && !name.isEmpty()) existing.setName(name);
        if (email != null && !email.isEmpty()) existing.setEmail(email);
        if (mobileNumber != null && !mobileNumber.isEmpty()) existing.setMobileNumber(mobileNumber);
        if (stream != null && !stream.isEmpty()) existing.setStream(stream);
        if (specialization != null && !specialization.isEmpty()) existing.setSpecialization(specialization);
        if (!hackerRank.isEmpty()) existing.setHackerRankUsername(hackerRank);
        return existing;
    }

    @Transactional
    public Map<String, Object> processStudentsExcel(MultipartFile file, com.example.sms.entity.StaffProfile assignedStaff) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        int insertedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            Map<String, Student> existingStudents = new HashMap<>();
            studentRepository.findAll().forEach(student -> existingStudents.put(normalizeRegNo(student.getRegNo()), student));
            Set<String> seenRegNos = new HashSet<>();

            Row headerRow = null;
            List<Row> dataRows = new ArrayList<>();
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (headerRow == null) {
                    headerRow = currentRow;
                } else {
                    dataRows.add(currentRow);
                }
            }

            Map<String, Integer> headerIndexes = readHeaderIndexes(headerRow);

            int rowNumber = 1;
            for (Row currentRow : dataRows) {
                rowNumber++;

                String regNo = getCellValue(currentRow, headerIndexes, 1, "regNo", "reg no", "registration number", "register number");
                String name = getCellValue(currentRow, headerIndexes, 0, "name", "full name", "student name");
                String email = getCellValue(currentRow, headerIndexes, 2, "email", "email address");
                String mobileNumber = getCellValue(currentRow, headerIndexes, 3, "mobileNumber", "mobile number", "mobile", "phone");
                String stream = getCellValue(currentRow, headerIndexes, 4, "stream", "dept", "department");
                String specialization = getCellValue(currentRow, headerIndexes, 5, "specialization", "spec", "branch");
                // Skipping pursuing year extraction as it's no longer a stored field
                String hackerRank = getCellValue(currentRow, headerIndexes, 6, "hackerRankUsername", "hackerrank username", "hackerrank");
                String startYearStr = getCellValue(currentRow, headerIndexes, -1, "startYear", "start year");
                String courseDurationStr = getCellValue(currentRow, headerIndexes, -1, "courseDuration", "course duration", "duration");

                if (regNo.isEmpty() && name.isEmpty() && email.isEmpty()) {
                    skippedCount++;
                    continue;
                }

                if (regNo.isEmpty()) {
                    errors.add(Map.of("row", rowNumber, "reason", "Missing Register Number"));
                    skippedCount++;
                    continue;
                }

                String normalized = normalizeRegNo(regNo);
                if (normalized.isEmpty()) {
                    errors.add(Map.of("row", rowNumber, "reason", "Invalid Register Number"));
                    skippedCount++;
                    continue;
                }

                if (seenRegNos.contains(normalized)) {
                    errors.add(Map.of("row", rowNumber, "reason", "Duplicate Register Number in Excel: " + regNo));
                    skippedCount++;
                    continue;
                }
                seenRegNos.add(normalized);

                if (name.isEmpty()) {
                    errors.add(Map.of("row", rowNumber, "reason", "Missing student name"));
                    skippedCount++;
                    continue;
                }

                // No pursuing year parsing needed

                if (email.isEmpty()) {
                    email = normalized + "@example.com";
                }

                Optional<Student> existingOpt = Optional.ofNullable(existingStudents.get(normalized));
                
                Integer startYear = null;
                Integer courseDuration = null;
                try { if (!startYearStr.isEmpty()) startYear = Integer.parseInt(startYearStr); } catch (NumberFormatException ignored) {}
                try { if (!courseDurationStr.isEmpty()) courseDuration = Integer.parseInt(courseDurationStr); } catch (NumberFormatException ignored) {}
                
                if (existingOpt.isPresent()) {
                    Student student = existingOpt.get();
                    mergeStudentFields(student, name, email, mobileNumber, stream, specialization, hackerRank);
                    if (startYear != null) student.setStartYear(startYear);
                    if (courseDuration != null) student.setCourseDuration(courseDuration);
                    if (assignedStaff != null) {
                        student.setAssignedStaff(assignedStaff);
                    }
                    updatedCount++;
                } else {
                    Student newStudent = new Student();
                    newStudent.setName(name);
                    newStudent.setRegNo(regNo);
                    newStudent.setEmail(email);
                    newStudent.setMobileNumber(mobileNumber);
                    newStudent.setStream(stream);
                    newStudent.setSpecialization(specialization);
                    newStudent.setHackerRankUsername(hackerRank);
                    if (startYear != null) newStudent.setStartYear(startYear);
                    if (courseDuration != null) newStudent.setCourseDuration(courseDuration);
                    if (assignedStaff != null) {
                        newStudent.setAssignedStaff(assignedStaff);
                    }
                    
                    // Create User entity with default password
                    Optional<User> userOpt = userRepository.findByUsername(regNo);
                    if (userOpt.isPresent()) {
                        newStudent.setUser(userOpt.get());
                    } else {
                        User newUser = new User();
                        newUser.setUsername(regNo);
                        newUser.setPassword(passwordEncoder.encode("student123"));
                        newUser.setRole(Role.STUDENT);
                        userRepository.save(newUser);
                        newStudent.setUser(newUser);
                    }
                    
                    existingStudents.put(normalized, newStudent);
                    insertedCount++;
                }
            }

            studentRepository.saveAll(existingStudents.values());

            log.info("Student Excel import completed: inserted={}, updated={}, skipped={}, errors={}", insertedCount, updatedCount, skippedCount, errors.size());
        } catch (Exception e) {
            log.error("Student Excel import failed", e);
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }

        response.put("insertedCount", insertedCount);
        response.put("updatedCount", updatedCount);
        response.put("skippedCount", skippedCount);
        response.put("failedCount", errors.size());
        response.put("errors", errors);
        return response;
    }

    private Optional<Student> findStudentByRegNo(String regNo) {
        if (regNo == null || regNo.trim().isEmpty()) {
            return Optional.empty();
        }

        String trimmed = regNo.trim();
        Optional<Student> studentOpt = studentRepository.findByRegNoIgnoreCase(trimmed);
        if (studentOpt.isPresent()) {
            return studentOpt;
        }

        String cleaned = normalizeRegNo(trimmed);
        if (!cleaned.isEmpty() && !cleaned.equals(trimmed)) {
            studentOpt = studentRepository.findByRegNoIgnoreCase(cleaned);
            if (studentOpt.isPresent()) {
                return studentOpt;
            }
        }

        return studentRepository.findByRegNo(trimmed);
    }

    private int parseMarksValue(String value, Cell cell) {
        if (value == null || value.trim().isEmpty()) {
            throw new NumberFormatException("Empty marks value");
        }

        String trimmed = value.trim();
        try {
            if (trimmed.contains(".")) {
                return (int) Math.round(Double.parseDouble(trimmed));
            }
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                return (int) Math.round(cell.getNumericCellValue());
            }
            throw e;
        }
    }

    @Transactional
    public Map<String, Object> processMarksExcel(MultipartFile file, Long assessmentId) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        int marksInserted = 0;
        int marksUpdated = 0;
        int skippedCount = 0;

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            Assessment assessment = assessmentRepository.findById(assessmentId)
                    .orElseThrow(() -> new RuntimeException("Assessment not found"));

            Map<String, Student> studentMap = new HashMap<>();
            studentRepository.findAll().forEach(student -> studentMap.put(normalizeRegNo(student.getRegNo()), student));
            Set<String> seenRegNos = new HashSet<>();
            List<String> skippedRegNos = new ArrayList<>();
            Set<Long> affectedStudentIds = new HashSet<>();

            int regNoCol = 0;
            int marksCol = 1;
            int rowNumber = 1;

            if (rows.hasNext()) {
                Row headerRow = rows.next();
                rowNumber++;
                for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                    String h = getCellValueAsString(headerRow.getCell(c)).toLowerCase();
                    if (h.isEmpty()) continue;
                    if (h.contains("reg") || h.contains("regno") || h.contains("registration")) {
                        regNoCol = c;
                    }
                    if (h.contains("mark") || h.contains("score") || h.contains("marks")) {
                        marksCol = c;
                    }
                }
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                rowNumber++;

                String regNo = normalizeCellValue(currentRow.getCell(regNoCol));
                Cell marksCell = currentRow.getCell(marksCol);
                String marksStr = getCellValueAsString(marksCell).trim();

                if (regNo.isEmpty() && marksStr.isEmpty()) {
                    skippedCount++;
                    continue;
                }

                if (regNo.isEmpty()) {
                    errors.add(Map.of("row", rowNumber, "reason", "Missing Register Number"));
                    skippedCount++;
                    continue;
                }

                String normalized = normalizeRegNo(regNo);
                if (normalized.isEmpty()) {
                    errors.add(Map.of("row", rowNumber, "reason", "Invalid Register Number: " + regNo));
                    skippedCount++;
                    continue;
                }

                if (seenRegNos.contains(normalized)) {
                    errors.add(Map.of("row", rowNumber, "reason", "Duplicate Register Number in Excel: " + regNo));
                    skippedCount++;
                    continue;
                }
                seenRegNos.add(normalized);

                if (marksStr.isEmpty()) {
                    errors.add(Map.of("row", rowNumber, "reason", "Missing marks value for RegNo " + regNo));
                    skippedCount++;
                    continue;
                }

                int marks;
                try {
                    marks = parseMarksValue(marksStr, marksCell);
                } catch (NumberFormatException e) {
                    errors.add(Map.of("row", rowNumber, "reason", "Invalid marks value for RegNo " + regNo + ": " + marksStr));
                    skippedCount++;
                    continue;
                }

                Student student = studentMap.get(normalized);
                if (student == null) {
                    errors.add(Map.of("row", rowNumber, "reason", "Unknown Register Number: " + regNo));
                    skippedRegNos.add(regNo);
                    skippedCount++;
                    continue;
                }

                try {
                    Optional<com.example.sms.entity.StudentAssessmentMark> existingMarkOpt = markRepository.findByStudentIdAndAssessmentId(student.getId(), assessment.getId());
                    if (existingMarkOpt.isPresent()) {
                        com.example.sms.entity.StudentAssessmentMark markToUpdate = existingMarkOpt.get();
                        markToUpdate.setMarksScored(marks);
                        markRepository.save(markToUpdate);
                        marksUpdated++;
                    } else {
                        com.example.sms.entity.StudentAssessmentMark newMark = new com.example.sms.entity.StudentAssessmentMark();
                        newMark.setStudent(student);
                        newMark.setAssessment(assessment);
                        newMark.setMarksScored(marks);
                        markRepository.save(newMark);
                        marksInserted++;
                    }
                    affectedStudentIds.add(student.getId());
                } catch (Exception e) {
                    errors.add(Map.of("row", rowNumber, "reason", "Failed to assign marks for RegNo " + regNo + ": " + e.getMessage()));
                    skippedCount++;
                }
            }

            studentService.recalculatePerformanceForStudents(affectedStudentIds);

            response.put("skippedRegNos", skippedRegNos);
            log.info("Marks Excel import completed for assessment {}: inserted={}, updated={}, skipped={}, errors={}", assessment.getAssessmentName(), marksInserted, marksUpdated, skippedCount, errors.size());
        } catch (Exception e) {
            log.error("Marks Excel import failed", e);
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }

        response.put("marksInserted", marksInserted);
        response.put("marksUpdated", marksUpdated);
        response.put("skippedCount", skippedCount);
        response.put("failedCount", errors.size());
        response.put("errors", errors);
        return response;
    }
}
