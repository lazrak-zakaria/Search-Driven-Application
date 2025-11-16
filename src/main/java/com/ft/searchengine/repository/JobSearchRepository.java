package com.ft.searchengine.repository;




import com.ft.searchengine.document.JobDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobSearchRepository extends ElasticsearchRepository<JobDocument, String> {

    List<JobDocument> findByTitle(String title);

    List<JobDocument> findBySkillsContaining(String skill);

    List<JobDocument> findByLocation(String location);
}