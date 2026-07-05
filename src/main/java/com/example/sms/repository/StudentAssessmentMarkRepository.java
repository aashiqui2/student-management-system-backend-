package com.example.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.sms.entity.StudentAssessmentMark;

public interface StudentAssessmentMarkRepository extends JpaRepository<StudentAssessmentMark, Long> {
    List<StudentAssessmentMark> findByStudentId(Long studentId);
    List<StudentAssessmentMark> findByAssessmentId(Long assessmentId);
    void deleteByAssessmentId(Long assessmentId);
    Optional<StudentAssessmentMark> findByStudentIdAndAssessmentId(Long studentId, Long assessmentId);
}
