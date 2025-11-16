package com.ft.searchengine.controller;


import com.ft.searchengine.document.JobDocument;
import com.ft.searchengine.dto.JobSearchRequest;
import com.ft.searchengine.dto.JobSearchResponse;
import com.ft.searchengine.entity.Job;
import com.ft.searchengine.repository.JobsRepository;
import com.ft.searchengine.repository.JobSearchRepository;
import com.ft.searchengine.service.JobSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/test")
@Slf4j
public class TestController {

    @Autowired
    private JobsRepository jobRepository;

    @Autowired
    private JobSearchRepository jobSearchRepository;



    @Autowired
    private JobSearchService jobSearchService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/hello")
    public String hello() {
        return "JobSeekPro is running!";
    }

    @PostMapping("/create-job")
    public Job createTestJob() {
        // Only save to PostgreSQL
        Job job = new Job();
        job.setTitle("Senior Java Developer");
        job.setCompany("TechCorp");
        job.setDescription("Looking for experienced Java developer with Spring Boot");
        job.setLocation("San Francisco, CA");
        job.setMinSalary(120000);
        job.setMaxSalary(180000);
        job.setSkills(Arrays.asList("Java", "Spring Boot", "Microservices"));
        job.setExperienceLevel("Senior");
        job.setPostedDate(LocalDateTime.now());
        job.setIsActive(true);

        Job savedJob = jobRepository.save(job);

//        log.info("✅ Job created in PostgreSQL only (ID: {})", savedJob.getId());

        return savedJob;
    }

    @GetMapping("/all-jobs")
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/search-jobs")
    public List<JobDocument> searchJobs(@RequestParam String keyword) {
        return jobSearchRepository.findByTitle(keyword);
    }

    @GetMapping("/count")
    public String getCounts() {
        long postgresCount = jobRepository.count();
        long elasticsearchCount = jobSearchRepository.count();

        return "PostgreSQL: " + postgresCount + " jobs | Elasticsearch: " + elasticsearchCount + " jobs";
    }




    @GetMapping("/search-cached")
    public JobSearchResponse searchWithCache(@RequestParam String keyword) {
        JobSearchRequest request = new JobSearchRequest();
        request.setKeyword(keyword);
        request.setPage(0);
        request.setSize(20);
        return jobSearchService.searchJobs(request);
    }

    @GetMapping("/clear-cache")
    public String clearCache() {
        jobSearchService.clearCache();
        return "✅ Cache cleared!";
    }

    @GetMapping("/cache-stats")
    public String getCacheStats() {
        // Get Redis info
        try {
            Long dbSize = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .dbSize();

            return "Redis has " + dbSize + " keys in cache";
        } catch (Exception e) {
            return "Error getting cache stats: " + e.getMessage();
        }
    }

    @PostMapping("/create-multiple-jobs")
    public String createMultipleJobs() {
        String[] titles = {"Java Developer", "Python Developer", "React Developer", "DevOps Engineer", "Data Scientist"};
        String[] companies = {"TechCorp", "DataCo", "WebSolutions", "CloudSys", "AI Labs"};
        String[] locations = {"San Francisco, CA", "New York, NY", "Remote", "Austin, TX", "Seattle, WA"};
        String[][] skills = {
                {"Java", "Spring Boot", "Microservices"},
                {"Python", "Django", "FastAPI"},
                {"React", "JavaScript", "TypeScript"},
                {"Docker", "Kubernetes", "AWS"},
                {"Python", "TensorFlow", "Machine Learning"}
        };

        int count = 0;
        for (int i = 0; i < titles.length; i++) {
            // Save to PostgreSQL ONLY
            Job job = new Job();
            job.setTitle(titles[i]);
            job.setCompany(companies[i]);
            job.setDescription("Great opportunity for " + titles[i]);
            job.setLocation(locations[i]);
            job.setMinSalary(80000 + (i * 10000));
            job.setMaxSalary(120000 + (i * 20000));
            job.setSkills(Arrays.asList(skills[i]));
            job.setExperienceLevel("Mid");
            job.setPostedDate(LocalDateTime.now());
            job.setIsActive(true);

            jobRepository.save(job);
            count++;
        }

        log.info("✅ Created {} jobs in PostgreSQL only", count);

        return "✅ Created " + count + " jobs in PostgreSQL only (not synced to Elasticsearch yet)";
    }
}
