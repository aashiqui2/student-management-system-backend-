package com.example.sms.repository;

import com.example.sms.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    boolean existsByAssessmentNameIgnoreCase(String assessmentName);
}
