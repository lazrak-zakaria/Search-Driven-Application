package com.ft.searchengine.dto;



import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.ft.searchengine.document.JobDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class JobSearchResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<JobDocument> results;
    private long totalResults;
    private int currentPage;
    private int totalPages;
    private int pageSize;
    private long searchTimeMs;
    private boolean fromCache;
}