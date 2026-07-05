package com.example.sms.service;

import com.example.sms.entity.Assessment;
import com.example.sms.entity.Student;
import com.example.sms.entity.StudentAssessmentMark;
import com.example.sms.repository.AssessmentRepository;
import com.example.sms.repository.StudentRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelServiceTest {

    @Test
    void processMarksExcelShouldImportMatchingRegNoMarks() throws IOException {
        Assessment assessment = new Assessment();
        assessment.setId(12L);
        assessment.setAssessmentName("Test Assessment");
        assessment.setTotalMarks(100);

        Student student = new Student();
        student.setId(7L);
        student.setRegNo("410623243020");

        StudentRepository studentRepository = (StudentRepository) Proxy.newProxyInstance(
                StudentRepository.class.getClassLoader(),
                new Class[]{StudentRepository.class},
                new StudentRepositoryHandler(student)
        );

        AssessmentRepository assessmentRepository = (AssessmentRepository) Proxy.newProxyInstance(
                AssessmentRepository.class.getClassLoader(),
                new Class[]{AssessmentRepository.class},
                new AssessmentRepositoryHandler(assessment)
        );

        TestStudentService studentService = new TestStudentService();

        final long[] savedStudentId = {0L};
        final long[] savedAssessmentId = {0L};
        final int[] savedMarks = {0};

        com.example.sms.repository.StudentAssessmentMarkRepository markRepository = (com.example.sms.repository.StudentAssessmentMarkRepository) Proxy.newProxyInstance(
                com.example.sms.repository.StudentAssessmentMarkRepository.class.getClassLoader(),
                new Class[]{com.example.sms.repository.StudentAssessmentMarkRepository.class},
                (proxy, method, args1) -> {
                    if (method.getName().equals("findByStudentIdAndAssessmentId")) {
                        return Optional.empty();
                    }
                    if (method.getName().equals("save")) {
                        StudentAssessmentMark mark = (StudentAssessmentMark) args1[0];
                        savedStudentId[0] = mark.getStudent().getId();
                        savedAssessmentId[0] = mark.getAssessment().getId();
                        savedMarks[0] = mark.getMarksScored();
                        return mark;
                    }
                    return null;
                }
        );

        TestMarksService marksService = new TestMarksService();
        ExcelService excelService = new ExcelService(studentRepository, assessmentRepository, marksService, studentService, markRepository);

        MultipartFile file = new MockMultipartFile(
                "file",
                "marks.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createMarksWorkbook()
        );

        Map<String, Object> result = excelService.processMarksExcel(file, 12L);

        assertEquals(1, result.get("marksInserted"));
        assertEquals(0, result.get("failedCount"));
        assertEquals(7L, savedStudentId[0]);
        assertEquals(12L, savedAssessmentId[0]);
        assertEquals(10, savedMarks[0]);
    }

    @Test
    void processStudentsExcelShouldImportPursuingYearFromHeaders() throws IOException {
        List<Student> savedStudents = new ArrayList<>();

        StudentRepository studentRepository = (StudentRepository) Proxy.newProxyInstance(
                StudentRepository.class.getClassLoader(),
                new Class[]{StudentRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findAll")) {
                        return java.util.List.of();
                    }
                    if (method.getName().equals("findByRegNoIgnoreCase") || method.getName().equals("findByRegNo")) {
                        return Optional.empty();
                    }
                    if (method.getName().equals("saveAll")) {
                        List<Student> entities = new ArrayList<>();
                        ((Iterable<Student>) args[0]).forEach(entities::add);
                        savedStudents.addAll(entities);
                        return entities;
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        AssessmentRepository assessmentRepository = (AssessmentRepository) Proxy.newProxyInstance(
                AssessmentRepository.class.getClassLoader(),
                new Class[]{AssessmentRepository.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );

        TestStudentService studentService = new TestStudentService();
        TestMarksService marksService = new TestMarksService();
        com.example.sms.repository.StudentAssessmentMarkRepository markRepository = (com.example.sms.repository.StudentAssessmentMarkRepository) Proxy.newProxyInstance(
                com.example.sms.repository.StudentAssessmentMarkRepository.class.getClassLoader(),
                new Class[]{com.example.sms.repository.StudentAssessmentMarkRepository.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );

        ExcelService excelService = new ExcelService(studentRepository, assessmentRepository, marksService, studentService, markRepository);

        MultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createStudentsWorkbook()
        );

        Map<String, Object> result = excelService.processStudentsExcel(file, null);

        assertEquals(1, result.get("insertedCount"));
        assertEquals(0, result.get("failedCount"));
        assertEquals(1, savedStudents.size());
        assertEquals("CSE", savedStudents.get(0).getStream());
    }

    private byte[] createMarksWorkbook() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Marks");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("regNo");
            header.createCell(1).setCellValue("marks");

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(410623243020L);
            dataRow.createCell(1).setCellValue(10);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createStudentsWorkbook() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("name");
            header.createCell(1).setCellValue("regNo");
            header.createCell(2).setCellValue("email");
            header.createCell(3).setCellValue("mobileNumber");
            header.createCell(4).setCellValue("stream");
            header.createCell(5).setCellValue("pursuingyear");
            header.createCell(6).setCellValue("hackerRankUsername");

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("Jane Doe");
            dataRow.createCell(1).setCellValue("21CSE100");
            dataRow.createCell(2).setCellValue("jane@example.com");
            dataRow.createCell(3).setCellValue("9876543210");
            dataRow.createCell(4).setCellValue("CSE");
            dataRow.createCell(5).setCellValue("Fourth Year");
            dataRow.createCell(6).setCellValue("jane_d");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static class TestMarksService extends MarksService {
        private Long studentId;
        private Long assessmentId;
        private Integer marks;

        private TestMarksService() {
            super(null, null, null);
        }

        @Override
        public StudentAssessmentMark assignMarks(Long studentId, Long assessmentId, Integer marks) {
            this.studentId = studentId;
            this.assessmentId = assessmentId;
            this.marks = marks;
            return new StudentAssessmentMark();
        }
    }

    private static class TestStudentService extends com.example.sms.service.StudentService {
        private TestStudentService() {
            super(null, null, null, null, null);
        }

        @Override
        public void recalculatePerformanceForStudents(Iterable<Long> studentIds) {
            // Do nothing
        }
    }

    private static class StudentRepositoryHandler implements InvocationHandler {
        private final Student student;

        private StudentRepositoryHandler(Student student) {
            this.student = student;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("findByRegNoIgnoreCase") || method.getName().equals("findByRegNo")) {
                return Optional.of(student);
            }
            if (method.getName().equals("findAll")) {
                return java.util.List.of(student);
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static class AssessmentRepositoryHandler implements InvocationHandler {
        private final Assessment assessment;

        private AssessmentRepositoryHandler(Assessment assessment) {
            this.assessment = assessment;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("findById") && args != null && args.length > 0) {
                return Optional.of(assessment);
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static Object defaultValue(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            if (clazz == boolean.class) return false;
            if (clazz == byte.class) return (byte) 0;
            if (clazz == short.class) return (short) 0;
            if (clazz == int.class) return 0;
            if (clazz == long.class) return 0L;
            if (clazz == float.class) return 0f;
            if (clazz == double.class) return 0d;
            if (clazz == char.class) return '\0';
        }
        return null;
    }
}
