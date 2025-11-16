package com.ft.searchengine.controller;


import com.ft.searchengine.dto.JobSearchRequest;
import com.ft.searchengine.dto.JobSearchResponse;
import com.ft.searchengine.service.JobSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@Slf4j
public class JobSearchController {

    @Autowired
    private JobSearchService jobSearchService;

    @GetMapping("/search")
    public ResponseEntity<JobSearchResponse> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer minSalary,
            @RequestParam(required = false) Integer maxSalary,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "postedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        log.info("📨 Received search request: keyword={}, location={}", keyword, location);

        JobSearchRequest request = new JobSearchRequest();
        request.setKeyword(keyword);
        request.setLocation(location);
        request.setMinSalary(minSalary);
        request.setMaxSalary(maxSalary);
        request.setExperienceLevel(experienceLevel);
        request.setIsActive(isActive);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);

        JobSearchResponse response = jobSearchService.searchJobs(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/clear-cache")
    public ResponseEntity<String> clearCache() {
        jobSearchService.clearCache();
        return ResponseEntity.ok("✅ Cache cleared successfully!");
    }
}