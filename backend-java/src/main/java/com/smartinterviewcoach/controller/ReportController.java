package com.smartinterviewcoach.controller;

import com.smartinterviewcoach.security.AuthenticatedUser;
import com.smartinterviewcoach.service.GeminiService;
import com.smartinterviewcoach.service.SupabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final SupabaseService supabaseService;
    private final GeminiService geminiService;

    /**
     * GET /api/reports - Get all user reports
     */
    @GetMapping
    public ResponseEntity<Object> getAllReports(
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.notFound().build();

            List<Map<String, Object>> reports = supabaseService.getUserReports((String) profile.get("id"));
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("Error fetching reports", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch reports"));
        }
    }

    /**
     * GET /api/reports/{interviewId} - Get report for a specific interview
     */
    @GetMapping("/{interviewId}")
    public ResponseEntity<Map<String, Object>> getReport(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String interviewId
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.notFound().build();

            // Verify ownership
            Map<String, Object> interview = supabaseService.getInterview(interviewId);
            if (interview == null) return ResponseEntity.notFound().build();

            if (!profile.get("id").equals(interview.get("user_id"))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            Map<String, Object> report = supabaseService.getReport(interviewId);
            if (report == null) return ResponseEntity.notFound().build();

            // Include interview metadata
            report.put("interviewMeta", interview);

            // Include question-level evaluations
            List<Map<String, Object>> questions = supabaseService.getInterviewQuestions(interviewId);
            report.put("questions", questions);

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error fetching report for interview {}", interviewId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch report"));
        }
    }

    /**
     * POST /api/reports/{interviewId}/ideal-answer - Generate ideal answer for a question
     */
    @PostMapping("/{interviewId}/ideal-answer")
    public ResponseEntity<Map<String, Object>> generateIdealAnswer(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String interviewId,
        @RequestBody Map<String, String> request
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.badRequest().body(Map.of("error", "Profile not found"));

            Map<String, Object> interview = supabaseService.getInterview(interviewId);
            if (interview == null || !profile.get("id").equals(interview.get("user_id"))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            String question = request.get("question");
            if (question == null || question.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
            }

            String idealAnswer = geminiService.generateIdealAnswer(
                question,
                (String) interview.get("role"),
                (String) interview.get("interview_type")
            );

            return ResponseEntity.ok(Map.of("idealAnswer", idealAnswer));
        } catch (Exception e) {
            log.error("Error generating ideal answer", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to generate ideal answer"));
        }
    }

    /**
     * POST /api/reports/job-analysis - Analyze a job description
     */
    @PostMapping("/job-analysis")
    public ResponseEntity<Map<String, Object>> analyzeJobDescription(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, String> request
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.badRequest().body(Map.of("error", "Profile not found"));

            String jobDescription = request.get("jobDescription");
            if (jobDescription == null || jobDescription.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Job description is required"));
            }

            Object skillsObj = profile.get("skills");
            String skills = skillsObj != null ? skillsObj.toString() : "";

            Map<String, Object> analysis = geminiService.analyzeJobDescription(jobDescription, skills);
            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("Error analyzing job description", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to analyze job description"));
        }
    }
}
