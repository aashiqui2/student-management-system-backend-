package com.example.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.sms.entity.StaffProfile;
import com.example.sms.entity.User;


public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    Optional<StaffProfile> findByUser(User user);
    Optional<StaffProfile> findByEmployeeId(String employeeId);
    
    @Query("SELECT sp FROM StaffProfile sp JOIN sp.user u WHERE u.role = com.example.sms.enums.Role.STAFF")
    List<StaffProfile> findAllByUserRoleStaff();
}
