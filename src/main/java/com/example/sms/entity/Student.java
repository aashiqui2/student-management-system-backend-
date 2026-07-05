package com.example.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @Column(unique = true)
    private String regNo;

    @Email(message = "Invalid email format")
    @Column(unique = true)
    private String email;

    @Pattern(regexp = "^([0-9]{10})?$", message = "Mobile number must be 10 digits if provided")
    private String mobileNumber;

    private String stream;
    private String department;
    private String specialization;

    private String hackerRankUsername;

    private Integer startYear;
    private Integer courseDuration;

    private String linkedInUrl;
    
    private String githubUrl;
    
    private String leetcodeUrl;

    private String profilePicUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private Integer totalMarks = 0;
    
    private Double averageMarks = 0.0;
    
    private String category;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<StudentAssessmentMark> marks;

    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    @JsonIgnore
    private StaffProfile assignedStaff;

}
