package com.ft.searchengine.document;



import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "jobs")
@Setting(settingPath = "elasticsearch-settings.json")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class JobDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String company;

    @Field(type = FieldType.Text, analyzer = "english")
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> skills;

    @Field(type = FieldType.Keyword)
    private String location;

    @Field(type = FieldType.Integer)
    private Integer minSalary;

    @Field(type = FieldType.Integer)
    private Integer maxSalary;

    @Field(type = FieldType.Keyword)
    private String experienceLevel;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime postedDate;

    @Field(type = FieldType.Boolean)
    private Boolean isActive;
}