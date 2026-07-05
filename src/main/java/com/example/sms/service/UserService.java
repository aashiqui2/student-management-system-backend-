package com.example.sms.service;

import com.example.sms.dto.UserDto;
import com.example.sms.entity.Student;
import com.example.sms.entity.User;
import com.example.sms.enums.Role;
import com.example.sms.repository.UserRepository;
import com.example.sms.repository.StaffProfileRepository;
import com.example.sms.entity.StaffProfile;
import com.example.sms.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final StudentRepository studentRepository;

    public UserService(UserRepository userRepository, StaffProfileRepository staffProfileRepository, StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.studentRepository = studentRepository;
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public void updateUser(Long id, Map<String, String> body) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (body.containsKey("username")) {
            user.setUsername(body.get("username"));
        }
        if (body.containsKey("enabled")) {
            user.setEnabled(Boolean.parseBoolean(body.get("enabled")));
        }
        
        if (body.containsKey("fullName") || body.containsKey("email") || body.containsKey("stream") || body.containsKey("department") || body.containsKey("specialization") || body.containsKey("regNo")) {
            Student student = user.getStudentProfile();
            StaffProfile staff = user.getStaffProfile();
            
            if (user.getRole() == Role.STUDENT) {
                if (student == null) {
                    student = new Student();
                    student.setUser(user);
                    student.setRegNo(null);
                    student.setStream(null);
                    student.setSpecialization(null);
                    student.setEmail(null);
                    user.setStudentProfile(student);
                }
                if (body.containsKey("fullName")) student.setName(body.get("fullName"));
                if (body.containsKey("email")) student.setEmail(body.get("email"));
                if (body.containsKey("stream")) student.setStream(body.get("stream"));
                if (body.containsKey("department")) student.setDepartment(body.get("department"));
                if (body.containsKey("specialization")) student.setSpecialization(body.get("specialization"));
                if (body.containsKey("regNo")) student.setRegNo(body.get("regNo"));
                studentRepository.save(student);
            } else {
                if (staff == null) {
                    staff = new StaffProfile();
                    staff.setUser(user);
                    staff.setEmployeeId(null);
                    staff.setStream(null);
                    staff.setSpecialization(null);
                    staff.setEmail(null);
                    user.setStaffProfile(staff);
                }
                if (body.containsKey("fullName")) staff.setName(body.get("fullName"));
                if (body.containsKey("email")) staff.setEmail(body.get("email"));
                if (body.containsKey("stream")) staff.setStream(body.get("stream"));
                if (body.containsKey("department")) staff.setDepartment(body.get("department"));
                if (body.containsKey("specialization")) staff.setSpecialization(body.get("specialization"));
                
                if (staff.getEmployeeId() == null) {
                    long count = staffProfileRepository.count();
                    String formattedId = String.format("STF%03d", count + 1);
                    while (staffProfileRepository.findByEmployeeId(formattedId).isPresent()) {
                        count++;
                        formattedId = String.format("STF%03d", count + 1);
                    }
                    staff.setEmployeeId(formattedId);
                }
                
                staffProfileRepository.save(staff);
            }
        }
        userRepository.save(user);
    }

    public void changeRole(Long id, Role role) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(role);
        userRepository.save(user);
        
        if (role == Role.STAFF) {
            if (staffProfileRepository.findByUser(user).isEmpty()) {
                StaffProfile profile = new StaffProfile();
                profile.setUser(user);
                profile.setName(user.getUsername());
                // Generate employee ID automatically (Staff ID)
                long count = staffProfileRepository.count();
                String formattedId = String.format("STF%03d", count + 1);
                while (staffProfileRepository.findByEmployeeId(formattedId).isPresent()) {
                    count++;
                    formattedId = String.format("STF%03d", count + 1);
                }
                profile.setEmployeeId(formattedId);
                staffProfileRepository.save(profile);
            }
        } else if (role == Role.STUDENT) {
            if (studentRepository.findByUser(user).isEmpty()) {
                // Check if a student profile with regNo = username exists
                Optional<Student> existingStudentOpt = studentRepository.findByRegNo(user.getUsername());
                if (existingStudentOpt.isPresent()) {
                    Student student = existingStudentOpt.get();
                    student.setUser(user);
                    studentRepository.save(student);
                } else {
                    // Create a new basic student profile
                    Student student = new Student();
                    student.setUser(user);
                    student.setName(user.getUsername());
                    student.setRegNo(user.getUsername());
                    // Generate a unique email
                    String email = user.getUsername() + "@school.com";
                    int suffix = 1;
                    while (studentRepository.findByEmail(email).isPresent()) {
                        email = user.getUsername() + suffix + "@school.com";
                        suffix++;
                    }
                    student.setEmail(email);
                    studentRepository.save(student);
                }
            }
        }
    }

    public void toggleStatus(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        boolean newStatus = !user.isEnabled();
        user.setEnabled(newStatus);
        
        if (newStatus && user.getRole() == Role.STAFF) {
            StaffProfile profile = staffProfileRepository.findByUser(user).orElse(null);
            if (profile != null && profile.getJoiningDate() == null) {
                profile.setJoiningDate(java.time.LocalDate.now());
                staffProfileRepository.save(profile);
            }
        }
        
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Cannot delete an Admin account");
        }
        userRepository.delete(user);
    }

    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole().name());
        dto.setEnabled(user.isEnabled());
        
        Student student = user.getStudentProfile();
        if (student != null) {
            dto.setFullName(student.getName());
            dto.setRegNo(student.getRegNo());
            dto.setStream(student.getStream());
            dto.setDepartment(student.getDepartment());
            dto.setSpecialization(student.getSpecialization());
            dto.setEmail(student.getEmail());
            dto.setProfilePicUrl(student.getProfilePicUrl());
            dto.setDateRegistered(student.getCreatedAt());
        } else {
            StaffProfile staff = user.getStaffProfile();
            if (staff != null) {
                dto.setFullName(staff.getName());
                dto.setRegNo(staff.getEmployeeId()); // Using employeeId as the Reg No/ID identifier
                dto.setStream(staff.getStream());
                dto.setDepartment(staff.getDepartment());
                dto.setSpecialization(staff.getSpecialization());
                dto.setEmail(staff.getEmail());
                if (staff.getProfilePhotoUrl() != null && !staff.getProfilePhotoUrl().isEmpty()) {
                    dto.setProfilePicUrl("/api/staff/" + staff.getId() + "/photo");
                }
                dto.setDateRegistered(staff.getCreatedAt());
            }
        }
        
        return dto;
    }
}
