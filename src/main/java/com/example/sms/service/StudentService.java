package com.example.sms.service;

import com.example.sms.dto.AssessmentMarkDto;
import com.example.sms.dto.StudentDetailDto;
import com.example.sms.entity.Student;
import com.example.sms.entity.User;
import com.example.sms.enums.Role;
import com.example.sms.entity.StaffProfile;
import com.example.sms.repository.StudentRepository;
import com.example.sms.util.CategoryUtil;
import com.example.sms.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final FileStorageService fileStorageService;
    private final CategoryUtil categoryUtil;
    private final com.example.sms.repository.StudentAssessmentMarkRepository markRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository, FileStorageService fileStorageService, CategoryUtil categoryUtil, com.example.sms.repository.StudentAssessmentMarkRepository markRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.fileStorageService = fileStorageService;
        this.categoryUtil = categoryUtil;
        this.markRepository = markRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll().stream()
                .filter(this::isStudentEnabled)
                .collect(Collectors.toList());
    }

    public List<Student> getStudentsByStaff(StaffProfile staffProfile) {
        return studentRepository.findByAssignedStaff(staffProfile).stream()
                .filter(this::isStudentEnabled)
                .collect(Collectors.toList());
    }

    private boolean isStudentEnabled(Student student) {
        return student.getUser() == null || student.getUser().isEnabled();
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id " + id));
    }

    public Student createStudent(Student student, MultipartFile file) {
        if (studentRepository.findByRegNo(student.getRegNo()).isPresent()) {
            throw new RuntimeException("Student with Reg No already exists");
        }
        if (studentRepository.findByEmail(student.getEmail()).isPresent()) {
            throw new RuntimeException("Student with Email already exists");
        }

        if (file != null && !file.isEmpty()) {
            String fileName = fileStorageService.storeFile(file);
            student.setProfilePicUrl(fileName);
        }

        // Link to user if username matches regNo, or create new user
        Optional<User> userOpt = userRepository.findByUsername(student.getRegNo());
        if (userOpt.isPresent()) {
            student.setUser(userOpt.get());
        } else {
            User newUser = new User();
            newUser.setUsername(student.getRegNo());
            newUser.setPassword(passwordEncoder.encode("student123"));
            newUser.setRole(Role.STUDENT);
            userRepository.save(newUser);
            student.setUser(newUser);
        }

        return studentRepository.save(student);
    }
    
    public Student getStudentByUsername(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Student profile not found for user: " + username));
    }

    public StudentDetailDto getStudentDetails(Long id) {
        Student student = getStudentById(id);
        
        List<AssessmentMarkDto> assessmentDtos = student.getMarks().stream().map(mark -> {
            AssessmentMarkDto dto = new AssessmentMarkDto();
            dto.setAssessmentName(mark.getAssessment().getAssessmentName());
            dto.setDateConducted(mark.getAssessment().getDateConducted());
            dto.setTotalMarks(mark.getAssessment().getTotalMarks());
            dto.setMarksScored(mark.getMarksScored());
            return dto;
        }).collect(Collectors.toList());
        
        int totalMarks = student.getTotalMarks() != null ? student.getTotalMarks() : 0;
        double averageMarks = student.getAverageMarks() != null ? student.getAverageMarks() : 0.0;
        String category = student.getCategory();
        if (category == null) {
            category = categoryUtil.determineCategory(averageMarks);
        }
        
        StudentDetailDto dto = new StudentDetailDto();
        dto.setId(student.getId());
        dto.setName(student.getName());
        dto.setRegNo(student.getRegNo());
        dto.setEmail(student.getEmail());
        dto.setMobileNumber(student.getMobileNumber());
        dto.setStream(student.getStream());
        dto.setSpecialization(student.getSpecialization());
        dto.setStartYear(student.getStartYear());
        dto.setCourseDuration(student.getCourseDuration());
        
        // Manual trigger of dynamic calculation since we are using setters
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
        dto.setHackerRankUsername(student.getHackerRankUsername());
        
        if (student.getProfilePicUrl() != null && !student.getProfilePicUrl().isEmpty()) {
            dto.setProfilePicUrl("/api/students/" + student.getId() + "/photo");
        }
        
        dto.setCategory(category);
        dto.setAverageMarks(averageMarks);
        dto.setTotalMarks(totalMarks);
        dto.setAssessments(assessmentDtos);
        
        return dto;
    }

    public Student updateStudent(Long id, Student studentDetails, MultipartFile file) {
        Student existingStudent = getStudentById(id);

        existingStudent.setName(studentDetails.getName());
        existingStudent.setRegNo(studentDetails.getRegNo());
        existingStudent.setEmail(studentDetails.getEmail());
        existingStudent.setMobileNumber(studentDetails.getMobileNumber());
        existingStudent.setHackerRankUsername(studentDetails.getHackerRankUsername());
        existingStudent.setStream(studentDetails.getStream());
        existingStudent.setSpecialization(studentDetails.getSpecialization());
        existingStudent.setStartYear(studentDetails.getStartYear());
        existingStudent.setCourseDuration(studentDetails.getCourseDuration());
        existingStudent.setLinkedInUrl(studentDetails.getLinkedInUrl());
        existingStudent.setGithubUrl(studentDetails.getGithubUrl());
        existingStudent.setLeetcodeUrl(studentDetails.getLeetcodeUrl());

        if (file != null && !file.isEmpty()) {
            String fileName = fileStorageService.storeFile(file);
            existingStudent.setProfilePicUrl(fileName);
        }

        return studentRepository.save(existingStudent);
    }

    public void deleteStudentPhoto(Long id) {
        Student student = getStudentById(id);
        student.setProfilePicUrl(null);
        studentRepository.save(student);
    }

    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        studentRepository.delete(student);
    }

    public void deleteStudents(List<Long> ids) {
        studentRepository.deleteAllById(ids);
    }

    public void deleteAllStudents() {
        studentRepository.deleteAll();
    }

    public void recalculateStudentPerformance(Long studentId) {
        Student student = getStudentById(studentId);
        List<com.example.sms.entity.StudentAssessmentMark> freshMarks = markRepository.findByStudentId(studentId);
        int totalMarksScored = 0;
        int totalPossibleMarks = 0;
        if (freshMarks != null && !freshMarks.isEmpty()) {
            for(var mark : freshMarks) {
                totalMarksScored += mark.getMarksScored();
                totalPossibleMarks += mark.getAssessment().getTotalMarks();
            }
        }
        double percentage = (totalPossibleMarks == 0) ? 0.0 : ((double) totalMarksScored / totalPossibleMarks) * 100.0;
        String category = categoryUtil.determineCategory(percentage);

        student.setTotalMarks(totalMarksScored);
        student.setAverageMarks(percentage);
        student.setCategory(category);
        studentRepository.save(student);
    }

    public void recalculatePerformanceForStudents(Iterable<Long> studentIds) {
        for (Long id : studentIds) {
            recalculateStudentPerformance(id);
        }
    }
}
