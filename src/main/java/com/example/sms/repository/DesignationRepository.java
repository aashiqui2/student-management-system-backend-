package com.example.sms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.sms.entity.Designation;


public interface DesignationRepository extends JpaRepository<Designation, Long> {
    boolean existsByName(String name);
    java.util.Optional<Designation> findByName(String name);
}
