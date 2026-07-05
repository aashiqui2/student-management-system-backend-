package com.example.sms.dto;

import java.util.List;

public class StudentDetailDto {
    private Long id;
    private String name;
    private String regNo;
    private String email;
    private String mobileNumber;
    private String stream;
    private String specialization;
    private Integer startYear;
    private Integer courseDuration;
    private Integer graduationYear;
    private String pursuingYearLabel;
    private String hackerRankUsername;
    private String profilePicUrl;
    private String category;
    private Double averageMarks;
    private Integer totalMarks;
    private List<AssessmentMarkDto> assessments;

    public StudentDetailDto() {
    }

    public StudentDetailDto(Long id, String name, String regNo, String email, String mobileNumber, String stream, String specialization, Integer startYear, Integer courseDuration, String hackerRankUsername, String profilePicUrl, String category, Double averageMarks, Integer totalMarks, List<AssessmentMarkDto> assessments) {
        this.id = id;
        this.name = name;
        this.regNo = regNo;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.stream = stream;
        this.specialization = specialization;
        this.startYear = startYear;
        this.courseDuration = courseDuration;
        
        // Dynamic calculations
        int duration = (courseDuration != null) ? courseDuration : 4; // Default to 4 if null
        this.graduationYear = (startYear != null) ? startYear + duration : null;
        
        if (startYear != null) {
            int currentYear = java.time.LocalDate.now().getYear();
            if (currentYear >= this.graduationYear) {
                this.pursuingYearLabel = "Graduated";
            } else if (currentYear < startYear) {
                this.pursuingYearLabel = "Not Started";
            } else {
                int diff = currentYear - startYear;
                switch (diff) {
                    case 0: this.pursuingYearLabel = "First Year"; break;
                    case 1: this.pursuingYearLabel = "Second Year"; break;
                    case 2: this.pursuingYearLabel = "Third Year"; break;
                    case 3: this.pursuingYearLabel = "Fourth Year"; break;
                    case 4: this.pursuingYearLabel = "Fifth Year"; break;
                    default: this.pursuingYearLabel = diff + "th Year"; break;
                }
            }
        } else {
            this.pursuingYearLabel = "Unknown";
        }
        this.hackerRankUsername = hackerRankUsername;
        this.profilePicUrl = profilePicUrl;
        this.category = category;
        this.averageMarks = averageMarks;
        this.totalMarks = totalMarks;
        this.assessments = assessments;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public Integer getStartYear() {
        return startYear;
    }

    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    public Integer getCourseDuration() {
        return courseDuration;
    }

    public void setCourseDuration(Integer courseDuration) {
        this.courseDuration = courseDuration;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getPursuingYearLabel() {
        return pursuingYearLabel;
    }

    public void setPursuingYearLabel(String pursuingYearLabel) {
        this.pursuingYearLabel = pursuingYearLabel;
    }

    public String getHackerRankUsername() {
        return hackerRankUsername;
    }

    public void setHackerRankUsername(String hackerRankUsername) {
        this.hackerRankUsername = hackerRankUsername;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getAverageMarks() {
        return averageMarks;
    }

    public void setAverageMarks(Double averageMarks) {
        this.averageMarks = averageMarks;
    }

    public Integer getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Integer totalMarks) {
        this.totalMarks = totalMarks;
    }

    public List<AssessmentMarkDto> getAssessments() {
        return assessments;
    }

    public void setAssessments(List<AssessmentMarkDto> assessments) {
        this.assessments = assessments;
    }
}
