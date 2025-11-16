package com.ft.searchengine.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.ft.searchengine.repository")
public class ElasticsearchConfig {
}