package com.ft.searchengine.repository;


import com.ft.searchengine.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // it seems i need to change the name of this // job i guess a reserved bean on sprinbg
public interface JobsRepository extends JpaRepository<Job, Long> {

    List<Job> findByIsActiveTrue();

    List<Job> findByCompany(String company);

    List<Job> findByLocationContaining(String location);
}