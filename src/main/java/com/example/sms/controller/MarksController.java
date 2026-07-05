package com.example.sms.controller;

import com.example.sms.entity.StudentAssessmentMark;
import com.example.sms.service.ExcelService;
import com.example.sms.service.MarksService;
import com.example.sms.service.StudentService;
import com.example.sms.entity.Student;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marks")
public class MarksController {

    private final MarksService marksService;
    private final ExcelService excelService;
    private final StudentService studentService;

    public MarksController(MarksService marksService, ExcelService excelService, StudentService studentService) {
        this.marksService = marksService;
        this.excelService = excelService;
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<StudentAssessmentMark> assignMarks(
            @RequestParam Long studentId,
            @RequestParam Long assessmentId,
            @RequestParam Integer marks) {
        return ResponseEntity.ok(marksService.assignMarks(studentId, assessmentId, marks));
    }

    @GetMapping
    public ResponseEntity<List<StudentAssessmentMark>> getAllMarks() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
            try {
                Student me = studentService.getStudentByUsername(auth.getName());
                return ResponseEntity.ok(marksService.getMarksForStudent(me.getId()));
            } catch (Exception e) {
                return ResponseEntity.ok(List.of());
            }
        }
        return ResponseEntity.ok(marksService.getAllMarks());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMark(
            @RequestParam Long studentId,
            @RequestParam Long assessmentId) {
        marksService.deleteMark(studentId, assessmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<StudentAssessmentMark>> getMarksForStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(marksService.getMarksForStudent(studentId));
    }

    @GetMapping("/assessment/{assessmentId}")
    public ResponseEntity<List<StudentAssessmentMark>> getMarksForAssessment(@PathVariable Long assessmentId) {
        return ResponseEntity.ok(marksService.getMarksForAssessment(assessmentId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentAssessmentMark> updateMark(@PathVariable Long id, @RequestParam Integer marksScored) {
        return ResponseEntity.ok(marksService.updateMark(id, marksScored));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMarksExcel(@RequestParam("file") MultipartFile file, @RequestParam("assessmentId") Long assessmentId) {
        try {
            Map<String, Object> result = excelService.processMarksExcel(file, assessmentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to upload marks: " + e.getMessage());
        }
    }
}
