package com.example.sms.controller;

import com.example.sms.dto.AuthResponse;
import com.example.sms.dto.LoginRequest;
import com.example.sms.dto.RegisterRequest;
import com.example.sms.entity.User;
import com.example.sms.enums.Role;
import com.example.sms.repository.UserRepository;
import com.example.sms.repository.StudentRepository;
import com.example.sms.repository.StaffProfileRepository;
import com.example.sms.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.example.sms.dto.ChangePasswordRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final StudentRepository studentRepository;
    private final StaffProfileRepository staffProfileRepository;

    public AuthController(UserRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, StudentRepository studentRepository, StaffProfileRepository staffProfileRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.studentRepository = studentRepository;
        this.staffProfileRepository = staffProfileRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (repository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        if ("staff".equalsIgnoreCase(request.getRoleType())) {
            user.setRole(Role.STAFF);
            user.setEnabled(false);
        } else {
            user.setRole(Role.STUDENT);
            user.setEnabled(true);
        }
        
        repository.save(user);
        
        if ("staff".equalsIgnoreCase(request.getRoleType())) {
            com.example.sms.entity.StaffProfile staff = new com.example.sms.entity.StaffProfile();
            staff.setUser(user);
            staff.setName(request.getUsername());
            
            long count = staffProfileRepository.count();
            String formattedId = String.format("STF%03d", count + 1);
            while (staffProfileRepository.findByEmployeeId(formattedId).isPresent()) {
                count++;
                formattedId = String.format("STF%03d", count + 1);
            }
            staff.setEmployeeId(formattedId);
            
            staffProfileRepository.save(staff);
        } else if ("student".equalsIgnoreCase(request.getRoleType())) {
            java.util.Optional<com.example.sms.entity.Student> existingStudent = studentRepository.findByRegNo(request.getUsername());
            if (existingStudent.isPresent()) {
                com.example.sms.entity.Student student = existingStudent.get();
                student.setUser(user);
                studentRepository.save(student);
            } else {
                com.example.sms.entity.Student student = new com.example.sms.entity.Student();
                student.setUser(user);
                student.setRegNo(request.getRegNo() != null && !request.getRegNo().isEmpty() ? request.getRegNo() : request.getUsername());
                student.setName(request.getName() != null && !request.getName().isEmpty() ? request.getName() : request.getUsername());
                student.setEmail(request.getEmail() != null && !request.getEmail().isEmpty() ? request.getEmail() : request.getUsername() + "@school.com");
                student.setStream(request.getStream());
                student.setDepartment(request.getDepartment());
                student.setSpecialization(request.getSpecialization());
                student.setStartYear(request.getStartYear());
                student.setCourseDuration(request.getCourseDuration());
                if (request.getStartYear() != null && request.getCourseDuration() != null && request.getStream() != null) {
                    String stream = request.getStream().toUpperCase();
                    int expectedDuration = (stream.equals("BE") || stream.equals("B.TECH") || stream.equals("BTECH")) ? 4 : 3;
                    if (request.getCourseDuration() != expectedDuration) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid course duration for the selected stream");
                    }
                }
                studentRepository.save(student);
            }
        } else {
            // Automatically link to existing student profile if registration number matches username
            studentRepository.findByRegNo(request.getUsername()).ifPresent(student -> {
                student.setUser(user);
                studentRepository.save(student);
            });
        }
        
        String jwtToken = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(jwtToken, user.getUsername(), user.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (org.springframework.security.authentication.DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is disabled. Please wait for admin approval.");
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
        
        var user = repository.findByUsername(request.getUsername()).orElseThrow();
        if (!user.isEnabled()) {
            return ResponseEntity.status(403).body("Account is disabled. Please wait for admin approval.");
        }
        var jwtToken = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(jwtToken, user.getUsername(), user.getRole()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String username = auth.getName();
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Current password is incorrect");
        }
        
        if (request.getNewPassword().length() < 8) {
            return ResponseEntity.badRequest().body("Password must be at least 8 characters long");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
        
        return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully"));
    }
}
