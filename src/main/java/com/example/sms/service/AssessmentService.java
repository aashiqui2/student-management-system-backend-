package com.example.sms.service;

import com.example.sms.entity.Assessment;
import com.example.sms.entity.AssessmentResource;
import com.example.sms.repository.AssessmentRepository;
import com.example.sms.repository.AssessmentResourceRepository;
import com.example.sms.repository.StudentAssessmentMarkRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentResourceRepository assessmentResourceRepository;
    private final StudentAssessmentMarkRepository studentAssessmentMarkRepository;
    private final FileStorageService fileStorageService;

    public AssessmentService(AssessmentRepository assessmentRepository, AssessmentResourceRepository assessmentResourceRepository, StudentAssessmentMarkRepository studentAssessmentMarkRepository, FileStorageService fileStorageService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentResourceRepository = assessmentResourceRepository;
        this.studentAssessmentMarkRepository = studentAssessmentMarkRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Assessment> getAllAssessments() {
        return assessmentRepository.findAll();
    }

    public Assessment getAssessmentById(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
    }

    public Assessment createAssessment(Assessment assessment, List<MultipartFile> resources) {
        if (assessmentRepository.existsByAssessmentNameIgnoreCase(assessment.getAssessmentName())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, 
                "Assessment name already exists. Please choose a new name."
            );
        }
        attachResources(assessment, resources);
        return assessmentRepository.save(assessment);
    }

    public Assessment updateAssessment(Long id, Assessment assessmentDetails, List<MultipartFile> resources) {
        Assessment assessment = getAssessmentById(id);
        
        if (!assessment.getAssessmentName().equalsIgnoreCase(assessmentDetails.getAssessmentName()) &&
            assessmentRepository.existsByAssessmentNameIgnoreCase(assessmentDetails.getAssessmentName())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, 
                "Assessment name already exists. Please choose a new name."
            );
        }

        assessment.setAssessmentName(assessmentDetails.getAssessmentName());
        assessment.setDateConducted(assessmentDetails.getDateConducted());
        assessment.setTotalMarks(assessmentDetails.getTotalMarks());
        attachResources(assessment, resources);
        return assessmentRepository.save(assessment);
    }

    private void attachResources(Assessment assessment, List<MultipartFile> resources) {
        if (resources == null || resources.isEmpty()) {
            return;
        }

        if (assessment.getResources() == null) {
            assessment.setResources(new java.util.ArrayList<>());
        }

        for (MultipartFile file : resources) {
            if (file.isEmpty()) {
                continue;
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                List<String> allowed = List.of("pdf", "doc", "docx", "xls", "xlsx", "sql", "csv", "txt", "png", "jpg", "jpeg", "gif");
                if (!allowed.contains(ext)) {
                    throw new RuntimeException("Invalid file type: " + ext + ". Allowed types: " + allowed);
                }
            }

            String storedFilePath = fileStorageService.storeFile("resources", file);
            AssessmentResource resource = new AssessmentResource();
            resource.setName(StringUtils.cleanPath(file.getOriginalFilename()));
            resource.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            resource.setFileName(storedFilePath);
            resource.setAssessment(assessment);
            assessment.getResources().add(resource);
        }
    }

    public AssessmentResource getResourceById(Long id) {
        return assessmentResourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment resource not found with id " + id));
    }

    public Resource loadResourceFile(String fileName) {
        return fileStorageService.loadFileAsResource(fileName);
    }

    @Transactional
    public void deleteAssessment(Long id) {
        studentAssessmentMarkRepository.deleteByAssessmentId(id);
        Assessment assessment = getAssessmentById(id);
        assessmentRepository.delete(assessment);
    }

    @Transactional
    public void deleteAssessments(List<Long> ids) {
        for (Long id : ids) {
            studentAssessmentMarkRepository.deleteByAssessmentId(id);
        }
        assessmentRepository.deleteAllById(ids);
    }

    @Transactional
    public void deleteAllAssessments() {
        studentAssessmentMarkRepository.deleteAll();
        assessmentRepository.deleteAll();
    }
}
