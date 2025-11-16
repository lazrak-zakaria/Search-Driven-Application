package com.ft.searchengine.service;

import com.ft.searchengine.document.JobDocument;
import com.ft.searchengine.dto.JobSearchRequest;
import com.ft.searchengine.dto.JobSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JobSearchService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

//    @Cacheable(value = "jobSearch", key = "#request.toString()")
    public JobSearchResponse searchJobs(JobSearchRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("🔍 Searching with params: {}", request);
        log.info("⚠️ CACHE MISS - Querying Elasticsearch");

        // Build query
        Query query = buildQuery(request);

        // Execute search
        SearchHits<JobDocument> searchHits = elasticsearchOperations.search(
                query,
                JobDocument.class
        );

        // Extract results
        List<JobDocument> results = searchHits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        long totalHits = searchHits.getTotalHits();
        int totalPages = (int) Math.ceil((double) totalHits / request.getSize());
        long searchTime = System.currentTimeMillis() - startTime;

        log.info("✅ Found {} results in {}ms", totalHits, searchTime);

        return new JobSearchResponse(
                results,
                totalHits,
                request.getPage(),
                totalPages,
                request.getSize(),
                searchTime,
                false
        );
    }

    private Query buildQuery(JobSearchRequest request) {
        Criteria criteria = new Criteria();

        // Keyword search (in title, company, description)
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            Criteria keywordCriteria = new Criteria("title").contains(request.getKeyword())
                    .or("company").contains(request.getKeyword())
                    .or("description").contains(request.getKeyword());
            criteria = criteria.and(keywordCriteria);
        }

        // Filter by skills
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            criteria = criteria.and(new Criteria("skills").in(request.getSkills()));
        }

        // Filter by location
        if (request.getLocation() != null && !request.getLocation().isEmpty()) {
            criteria = criteria.and(new Criteria("location").is(request.getLocation()));
        }

        // Filter by salary range
        if (request.getMinSalary() != null) {
            criteria = criteria.and(new Criteria("maxSalary").greaterThanEqual(request.getMinSalary()));
        }

        if (request.getMaxSalary() != null) {
            criteria = criteria.and(new Criteria("minSalary").lessThanEqual(request.getMaxSalary()));
        }

        // Filter by experience level
        if (request.getExperienceLevel() != null && !request.getExperienceLevel().isEmpty()) {
            criteria = criteria.and(new Criteria("experienceLevel").is(request.getExperienceLevel()));
        }

        // Filter by active status
        if (request.getIsActive() != null) {
            criteria = criteria.and(new Criteria("isActive").is(request.getIsActive()));
        }

        // Build query with pagination and sorting
        Sort.Direction direction = request.getSortOrder().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        PageRequest pageRequest = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(direction, request.getSortBy())
        );

        return new CriteriaQuery(criteria).setPageable(pageRequest);
    }

    @CacheEvict(value = "jobSearch", allEntries = true)
    public void clearCache() {
        log.info("🗑️ All search cache cleared!");
    }
}