package com.example.sms.dto;

public class RegisterRequest {
    private String username;
    private String password;
    private String name;
    private String email;
    private String stream;
    private String department;
    private String specialization;
    private String roleType;
    private String regNo;
    private Integer startYear;
    private Integer courseDuration;

    public RegisterRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStream() { return stream; }
    public void setStream(String stream) { this.stream = stream; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public Integer getStartYear() { return startYear; }
    public void setStartYear(Integer startYear) { this.startYear = startYear; }
    public Integer getCourseDuration() { return courseDuration; }
    public void setCourseDuration(Integer courseDuration) { this.courseDuration = courseDuration; }
}
