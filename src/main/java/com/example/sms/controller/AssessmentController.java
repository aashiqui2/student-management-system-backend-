package com.example.sms.controller;

import com.example.sms.entity.Assessment;
import com.example.sms.entity.AssessmentResource;
import com.example.sms.service.AssessmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.sms.repository.UserRepository;
import com.example.sms.repository.StaffProfileRepository;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;

    public AssessmentController(AssessmentService assessmentService, UserRepository userRepository, StaffProfileRepository staffProfileRepository) {
        this.assessmentService = assessmentService;
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
    }

    @GetMapping
    public ResponseEntity<List<Assessment>> getAllAssessments() {
        return ResponseEntity.ok(assessmentService.getAllAssessments());
    }

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<Assessment> createAssessment(
            @RequestPart("assessment") @Valid Assessment assessment,
            @RequestPart(value = "resources", required = false) List<MultipartFile> resources) {
            
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            userRepository.findByUsername(username).ifPresent(user -> {
                if (user.getRole().name().equals("ADMIN")) {
                    assessment.setCreatedBy("Admin");
                } else if (user.getRole().name().equals("STAFF")) {
                    staffProfileRepository.findByUser(user).ifPresent(staff -> {
                        assessment.setCreatedBy(staff.getName());
                    });
                }
            });
        }
            
        return ResponseEntity.ok(assessmentService.createAssessment(assessment, resources));
    }

    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<Assessment> updateAssessment(
            @PathVariable Long id,
            @RequestPart("assessment") @Valid Assessment assessmentDetails,
            @RequestPart(value = "resources", required = false) List<MultipartFile> resources) {
        return ResponseEntity.ok(assessmentService.updateAssessment(id, assessmentDetails, resources));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssessment(@PathVariable Long id) {
        assessmentService.deleteAssessment(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteAssessments(@RequestBody List<Long> ids) {
        assessmentService.deleteAssessments(ids);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllAssessments() {
        assessmentService.deleteAllAssessments();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resources/{resourceId}")
    public ResponseEntity<Resource> downloadResource(@PathVariable Long resourceId, HttpServletRequest request) {
        AssessmentResource resource = assessmentService.getResourceById(resourceId);
        Resource fileResource = assessmentService.loadResourceFile(resource.getFileName());

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(fileResource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            System.out.println("Could not determine file type.");
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getName() + "\"")
                .body(fileResource);
    }
}
