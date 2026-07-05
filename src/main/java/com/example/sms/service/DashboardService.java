package com.example.sms.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.sms.dto.DashboardStudentDto;
import com.example.sms.entity.Student;
import com.example.sms.util.CategoryUtil;

@Service
public class DashboardService {

    private final StudentService studentService;
    private final CategoryUtil categoryUtil;

    public DashboardService(StudentService studentService, CategoryUtil categoryUtil) {
        this.studentService = studentService;
        this.categoryUtil = categoryUtil;
    }

    public List<DashboardStudentDto> getDashboardData() {
        List<Student> students = studentService.getAllStudents();
        List<DashboardStudentDto> dashboardData = new ArrayList<>();

        for (Student student : students) {
            int totalMarks = student.getTotalMarks() != null ? student.getTotalMarks() : 0;
            double averageMarks = student.getAverageMarks() != null ? student.getAverageMarks() : 0.0;
            String category = categoryUtil.determineCategory(averageMarks);

            DashboardStudentDto dto = new DashboardStudentDto();
            dto.setStudentId(student.getId());
            dto.setName(student.getName());
            dto.setRegNo(student.getRegNo());
            dto.setEmail(student.getEmail());
            dto.setHackerRankUsername(student.getHackerRankUsername());
            dto.setMobileNumber(student.getMobileNumber());
            dto.setLinkedInUrl(student.getLinkedInUrl());
            dto.setGithubUrl(student.getGithubUrl());
            dto.setLeetcodeUrl(student.getLeetcodeUrl());
            
            dto.setStream(student.getStream());
            dto.setSpecialization(student.getSpecialization());
            dto.setStartYear(student.getStartYear());
            dto.setCourseDuration(student.getCourseDuration());
            
            int duration = (student.getCourseDuration() != null) ? student.getCourseDuration() : 4;
            dto.setGraduationYear((student.getStartYear() != null) ? student.getStartYear() + duration : null);
            
            if (student.getStartYear() != null) {
                int currentYear = java.time.LocalDate.now().getYear();
                if (currentYear >= dto.getGraduationYear()) {
                    dto.setPursuingYearLabel("Graduated");
                } else if (currentYear < student.getStartYear()) {
                    dto.setPursuingYearLabel("Not Started");
                } else {
                    int diff = currentYear - student.getStartYear();
                    switch (diff) {
                        case 0: dto.setPursuingYearLabel("First Year"); break;
                        case 1: dto.setPursuingYearLabel("Second Year"); break;
                        case 2: dto.setPursuingYearLabel("Third Year"); break;
                        case 3: dto.setPursuingYearLabel("Fourth Year"); break;
                        case 4: dto.setPursuingYearLabel("Fifth Year"); break;
                        default: dto.setPursuingYearLabel(diff + "th Year"); break;
                    }
                }
            } else {
                dto.setPursuingYearLabel("Unknown");
            }
            
            if(student.getProfilePicUrl() != null && !student.getProfilePicUrl().isEmpty()) {
                 dto.setProfilePicUrl("/api/students/" + student.getId() + "/photo");
            }

            dto.setTotalMarks(totalMarks);
            dto.setAverageMarks(averageMarks);
            dto.setCategory(category);

            dashboardData.add(dto);
        }

        // Sort by average marks descending
        return dashboardData.stream()
                .sorted(Comparator.comparing(DashboardStudentDto::getAverageMarks).reversed())
                .collect(Collectors.toList());
    }

    public List<DashboardStudentDto> getDashboardDataByCategory(String level) {
        return getDashboardData().stream()
                .filter(dto -> dto.getCategory().equalsIgnoreCase(level))
                .collect(Collectors.toList());
    }
}
