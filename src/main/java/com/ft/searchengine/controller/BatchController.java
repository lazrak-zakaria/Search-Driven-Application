package com.ft.searchengine.controller;


import com.ft.searchengine.batch.JobSyncScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
@Slf4j
public class BatchController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job syncJobsToElasticsearch;

    @PostMapping("/sync")
    public ResponseEntity<String> triggerSync() {
        try {
            log.info("Starting batch sync job...");

            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(syncJobsToElasticsearch, params);

            log.info("✅ Batch sync completed successfully!");
            return ResponseEntity.ok("Sync job started successfully!");

        } catch (Exception e) {
            log.error("Batch sync failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Sync failed: " + e.getMessage());
        }
    }


    @Autowired
    private JobSyncScheduler jobSyncScheduler;

    @GetMapping("/status")
    public ResponseEntity<String> getBatchStatus() {
        return ResponseEntity.ok("Batch job is scheduled to run every hour");
    }

    @PostMapping("/sync-now")
    public ResponseEntity<String> syncNow() {
        try {
            log.info("🔄 Manual sync triggered via API...");
            jobSyncScheduler.scheduledSync();
            return ResponseEntity.ok("✅ Manual sync completed!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("❌ Manual sync failed: " + e.getMessage());
        }
    }
}