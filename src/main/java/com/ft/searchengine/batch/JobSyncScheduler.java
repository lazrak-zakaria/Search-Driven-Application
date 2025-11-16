package com.ft.searchengine.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JobSyncScheduler {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job syncJobsToElasticsearch;

//    // Run every hour (3600000 milliseconds = 1 hour)
    @Scheduled(fixedRate = 3600000)
    //  every 2 minutes for testing (120000 milliseconds)
//    @Scheduled(fixedRate = 120000)
    public void scheduledSync() {
        try {
            log.info("Scheduled sync starting...");

            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(syncJobsToElasticsearch, params);

            log.info("Scheduled sync completed successfully!");

        } catch (Exception e) {
            log.error("Scheduled sync failed: {}", e.getMessage(), e);
        }
    }


    // @Scheduled(cron = "0 0 2 * * ?")
    // public void dailySync() {
    //     scheduledSync();
    // }
}