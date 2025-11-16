package com.ft.searchengine.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(length = 5000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "job_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill")
    private List<String> skills;

    private String location;

    private Integer minSalary;

    private Integer maxSalary;

    private String experienceLevel;

    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime postedDate;

    @Column(nullable = false)
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        if (postedDate == null) {
            postedDate = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}