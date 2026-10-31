package com.smartinterviewcoach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SupabaseService {

    private final WebClient supabaseClient;
    private final ObjectMapper objectMapper;

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    public SupabaseService(@Qualifier("supabaseWebClient") WebClient supabaseClient,
                           ObjectMapper objectMapper) {
        this.supabaseClient = supabaseClient;
        this.objectMapper = objectMapper;
    }

    // =================== PROFILES ===================

    public Map<String, Object> getProfileBySupabaseUid(String supabaseUid) {
        List<Map<String, Object>> results = supabaseClient.get()
            .uri("/rest/v1/profiles?supabase_uid=eq." + supabaseUid + "&select=*")
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();

        return (results != null && !results.isEmpty()) ? results.get(0) : null;
    }

    public Map<String, Object> upsertProfile(Map<String, Object> profileData) {
        List<Map<String, Object>> result = supabaseClient.post()
            .uri("/rest/v1/profiles")
            .header("Prefer", "resolution=merge-duplicates,return=representation")
            .bodyValue(profileData)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();

        return (result != null && !result.isEmpty()) ? result.get(0) : null;
    }

    public Map<String, Object> updateProfile(String profileId, Map<String, Object> updates) {
        List<Map<String, Object>> result = supabaseClient.patch()
            .uri("/rest/v1/profiles?id=eq." + profileId)
            .bodyValue(updates)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();

        return (result != null && !result.isEmpty()) ? result.get(0) : null;
    }

    // =================== RESUMES ===================

    public Map<String, Object> createResume(Map<String, Object> resumeData) {
        List<Map<String, Object>> result = supabaseClient.post()
            .uri("/rest/v1/resumes")
            .bodyValue(resumeData)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();

        return (result != null && !result.isEmpty()) ? result.get(0) : null;
    }

    public void updateResume(String resumeId, Map<String, Object> updates) {
        supabaseClient.patch()
            .uri("/rest/v1/resumes?id=eq." + resumeId)
            .bodyValue(updates)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    // =================== INTERVIEWS ===================

    public Map<String, Object> createInterview(Map<String, Object> interviewData) {
        List<Map<String, Object>> result = supabaseClient.post()
            .uri("/rest/v1/interviews")
            .bodyValue(interviewData)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();

        return (result != null && !result.isEmpty()) ? result.get(0) : null;
    }

    public Map<String, Object> getInterview(String interviewId) {
        List<Map<String, Object>> results = supabaseClient.get()
            .uri("/rest/v1/interviews?id=eq." + interviewId + "&select=*")
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();

        return (results != null && !results.isEmpty()) ? results.get(0) : null;
    }

    public List<Map<String, Object>> getUserInterviews(String userId, int page, int pageSize) {
        int from = page * pageSize;
        int to = from + pageSize - 1;
        return supabaseClient.get()
            .uri("/rest/v1/interviews?user_id=eq." + userId
                + "&select=*&order=created_at.desc"
                + "&offset=" + from + "&limit=" + pageSize)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();
    }

    public void updateInterview(String interviewId, Map<String, Object> updates) {
        supabaseClient.patch()
            .uri("/rest/v1/interviews?id=eq." + interviewId)
            .bodyValue(updates)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    // =================== INTERVIEW QUESTIONS ===================

    public List<Map<String, Object>> createInterviewQuestions(List<Map<String, Object>> questions) {
        return supabaseClient.post()
            .uri("/rest/v1/interview_questions")
            .bodyValue(questions)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();
    }

    public List<Map<String, Object>> getInterviewQuestions(String interviewId) {
        return supabaseClient.get()
            .uri("/rest/v1/interview_questions?interview_id=eq." + interviewId
                + "&select=*&order=position.asc")
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();
    }

    public void updateQuestion(String questionId, Map<String, Object> updates) {
        supabaseClient.patch()
            .uri("/rest/v1/interview_questions?id=eq." + questionId)
            .bodyValue(updates)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    // =================== REPORTS ===================

    public Map<String, Object> createReport(Map<String, Object> reportData) {
        List<Map<String, Object>> result = supabaseClient.post()
            .uri("/rest/v1/reports")
            .bodyValue(reportData)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();

        return (result != null && !result.isEmpty()) ? result.get(0) : null;
    }

    public Map<String, Object> getReport(String interviewId) {
        List<Map<String, Object>> results = supabaseClient.get()
            .uri("/rest/v1/reports?interview_id=eq." + interviewId + "&select=*")
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();

        return (results != null && !results.isEmpty()) ? results.get(0) : null;
    }

    public List<Map<String, Object>> getUserReports(String userId) {
        return supabaseClient.get()
            .uri("/rest/v1/reports?user_id=eq." + userId
                + "&select=*,interviews(role,interview_type,difficulty,created_at)&order=created_at.desc")
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();
    }

    // =================== ACTIVITY ===================

    public List<Map<String, Object>> getPracticeActivity(String userId, int weeks) {
        return supabaseClient.get()
            .uri("/rest/v1/practice_activity?user_id=eq." + userId
                + "&select=*&order=activity_date.desc&limit=" + (weeks * 7))
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();
    }

    public void upsertActivity(Map<String, Object> activityData) {
        supabaseClient.post()
            .uri("/rest/v1/practice_activity")
            .header("Prefer", "resolution=merge-duplicates,return=representation")
            .bodyValue(activityData)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    // =================== GOALS ===================

    public List<Map<String, Object>> getUserGoals(String userId) {
        return supabaseClient.get()
            .uri("/rest/v1/goals?user_id=eq." + userId + "&select=*")
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();
    }

    public Map<String, Object> createGoal(Map<String, Object> goalData) {
        List<Map<String, Object>> result = supabaseClient.post()
            .uri("/rest/v1/goals")
            .bodyValue(goalData)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();

        return (result != null && !result.isEmpty()) ? result.get(0) : null;
    }

    // =================== NOTIFICATIONS ===================

    public List<Map<String, Object>> getUserNotifications(String userId, boolean unreadOnly) {
        String uri = "/rest/v1/notifications?user_id=eq." + userId
            + "&select=*&order=created_at.desc&limit=20";
        if (unreadOnly) uri += "&read_at=is.null";

        return supabaseClient.get()
            .uri(uri)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .collectList()
            .block();
    }

    public void markNotificationsRead(String userId) {
        supabaseClient.patch()
            .uri("/rest/v1/notifications?user_id=eq." + userId + "&read_at=is.null")
            .bodyValue(Map.of("read_at", java.time.Instant.now().toString()))
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    // =================== STATS ===================

    public Map<String, Object> getUserStats(String userId) {
        // Fetch aggregate stats
        String interviewStats = supabaseClient.get()
            .uri("/rest/v1/interviews?user_id=eq." + userId
                + "&select=overall_score,status,created_at&status=eq.completed")
            .retrieve()
            .bodyToMono(String.class)
            .block();

        try {
            List<Map<String, Object>> interviews = objectMapper.readValue(
                interviewStats != null ? interviewStats : "[]",
                new TypeReference<>() {}
            );

            int total = interviews.size();
            double avgScore = interviews.stream()
                .filter(i -> i.get("overall_score") != null)
                .mapToDouble(i -> ((Number) i.get("overall_score")).doubleValue())
                .average()
                .orElse(0);
            double bestScore = interviews.stream()
                .filter(i -> i.get("overall_score") != null)
                .mapToDouble(i -> ((Number) i.get("overall_score")).doubleValue())
                .max()
                .orElse(0);

            return Map.of(
                "totalInterviews", total,
                "averageScore", Math.round(avgScore),
                "bestScore", Math.round(bestScore),
                "improvement", total > 1 ? Math.round(avgScore - 50) : 0
            );
        } catch (Exception e) {
            log.error("Failed to parse interview stats", e);
            return Map.of("totalInterviews", 0, "averageScore", 0, "bestScore", 0, "improvement", 0);
        }
    }

    // =================== STORAGE ===================

    public String uploadResume(String userId, String fileName, byte[] fileBytes, String contentType) {
        String path = userId + "/" + fileName;
        supabaseClient.post()
            .uri("/storage/v1/object/resumes/" + path)
            .header("Content-Type", contentType)
            .bodyValue(fileBytes)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
        return path;
    }
}
