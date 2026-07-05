package com.example.sms.security;

import com.example.sms.entity.User;
import com.example.sms.enums.Role;
import com.example.sms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.sms.entity.StaffProfile;
import com.example.sms.entity.Student;
import com.example.sms.repository.StaffProfileRepository;
import com.example.sms.repository.StudentRepository;
import com.example.sms.repository.DesignationRepository;
import com.example.sms.entity.Designation;
import java.util.List;
import java.util.Arrays;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StaffProfileRepository staffProfileRepository;
    private final StudentRepository studentRepository;
    private final DesignationRepository designationRepository;

    public DatabaseSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, StaffProfileRepository staffProfileRepository, StudentRepository studentRepository, DesignationRepository designationRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.staffProfileRepository = staffProfileRepository;
        this.studentRepository = studentRepository;
        this.designationRepository = designationRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        List<String> defaultDesignations = Arrays.asList("College Faculty", "Technical Trainer", "Aptitude Trainer", "Soft Skill Trainer");
        for (String designationName : defaultDesignations) {
            if (designationRepository.findByName(designationName).isEmpty()) {
                designationRepository.save(new Designation(designationName));
                System.out.println("Default designation created: " + designationName);
            }
        }

        User admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername("admin");
            newUser.setPassword(passwordEncoder.encode("admin123"));
            newUser.setRole(Role.ADMIN);
            System.out.println("Default admin user created with username: admin and password: admin123");
            return userRepository.save(newUser);
        });
        
        if (staffProfileRepository.findByUser(admin).isEmpty()) {
            StaffProfile adminProfile = new StaffProfile();
            adminProfile.setUser(admin);
            adminProfile.setName("Admin");
            adminProfile.setEmail(null);
            adminProfile.setEmployeeId(null);
            adminProfile.setStream(null);
            adminProfile.setSpecialization(null);
            staffProfileRepository.save(adminProfile);
            System.out.println("Default admin profile created");
        }
        
        /*
        User staff = userRepository.findByUsername("staff").orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername("staff");
            newUser.setPassword(passwordEncoder.encode("staff123"));
            newUser.setRole(Role.STAFF);
            System.out.println("Default staff user created");
            return userRepository.save(newUser);
        });
        
        if (staffProfileRepository.findByUser(staff).isEmpty()) {
            StaffProfile staffProfile = new StaffProfile();
            staffProfile.setUser(staff);
            staffProfile.setName("Default Staff");
            staffProfile.setEmail(null);
            
            long count = staffProfileRepository.count();
            String formattedId = String.format("STF%03d", count + 1);
            while (staffProfileRepository.findByEmployeeId(formattedId).isPresent()) {
                count++;
                formattedId = String.format("STF%03d", count + 1);
            }
            staffProfile.setEmployeeId(formattedId);
            
            staffProfile.setStream(null);
            staffProfile.setSpecialization(null);
            staffProfileRepository.save(staffProfile);
            System.out.println("Default staff profile created");
        }
        
        User studentUser = userRepository.findByUsername("student").orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername("student");
            newUser.setPassword(passwordEncoder.encode("student123"));
            newUser.setRole(Role.STUDENT);
            System.out.println("Default student user created");
            return userRepository.save(newUser);
        });
        
        if (studentRepository.findByUser(studentUser).isEmpty()) {
            Student studentProfile = new Student();
            studentProfile.setUser(studentUser);
            studentProfile.setName("Default Student");
            studentProfile.setRegNo(null);
            studentProfile.setEmail(null);
            studentProfile.setStream(null);
            studentProfile.setSpecialization(null);
            studentRepository.save(studentProfile);
            System.out.println("Default student profile created");
        }
        */

        // Backfill users for existing students uploaded without a User
        List<Student> allStudents = studentRepository.findAll();
        for (Student s : allStudents) {
            if (s.getUser() == null && s.getRegNo() != null && !s.getRegNo().isEmpty()) {
                User u = userRepository.findByUsername(s.getRegNo()).orElse(null);
                if (u == null) {
                    u = new User();
                    u.setUsername(s.getRegNo());
                    u.setPassword(passwordEncoder.encode("student123"));
                    u.setRole(Role.STUDENT);
                    userRepository.save(u);
                }
                s.setUser(u);
                studentRepository.save(s);
                System.out.println("Backfilled user for student: " + s.getRegNo());
            }
        }
    }
}
