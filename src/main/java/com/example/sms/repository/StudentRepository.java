package com.example.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.sms.entity.StaffProfile;
import com.example.sms.entity.Student;
import com.example.sms.entity.User;


public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRegNo(String regNo);
    Optional<Student> findByRegNoIgnoreCase(String regNo);
    Optional<Student> findByEmail(String email);
    Optional<Student> findByUserUsername(String username);
    Optional<Student> findByUser(User user);
    List<Student> findByAssignedStaff(StaffProfile staff);
    List<Student> findByAssignedStaffIsNull();
}
