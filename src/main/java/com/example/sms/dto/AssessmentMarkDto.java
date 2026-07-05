package com.example.sms.dto;

import java.time.LocalDate;

public class AssessmentMarkDto {
    private String assessmentName;
    private LocalDate dateConducted;
    private Integer totalMarks;
    private Integer marksScored;

    public AssessmentMarkDto() {
    }

    public AssessmentMarkDto(String assessmentName, LocalDate dateConducted, Integer totalMarks, Integer marksScored) {
        this.assessmentName = assessmentName;
        this.dateConducted = dateConducted;
        this.totalMarks = totalMarks;
        this.marksScored = marksScored;
    }

    public String getAssessmentName() {
        return assessmentName;
    }

    public void setAssessmentName(String assessmentName) {
        this.assessmentName = assessmentName;
    }

    public LocalDate getDateConducted() {
        return dateConducted;
    }

    public void setDateConducted(LocalDate dateConducted) {
        this.dateConducted = dateConducted;
    }

    public Integer getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Integer totalMarks) {
        this.totalMarks = totalMarks;
    }

    public Integer getMarksScored() {
        return marksScored;
    }

    public void setMarksScored(Integer marksScored) {
        this.marksScored = marksScored;
    }
}
