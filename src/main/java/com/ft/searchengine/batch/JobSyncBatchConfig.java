package com.ft.searchengine.batch;



import com.ft.searchengine.document.JobDocument;
import com.ft.searchengine.entity.Job;
import com.ft.searchengine.repository.JobSearchRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Slf4j
public class JobSyncBatchConfig {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JobSearchRepository jobSearchRepository;

    // Step 1: Read jobs from PostgreSQL
    @Bean
    public JpaPagingItemReader<Job> jobReader() {
        return new JpaPagingItemReaderBuilder<Job>()
                .name("jobReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT j FROM Job j WHERE j.isActive = true")
                .pageSize(100)
                .build();
    }

    // Step 2: Process (convert Job to JobDocument)
    @Bean
    public ItemProcessor<Job, JobDocument> jobProcessor() {
        return job -> {
            log.debug("Processing job: {}", job.getId());

            JobDocument doc = new JobDocument();
            doc.setId(job.getId().toString());
            doc.setTitle(job.getTitle());
            doc.setCompany(job.getCompany());
            doc.setDescription(job.getDescription());
            doc.setSkills(job.getSkills());
            doc.setLocation(job.getLocation());
            doc.setMinSalary(job.getMinSalary());
            doc.setMaxSalary(job.getMaxSalary());
            doc.setExperienceLevel(job.getExperienceLevel());
            doc.setPostedDate(job.getPostedDate());
            doc.setIsActive(job.getIsActive());

            return doc;
        };
    }

    // Step 3: Write to Elasticsearch
    @Bean
    public ItemWriter<JobDocument> jobWriter() {
        return items -> {
            log.info("Writing {} jobs to Elasticsearch", items.size());
            jobSearchRepository.saveAll(items);
        };
    }

    // Define the Step
    @Bean
    public Step syncJobsStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager) {
        return new StepBuilder("syncJobsStep", jobRepository)
                .<Job, JobDocument>chunk(100, transactionManager)
                .reader(jobReader())
                .processor(jobProcessor())
                .writer(jobWriter())
                .build();
    }

    // Define the Job
    @Bean
    public org.springframework.batch.core.Job syncJobsToElasticsearch(JobRepository jobRepository,
                                       Step syncJobsStep) {
        return new JobBuilder("syncJobsToElasticsearch", jobRepository)
                .start(syncJobsStep)
                .build();
    }
}