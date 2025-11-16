package com.ft.searchengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class JobSearchRequest {

    private String keyword;
    private List<String> skills;
    private String location;
    private Integer minSalary;
    private Integer maxSalary;
    private String experienceLevel;
    private Boolean isActive;

    // Pagination
    private Integer page = 0;
    private Integer size = 20;

    // Sorting
    private String sortBy = "postedDate";
    private String sortOrder = "desc";
}