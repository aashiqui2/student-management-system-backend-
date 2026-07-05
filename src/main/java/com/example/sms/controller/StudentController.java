package com.example.sms.controller;

import com.example.sms.entity.Student;
import com.example.sms.entity.StaffProfile;
import com.example.sms.repository.StaffProfileRepository;
import com.example.sms.repository.UserRepository;
import com.example.sms.service.ExcelService;
import com.example.sms.service.FileStorageService;
import com.example.sms.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final FileStorageService fileStorageService;
    private final ExcelService excelService;
    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;

    public StudentController(StudentService studentService, FileStorageService fileStorageService, ExcelService excelService, StaffProfileRepository staffProfileRepository, UserRepository userRepository) {
        this.studentService = studentService;
        this.fileStorageService = fileStorageService;
        this.excelService = excelService;
        this.staffProfileRepository = staffProfileRepository;
        this.userRepository = userRepository;
    }

    private void verifyAccess(Long studentId, boolean isUpdate) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isStaff = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
        
        if (isAdmin) return;
        
        if (isStaff) {
            // Staff can view and edit their assigned students, but for simplicity we allow staff to edit any student.
            return;
        }
        
        // It's a STUDENT
        Student student = studentService.getStudentById(studentId);
        if (student.getUser() == null || !student.getUser().getUsername().equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own profile");
        }
    }

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.ok(List.of());

        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        boolean isStaff   = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));

        if (isStudent) {
            try {
                Student me = studentService.getStudentByUsername(auth.getName());
                return ResponseEntity.ok(List.of(me));
            } catch (Exception e) {
                return ResponseEntity.ok(List.of());
            }
        }

        if (isStaff) {
            // Return all students so staff can view the complete dashboard
            return ResponseEntity.ok(studentService.getAllStudents());
        }

        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/me")
    public ResponseEntity<Student> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return ResponseEntity.ok(studentService.getStudentByUsername(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        verifyAccess(id, false);
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<com.example.sms.dto.StudentDetailDto> getStudentDetails(@PathVariable Long id) {
        verifyAccess(id, false);
        return ResponseEntity.ok(studentService.getStudentDetails(id));
    }

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<Student> createStudent(
            @RequestPart("student") @Valid Student student,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaff = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
        if (isStaff) {
            // Auto-assign this student to the calling staff member
            userRepository.findByUsername(auth.getName())
                .flatMap(staffProfileRepository::findByUser)
                .ifPresent(student::setAssignedStaff);
        }
        return ResponseEntity.ok(studentService.createStudent(student, file));
    }

    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<Student> updateStudent(
            @PathVariable Long id,
            @RequestPart("student") @Valid Student studentDetails,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        verifyAccess(id, true);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        
        if (isStudent) {
            // Students can only update specific fields, so we enforce it by copying only allowed fields.
            // A better way is to do it in the service, but we will fetch and overwrite in controller for security.
            Student existing = studentService.getStudentById(id);
            existing.setEmail(studentDetails.getEmail());
            existing.setMobileNumber(studentDetails.getMobileNumber());
            existing.setLinkedInUrl(studentDetails.getLinkedInUrl());
            existing.setGithubUrl(studentDetails.getGithubUrl());
            existing.setLeetcodeUrl(studentDetails.getLeetcodeUrl());
            existing.setHackerRankUsername(studentDetails.getHackerRankUsername());
            existing.setStream(studentDetails.getStream());
            existing.setSpecialization(studentDetails.getSpecialization());
            existing.setStartYear(studentDetails.getStartYear());
            existing.setCourseDuration(studentDetails.getCourseDuration());
            
            // Validate courseDuration based on updated degree/startYear
            if (existing.getStartYear() != null && existing.getCourseDuration() != null && existing.getStream() != null) {
                String stream = existing.getStream().toUpperCase();
                boolean isEngineering = stream.contains("ENGINEERING") || stream.contains("B.E") || stream.contains("B.TECH") || stream.equals("BE") || stream.equals("BTECH");
                int expectedDuration = isEngineering ? 4 : 3;
                if (existing.getCourseDuration() != expectedDuration) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid course duration for the selected stream");
                }
            }
            // Ignore name, regNo, email
            return ResponseEntity.ok(studentService.updateStudent(id, existing, file));
        }

        return ResponseEntity.ok(studentService.updateStudent(id, studentDetails, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteStudents(@RequestBody List<Long> ids) {
        studentService.deleteStudents(ids);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllStudents() {
        studentService.deleteAllStudents();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> getStudentPhoto(@PathVariable Long id, HttpServletRequest request) {
        Student student = studentService.getStudentById(id);
        if (student.getProfilePicUrl() == null || student.getProfilePicUrl().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = fileStorageService.loadFileAsResource(student.getProfilePicUrl());
        
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            System.out.println("Could not determine file type.");
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}/photo")
    public ResponseEntity<Void> deleteStudentPhoto(@PathVariable Long id) {
        verifyAccess(id, true);
        studentService.deleteStudentPhoto(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadStudentsExcel(@RequestParam("file") MultipartFile file) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            StaffProfile assignedStaff = null;
            if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"))) {
                assignedStaff = userRepository.findByUsername(auth.getName())
                    .flatMap(staffProfileRepository::findByUser)
                    .orElse(null);
            } else if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                // When admin imports, find all real ROLE_STAFF users and auto-assign if only one exists
                List<StaffProfile> staffOnlyProfiles = staffProfileRepository.findAllByUserRoleStaff();
                if (staffOnlyProfiles.size() == 1) {
                    assignedStaff = staffOnlyProfiles.get(0);
                }
            }
            Map<String, Object> result = excelService.processStudentsExcel(file, assignedStaff);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to upload students: " + e.getMessage());
        }
    }
}
