package com.example.sms.service;

import com.example.sms.entity.Assessment;
import com.example.sms.entity.Student;
import com.example.sms.entity.StudentAssessmentMark;
import com.example.sms.repository.StudentAssessmentMarkRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MarksService {

    private final StudentAssessmentMarkRepository markRepository;
    private final StudentService studentService;
    private final AssessmentService assessmentService;

    public MarksService(StudentAssessmentMarkRepository markRepository, StudentService studentService, AssessmentService assessmentService) {
        this.markRepository = markRepository;
        this.studentService = studentService;
        this.assessmentService = assessmentService;
    }

    public StudentAssessmentMark assignMarks(Long studentId, Long assessmentId, Integer marks) {
        Student student = studentService.getStudentById(studentId);
        Assessment assessment = assessmentService.getAssessmentById(assessmentId);

        Optional<StudentAssessmentMark> existingMark = markRepository.findByStudentIdAndAssessmentId(studentId, assessmentId);
        if (existingMark.isPresent()) {
            StudentAssessmentMark markToUpdate = existingMark.get();
            markToUpdate.setMarksScored(marks);
            StudentAssessmentMark savedMark = markRepository.save(markToUpdate);
            studentService.recalculateStudentPerformance(studentId);
            return savedMark;
        }

        StudentAssessmentMark newMark = new StudentAssessmentMark();
        newMark.setStudent(student);
        newMark.setAssessment(assessment);
        newMark.setMarksScored(marks);

        StudentAssessmentMark savedMark = markRepository.save(newMark);
        studentService.recalculateStudentPerformance(studentId);
        return savedMark;
    }

    public boolean markExists(Long studentId, Long assessmentId) {
        return markRepository.findByStudentIdAndAssessmentId(studentId, assessmentId).isPresent();
    }

    public List<StudentAssessmentMark> getMarksForStudent(Long studentId) {
        return markRepository.findByStudentId(studentId);
    }

    public List<StudentAssessmentMark> getMarksForAssessment(Long assessmentId) {
        return markRepository.findByAssessmentId(assessmentId);
    }

    public StudentAssessmentMark updateMark(Long id, Integer marksScored) {
        StudentAssessmentMark mark = markRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mark entry not found"));
        mark.setMarksScored(marksScored);
        StudentAssessmentMark savedMark = markRepository.save(mark);
        studentService.recalculateStudentPerformance(mark.getStudent().getId());
        return savedMark;
    }

    public List<StudentAssessmentMark> getAllMarks() {
        return markRepository.findAll();
    }

    public void deleteMark(Long studentId, Long assessmentId) {
        markRepository.findByStudentIdAndAssessmentId(studentId, assessmentId)
                .ifPresent(mark -> {
                    markRepository.delete(mark);
                    studentService.recalculateStudentPerformance(studentId);
                });
    }
}
