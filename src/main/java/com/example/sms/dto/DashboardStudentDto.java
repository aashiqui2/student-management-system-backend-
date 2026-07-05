package com.example.sms.dto;

public class DashboardStudentDto {
    private Long studentId;
    private String name;
    private String regNo;
    private String email;
    private String hackerRankUsername;
    private String mobileNumber;
    private String linkedInUrl;
    private String githubUrl;
    private String leetcodeUrl;
    private String profilePicUrl;
    private String stream;
    private String specialization;
    private Integer startYear;
    private Integer courseDuration;
    private Integer graduationYear;
    private String pursuingYearLabel;
    private Integer totalMarks;
    private Double averageMarks;
    private String category;

    public DashboardStudentDto() {
    }

    public DashboardStudentDto(Long studentId, String name, String regNo, String email, String hackerRankUsername, String mobileNumber, String linkedInUrl, String githubUrl, String leetcodeUrl, String profilePicUrl, String stream, String specialization, Integer startYear, Integer courseDuration, Integer totalMarks, Double averageMarks, String category) {
        this.studentId = studentId;
        this.name = name;
        this.regNo = regNo;
        this.email = email;
        this.hackerRankUsername = hackerRankUsername;
        this.mobileNumber = mobileNumber;
        this.linkedInUrl = linkedInUrl;
        this.githubUrl = githubUrl;
        this.leetcodeUrl = leetcodeUrl;
        this.profilePicUrl = profilePicUrl;
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
        this.totalMarks = totalMarks;
        this.averageMarks = averageMarks;
        this.category = category;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
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

    public String getHackerRankUsername() {
        return hackerRankUsername;
    }

    public void setHackerRankUsername(String hackerRankUsername) {
        this.hackerRankUsername = hackerRankUsername;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getLinkedInUrl() {
        return linkedInUrl;
    }

    public void setLinkedInUrl(String linkedInUrl) {
        this.linkedInUrl = linkedInUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getLeetcodeUrl() {
        return leetcodeUrl;
    }

    public void setLeetcodeUrl(String leetcodeUrl) {
        this.leetcodeUrl = leetcodeUrl;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
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

    public Integer getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Integer totalMarks) {
        this.totalMarks = totalMarks;
    }

    public Double getAverageMarks() {
        return averageMarks;
    }

    public void setAverageMarks(Double averageMarks) {
        this.averageMarks = averageMarks;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
