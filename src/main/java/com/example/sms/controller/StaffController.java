package com.example.sms.controller;

import com.example.sms.entity.StaffProfile;
import com.example.sms.entity.User;
import com.example.sms.entity.Designation;
import com.example.sms.repository.StaffProfileRepository;
import com.example.sms.repository.DesignationRepository;
import com.example.sms.repository.UserRepository;
import com.example.sms.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffProfileRepository staffProfileRepository;
    private final DesignationRepository designationRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public StaffController(StaffProfileRepository staffProfileRepository, DesignationRepository designationRepository, UserRepository userRepository, FileStorageService fileStorageService) {
        this.staffProfileRepository = staffProfileRepository;
        this.designationRepository = designationRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    private void verifyAnyStaffAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isStaff = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
        
        if (!isAdmin && !isStaff) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void verifyAdminAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<StaffProfile> getCurrentStaffProfile() {
        verifyAnyStaffAccess();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        StaffProfile staff = staffProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff profile not found"));
        
        return ResponseEntity.ok(staff);
    }

    @PutMapping("/me")
    public ResponseEntity<StaffProfile> updateCurrentStaffProfile(
            @RequestBody StaffProfile staffDetails) {
        verifyAnyStaffAccess();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        StaffProfile staff = staffProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    StaffProfile newProfile = new StaffProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });
        
        if (staffDetails.getName() != null) staff.setName(staffDetails.getName());
        if (staffDetails.getEmail() != null) staff.setEmail(staffDetails.getEmail());
        if (staffDetails.getMobileNumber() != null) staff.setMobileNumber(staffDetails.getMobileNumber());
        if (staffDetails.getLinkedInUrl() != null) staff.setLinkedInUrl(staffDetails.getLinkedInUrl());
        if (staffDetails.getSpecialization() != null) staff.setSpecialization(staffDetails.getSpecialization());
        if (staffDetails.getDepartment() != null) staff.setDepartment(staffDetails.getDepartment());
        if (staffDetails.getStream() != null) staff.setStream(staffDetails.getStream());
        
        if (staffDetails.getUsername() != null && !staffDetails.getUsername().trim().isEmpty()) {
            if (!staffDetails.getUsername().equals(user.getUsername())) {
                boolean exists = userRepository.findByUsername(staffDetails.getUsername()).isPresent();
                if (exists) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken");
                }
                user.setUsername(staffDetails.getUsername());
                userRepository.save(user);
            }
        }
        
        // Editable fields
        if (staffDetails.getGender() != null) staff.setGender(staffDetails.getGender());
        if (staffDetails.getDateOfBirth() != null) staff.setDateOfBirth(staffDetails.getDateOfBirth());
        if (staffDetails.getAddress() != null) staff.setAddress(staffDetails.getAddress());
        if (staffDetails.getCity() != null) staff.setCity(staffDetails.getCity());
        if (staffDetails.getState() != null) staff.setState(staffDetails.getState());
        if (staffDetails.getCountry() != null) staff.setCountry(staffDetails.getCountry());
        if (staffDetails.getPincode() != null) staff.setPincode(staffDetails.getPincode());
        if (staffDetails.getQualification() != null) staff.setQualification(staffDetails.getQualification());
        if (staffDetails.getExperience() != null) staff.setExperience(staffDetails.getExperience());
        if (staffDetails.getGithubUrl() != null) staff.setGithubUrl(staffDetails.getGithubUrl());
        if (staffDetails.getPortfolioUrl() != null) staff.setPortfolioUrl(staffDetails.getPortfolioUrl());
        
        if (staffDetails.getDesignation() != null && staffDetails.getDesignation().getId() != null) {
            Designation designation = designationRepository.findById(staffDetails.getDesignation().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designation not found"));
            staff.setDesignation(designation);
        }
        
        if (staff.getDesignation() != null) {
            String dName = staff.getDesignation().getName();
            boolean isTrainer = dName.equalsIgnoreCase("Technical Trainer") || 
                                dName.equalsIgnoreCase("Soft Skill Trainer") || 
                                dName.equalsIgnoreCase("Aptitude Trainer");
            boolean isFaculty = dName.equalsIgnoreCase("College Faculty");
            
            if (isTrainer) {
                if (staff.getQualification() == null || staff.getQualification().trim().isEmpty() ||
                    staff.getExperience() == null ||
                    staff.getSpecialization() == null || staff.getSpecialization().trim().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Qualification, Experience, and Specialization are required for Trainers.");
                }
            } else if (isFaculty) {
                if (staff.getDepartment() == null || staff.getDepartment().trim().isEmpty() ||
                    staff.getStream() == null || staff.getStream().trim().isEmpty() ||
                    staff.getQualification() == null || staff.getQualification().trim().isEmpty() ||
                    staff.getExperience() == null ||
                    staff.getSpecialization() == null || staff.getSpecialization().trim().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department, Stream, Specialization, Qualification, and Experience are required for College Faculty.");
                }
            }
        }
        
        return ResponseEntity.ok(staffProfileRepository.save(staff));
    }

    @GetMapping
    public ResponseEntity<List<StaffProfile>> getAllStaff() {
        verifyAnyStaffAccess();
        List<StaffProfile> all = staffProfileRepository.findAll();
        // Don't show admin in the staff list, regardless of who is asking
        all.removeIf(p -> p.getUser() != null && p.getUser().getRole() == com.example.sms.enums.Role.ADMIN);
        return ResponseEntity.ok(all);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffProfile> getStaffById(@PathVariable Long id) {
        verifyAdminAccess();
        return staffProfileRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffProfile> updateStaff(
            @PathVariable Long id,
            @RequestBody StaffProfile staffDetails) {
        verifyAdminAccess();
        
        return staffProfileRepository.findById(id)
                .map(staff -> {
                    if (staffDetails.getName() != null) staff.setName(staffDetails.getName());
                    if (staffDetails.getEmail() != null) staff.setEmail(staffDetails.getEmail());
                    if (staffDetails.getMobileNumber() != null) staff.setMobileNumber(staffDetails.getMobileNumber());
                    if (staffDetails.getLinkedInUrl() != null) staff.setLinkedInUrl(staffDetails.getLinkedInUrl());
                    if (staffDetails.getSpecialization() != null) staff.setSpecialization(staffDetails.getSpecialization());
                    
                    // Admin controlled
                    if (staffDetails.getStream() != null) staff.setStream(staffDetails.getStream());
                    if (staffDetails.getDepartment() != null) staff.setDepartment(staffDetails.getDepartment());
                    if (staffDetails.getEmployeeId() != null) staff.setEmployeeId(staffDetails.getEmployeeId());
                    if (staffDetails.getJoiningDate() != null) staff.setJoiningDate(staffDetails.getJoiningDate());
                    
                    if (staffDetails.getDesignation() != null && staffDetails.getDesignation().getId() != null) {
                        Designation designation = designationRepository.findById(staffDetails.getDesignation().getId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designation not found"));
                        staff.setDesignation(designation);
                    }
                    
                    if (staffDetails.getGender() != null) staff.setGender(staffDetails.getGender());
                    if (staffDetails.getDateOfBirth() != null) staff.setDateOfBirth(staffDetails.getDateOfBirth());
                    if (staffDetails.getAddress() != null) staff.setAddress(staffDetails.getAddress());
                    if (staffDetails.getCity() != null) staff.setCity(staffDetails.getCity());
                    if (staffDetails.getState() != null) staff.setState(staffDetails.getState());
                    if (staffDetails.getCountry() != null) staff.setCountry(staffDetails.getCountry());
                    if (staffDetails.getPincode() != null) staff.setPincode(staffDetails.getPincode());
                    if (staffDetails.getQualification() != null) staff.setQualification(staffDetails.getQualification());
                    if (staffDetails.getExperience() != null) staff.setExperience(staffDetails.getExperience());
                    if (staffDetails.getGithubUrl() != null) staff.setGithubUrl(staffDetails.getGithubUrl());
                    if (staffDetails.getPortfolioUrl() != null) staff.setPortfolioUrl(staffDetails.getPortfolioUrl());
                    
                    return ResponseEntity.ok(staffProfileRepository.save(staff));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        verifyAdminAccess();
        return staffProfileRepository.findById(id)
                .map(staff -> {
                    User user = staff.getUser();
                    staffProfileRepository.delete(staff);
                    if (user != null) {
                        userRepository.delete(user);
                    }
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/me/photo", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<StaffProfile> uploadPhoto(@RequestParam("file") MultipartFile file) {
        verifyAnyStaffAccess();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        StaffProfile staff = staffProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    StaffProfile newProfile = new StaffProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });
        
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        
        String fileName = fileStorageService.storeFile(file);
        staff.setProfilePhotoUrl(fileName);
        return ResponseEntity.ok(staffProfileRepository.save(staff));
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable Long id, HttpServletRequest request) {
        StaffProfile staff = staffProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff profile not found"));
        
        if (staff.getProfilePhotoUrl() == null || staff.getProfilePhotoUrl().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = fileStorageService.loadFileAsResource(staff.getProfilePhotoUrl());
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

    @DeleteMapping("/me/photo")
    public ResponseEntity<StaffProfile> deletePhoto() {
        verifyAnyStaffAccess();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        StaffProfile staff = staffProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff profile not found"));
        
        staff.setProfilePhotoUrl(null);
        return ResponseEntity.ok(staffProfileRepository.save(staff));
    }

    @GetMapping("/designations")
    public ResponseEntity<List<Designation>> getDesignations() {
        return ResponseEntity.ok(designationRepository.findAll());
    }
}
