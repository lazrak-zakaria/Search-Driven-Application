package com.ft.searchengine.service;

import com.ft.searchengine.entity.Job;
import com.ft.searchengine.repository.JobsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
//import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ch.qos.logback.core.util.StringUtil.capitalizeFirstLetter;

@Service
@Slf4j
public class DataImportService {

    @Autowired
    private JobsRepository jobRepository;

    public ImportResult importFromCsv(MultipartFile file) {
        ImportResult result = new ImportResult();

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader,
                     CSVFormat.DEFAULT
                             .withFirstRecordAsHeader()
                             .withIgnoreHeaderCase()
                             .withTrim())) {

            List<Job> batch = new ArrayList<>();
            int rowNumber = 0;

            for (CSVRecord record : csvParser) {
                rowNumber++;

                try {
                    Job job = parseJob(record);

                    // Validate job has minimum required fields
                    if (job.getTitle() == null || job.getTitle().isEmpty()) {
                        result.errorCount++;
                        result.errors.add("Row " + rowNumber + ": Missing title");
                        continue;
                    }

                    if (job.getCompany() == null || job.getCompany().isEmpty()) {
                        result.errorCount++;
                        result.errors.add("Row " + rowNumber + ": Missing company");
                        continue;
                    }

                    batch.add(job);


                    if (batch.size() >= 50) {
                        try {
                            jobRepository.saveAll(batch);
                            result.successCount += batch.size();
                            log.info("Imported {} jobs so far...", result.successCount);
                        } catch (Exception e) {
                            log.error("Failed to save batch: {}", e.getMessage());
                            result.errorCount += batch.size();
                            result.errors.add("Batch save failed: " + e.getMessage());
                        }
                        batch.clear();
                    }

                } catch (Exception e) {
                    result.errorCount++;
                    String errorMsg = "Row " + rowNumber + ": " + e.getMessage();
                    result.errors.add(errorMsg);
                    log.warn(errorMsg);
                }
            }


            if (!batch.isEmpty()) {
                try {
                    jobRepository.saveAll(batch);
                    result.successCount += batch.size();
                    log.info("Saved final batch of {} jobs", batch.size());
                } catch (Exception e) {
                    log.error("Failed to save final batch: {}", e.getMessage());
                    result.errorCount += batch.size();
                    result.errors.add("Final batch save failed: " + e.getMessage());
                }
            }

            log.info("Import completed: {} success, {} errors", result.successCount, result.errorCount);

        } catch (Exception e) {
            log.error("Import failed: {}", e.getMessage(), e);
            result.errors.add("Import failed: " + e.getMessage());
        }

        return result;
    }
    private String getField(CSVRecord record, String fieldName) {
        try {
            String value = record.get(fieldName);
            return value != null && !value.trim().isEmpty() ? value.trim() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            // Remove any non-numeric characters except minus
            String cleaned = value.replaceAll("[^0-9-]", "");
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Job parseJob(CSVRecord record) {
        Job job = new Job();

        // Map CSV columns to our Job entity
        job.setTitle(getField(record, "title"));
        job.setCompany(getField(record, "company_name"));
        job.setDescription(getField(record, "description"));
        job.setLocation(getField(record, "location"));
        String jobUrl = getField(record, "job_posting_url");
        if (jobUrl == null || jobUrl.isEmpty()) {
            jobUrl = getField(record, "application_url");
        }
        job.setJobUrl(jobUrl);

        job.setMinSalary(parseInteger(getField(record, "min_salary")));
        job.setMaxSalary(parseInteger(getField(record, "max_salary")));


        String skillsDesc = getField(record, "skills_desc");
        if (skillsDesc != null && !skillsDesc.isEmpty()) {

            List<String> skills = extractSkills(skillsDesc);
            job.setSkills(skills);
        } else {

            String description = getField(record, "description");
            if (description != null) {
                job.setSkills(extractSkillsFromDescription(description));
            }
        }

        // Parse experience level from formatted_experience_level
        String experienceLevel = getField(record, "formatted_experience_level");
        if (experienceLevel != null && !experienceLevel.isEmpty()) {
            job.setExperienceLevel(mapExperienceLevel(experienceLevel));
        } else {
            job.setExperienceLevel("Mid"); // Default
        }

        // Parse date from listed_time or original_listed_time
        String listedTime = getField(record, "listed_time");
        if (listedTime == null || listedTime.isEmpty()) {
            listedTime = getField(record, "original_listed_time");
        }

        if (listedTime != null && !listedTime.isEmpty()) {
            job.setPostedDate(parseDateFromTimestamp(listedTime));
        } else {
            job.setPostedDate(LocalDateTime.now());
        }

        // Check if job is active (not closed)
        String closedTime = getField(record, "closed_time");
        job.setIsActive(closedTime == null || closedTime.isEmpty());
        log.info("----------------------{}-----",job.getTitle());
        return job;
    }


    private List<String> extractSkills(String skillsDesc) {
        List<String> skills = new ArrayList<>();

        if (skillsDesc == null || skillsDesc.isEmpty()) {
            return skills;
        }

        // Split by common delimiters: comma, semicolon, pipe, newline
        String[] parts = skillsDesc.split("[,;|\\n\\r]+");

        for (String part : parts) {
            String cleaned = part.trim();

            // Only include if it's a reasonable length for a skill
            if (!cleaned.isEmpty() && cleaned.length() >= 2 && cleaned.length() <= 50) {
                // Capitalize first letter
                cleaned = capitalizeFirstLetter(cleaned);
                skills.add(cleaned);

                // Limit to 15 skills
                if (skills.size() >= 15) break;
            }
        }

        return skills;
    }


    private List<String> extractSkillsFromDescription(String description) {
        List<String> skills = new ArrayList<>();

        if (description == null || description.isEmpty()) {
            return skills;
        }


        String[] skillIndicators = {
                "experience with", "proficient in", "knowledge of", "skilled in",
                "expertise in", "familiar with", "competent in", "strong in",
                "ability to", "experience in", "background in", "understanding of",
                "must know", "required:", "requirements:", "qualifications:",
                "skills:", "abilities:", "competencies:"
        };

        String lowerDesc = description.toLowerCase();


        for (String indicator : skillIndicators) {
            int index = lowerDesc.indexOf(indicator);
            if (index != -1) {

                int endIndex = Math.min(index + indicator.length() + 200, description.length());
                String skillSection = description.substring(index + indicator.length(), endIndex);


                String[] parts = skillSection.split("[,;.\\n\\r]+");

                for (String part : parts) {
                    String cleaned = part.trim();


                    if (!cleaned.isEmpty() && cleaned.length() >= 3 && cleaned.length() <= 50) {
                        // Remove common filler words from the start
                        cleaned = cleaned.replaceFirst("^(and|or|the|a|an|in|with|using)\\s+", "");
                        cleaned = capitalizeFirstLetter(cleaned.trim());

                        if (cleaned.length() >= 3 && !skills.contains(cleaned)) {
                            skills.add(cleaned);
                            if (skills.size() >= 10) return skills;
                        }
                    }
                }
            }
        }

        // If no skills found through indicators, look for common job-related terms
        if (skills.isEmpty()) {
            skills = extractCommonJobTerms(description);
        }

        return skills;
    }

    // Extract common terms from ANY job description
    private List<String> extractCommonJobTerms(String description) {
        List<String> terms = new ArrayList<>();

        if (description == null || description.isEmpty()) {
            return terms;
        }


        String[] universalSkills = {

                "Communication", "Leadership", "Teamwork", "Problem Solving", "Time Management",
                "Organization", "Attention to Detail", "Customer Service", "Multitasking",
                "Critical Thinking", "Adaptability", "Collaboration", "Creativity",


                "Microsoft Office", "Excel", "Word", "PowerPoint", "Email", "Scheduling",
                "Data Entry", "Filing", "Phone Skills", "Calendar Management",

                // Sales/Marketing
                "Sales", "Marketing", "Social Media", "Advertising", "Cold Calling",
                "Negotiation", "Presentation", "Client Relations", "Account Management",

                // HR/Recruiting
                "Recruiting", "Interviewing", "Onboarding", "HR", "Benefits", "Payroll",
                "Employee Relations", "Training", "Performance Management",

                // Customer Service
                "Customer Support", "Help Desk", "Client Relations", "Complaint Resolution",
                "Call Center", "Chat Support", "Email Support",

                // Healthcare
                "Patient Care", "Medical Records", "HIPAA", "CPR", "First Aid",
                "Clinical", "Nursing", "Pharmaceutical", "Diagnosis", "Treatment",

                // Transportation/Logistics
                "Driving", "CDL", "Delivery", "Route Planning", "GPS", "Vehicle Maintenance",
                "Logistics", "Supply Chain", "Inventory", "Warehouse",

                // Hospitality
                "Customer Service", "Food Service", "Bartending", "Housekeeping",
                "Front Desk", "Reservations", "POS Systems", "Cash Handling",

                // Retail
                "Cash Register", "POS", "Inventory Management", "Visual Merchandising",
                "Stocking", "Loss Prevention", "Product Knowledge", "Upselling",

                // Manufacturing
                "Assembly", "Quality Control", "Machine Operation", "Forklift",
                "Safety Procedures", "Production", "Packaging", "Inspection",

                // Finance/Accounting
                "Accounting", "Bookkeeping", "QuickBooks", "Financial Analysis",
                "Budgeting", "Tax Preparation", "Auditing", "Accounts Payable",

                // Education
                "Teaching", "Curriculum Development", "Classroom Management",
                "Lesson Planning", "Student Assessment", "Tutoring", "Mentoring",

                // Tech (basic - not just programming)
                "Computer Skills", "Typing", "Internet", "Basic Troubleshooting",
                "Software", "Hardware", "Windows", "Mac", "Mobile Devices",

                // Legal
                "Legal Research", "Contract Review", "Compliance", "Documentation",
                "Litigation", "Paralegal", "Case Management",

                // Creative
                "Graphic Design", "Photography", "Video Editing", "Writing",
                "Copywriting", "Content Creation", "Adobe", "Photoshop", "Illustrator",

                // General Professional
                "Project Management", "Report Writing", "Meeting Facilitation",
                "Vendor Management", "Budget Management", "Process Improvement"
        };

        String lowerDesc = description.toLowerCase();

        for (String skill : universalSkills) {
            if (lowerDesc.contains(skill.toLowerCase())) {
                if (!terms.contains(skill)) {
                    terms.add(skill);
                    if (terms.size() >= 10) break;
                }
            }
        }

        return terms;
    }

    // Helper method to capitalize first letter
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // Map experience level from various formats
    private String mapExperienceLevel(String level) {
        if (level == null) return "Mid";

        String lower = level.toLowerCase();

        if (lower.contains("entry") || lower.contains("junior") || lower.contains("associate")) {
            return "Entry";
        } else if (lower.contains("senior") || lower.contains("sr.") || lower.contains("lead")) {
            return "Senior";
        } else if (lower.contains("mid") || lower.contains("intermediate")) {
            return "Mid";
        } else if (lower.contains("director") || lower.contains("manager") || lower.contains("principal")) {
            return "Lead";
        }

        return "Mid"; // Default
    }

    // Parse date from Unix timestamp
    private LocalDateTime parseDateFromTimestamp(String timestamp) {
        try {
            // Try parsing as Unix timestamp (milliseconds)
            long millis = Long.parseLong(timestamp);
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(millis),
                    java.time.ZoneId.systemDefault()
            );
        } catch (NumberFormatException e) {
            // Try parsing as ISO date
            try {
                return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                return LocalDateTime.now();
            }
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e2) {
                return LocalDateTime.now();
            }
        }
    }

    // Result class
    public static class ImportResult {
        public int successCount = 0;
        public int errorCount = 0;
        public List<String> errors = new ArrayList<>();
    }
}