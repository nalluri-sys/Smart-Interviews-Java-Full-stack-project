package com.smartinterviewcoach.controller;

import com.smartinterviewcoach.security.AuthenticatedUser;
import com.smartinterviewcoach.service.GeminiService;
import com.smartinterviewcoach.service.SupabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final SupabaseService supabaseService;
    private final GeminiService geminiService;

    /**
     * POST /api/interviews - Create and start a new interview session
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createInterview(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Object> request
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Profile not found"));
            }

            String userId = (String) profile.get("id");
            String role = (String) request.getOrDefault("role", profile.get("target_role"));
            String interviewType = (String) request.getOrDefault("interviewType", "Technical");
            String difficulty = (String) request.getOrDefault("difficulty", "Intermediate");
            int questionCount = (int) request.getOrDefault("questionCount", 8);
            String language = (String) request.getOrDefault("language", "English");

            // Extract skills for question generation
            Object skillsObj = profile.get("skills");
            String skills = skillsObj != null ? skillsObj.toString() : "Java, Spring Boot";

            Object aiProfileObj = profile.get("ai_profile");
            String resumeSummary = "";
            if (aiProfileObj instanceof Map aiProfile) {
                Object summary = aiProfile.get("candidateSummary");
                if (summary != null) resumeSummary = summary.toString();
            }

            // Create interview record
            Map<String, Object> interviewData = new HashMap<>();
            interviewData.put("user_id", userId);
            interviewData.put("role", role);
            interviewData.put("interview_type", interviewType);
            interviewData.put("difficulty", difficulty);
            interviewData.put("experience_level", profile.get("experience_level"));
            interviewData.put("language", language);
            interviewData.put("question_count", questionCount);
            interviewData.put("status", "ready");
            interviewData.put("started_at", Instant.now().toString());
            interviewData.put("created_at", Instant.now().toString());

            Map<String, Object> interview = supabaseService.createInterview(interviewData);
            if (interview == null) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create interview"));
            }

            String interviewId = (String) interview.get("id");

            // Generate questions via Gemini
            List<Map<String, Object>> generatedQuestions = geminiService.generateQuestions(
                role, interviewType, difficulty, questionCount, skills, resumeSummary);

            // Save questions to Supabase
            List<Map<String, Object>> questionsToSave = new ArrayList<>();
            for (int i = 0; i < generatedQuestions.size(); i++) {
                Map<String, Object> q = generatedQuestions.get(i);
                Map<String, Object> questionData = new HashMap<>();
                questionData.put("interview_id", interviewId);
                questionData.put("position", i + 1);
                questionData.put("question_text", q.getOrDefault("questionText", q.getOrDefault("question", "")));
                questionData.put("category", q.getOrDefault("category", interviewType));
                questionData.put("difficulty", q.getOrDefault("difficulty", difficulty));
                questionData.put("expected_topics", q.get("expectedTopics"));
                questionData.put("created_at", Instant.now().toString());
                questionsToSave.add(questionData);
            }

            List<Map<String, Object>> savedQuestions = supabaseService.createInterviewQuestions(questionsToSave);

            // Update interview status to in_progress
            supabaseService.updateInterview(interviewId, Map.of("status", "in_progress"));

            return ResponseEntity.ok(Map.of(
                "interview", interview,
                "questions", savedQuestions != null ? savedQuestions : questionsToSave
            ));

        } catch (Exception e) {
            log.error("Error creating interview", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to create interview: " + e.getMessage()));
        }
    }

    /**
     * GET /api/interviews - Get paginated list of user's interviews
     */
    @GetMapping
    public ResponseEntity<Object> getInterviews(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.notFound().build();

            List<Map<String, Object>> interviews = supabaseService.getUserInterviews(
                (String) profile.get("id"), page, size);
            return ResponseEntity.ok(interviews);
        } catch (Exception e) {
            log.error("Error fetching interviews", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch interviews"));
        }
    }

    /**
     * GET /api/interviews/{id} - Get interview with questions
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getInterview(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String id
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.notFound().build();

            Map<String, Object> interview = supabaseService.getInterview(id);
            if (interview == null) return ResponseEntity.notFound().build();

            // Ownership check
            if (!profile.get("id").equals(interview.get("user_id"))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            List<Map<String, Object>> questions = supabaseService.getInterviewQuestions(id);

            Map<String, Object> result = new HashMap<>(interview);
            result.put("questions", questions);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error fetching interview {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch interview"));
        }
    }

    /**
     * POST /api/interviews/{id}/answers - Submit an answer for a question
     */
    @PostMapping("/{id}/answers")
    public ResponseEntity<Map<String, Object>> submitAnswer(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String id,
        @RequestBody Map<String, Object> request
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.badRequest().body(Map.of("error", "Profile not found"));

            Map<String, Object> interview = supabaseService.getInterview(id);
            if (interview == null) return ResponseEntity.notFound().build();

            if (!profile.get("id").equals(interview.get("user_id"))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            String questionId = (String) request.get("questionId");
            String answerText = (String) request.get("answerText");
            String questionText = (String) request.get("questionText");

            // Evaluate answer with Gemini
            Map<String, Object> evaluation = geminiService.evaluateAnswer(
                questionText, answerText,
                (String) interview.get("role"),
                (String) interview.get("interview_type")
            );

            double score = 0;
            if (evaluation.containsKey("overallScore")) {
                Object scoreObj = evaluation.get("overallScore");
                if (scoreObj instanceof Number n) score = n.doubleValue();
            }

            // Generate ideal answer
            String idealAnswer = geminiService.generateIdealAnswer(
                questionText,
                (String) interview.get("role"),
                (String) interview.get("interview_type")
            );

            // Update question record
            Map<String, Object> questionUpdates = new HashMap<>();
            questionUpdates.put("answer_text", answerText);
            questionUpdates.put("score", score);
            questionUpdates.put("evaluation", evaluation);

            supabaseService.updateQuestion(questionId, questionUpdates);

            Map<String, Object> response = new HashMap<>(evaluation);
            response.put("idealAnswer", idealAnswer);
            response.put("score", score);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error submitting answer", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to evaluate answer"));
        }
    }

    /**
     * POST /api/interviews/{id}/complete - Complete interview and generate report
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeInterview(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String id
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.badRequest().body(Map.of("error", "Profile not found"));

            Map<String, Object> interview = supabaseService.getInterview(id);
            if (interview == null) return ResponseEntity.notFound().build();

            if (!profile.get("id").equals(interview.get("user_id"))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            // Mark interview as evaluating
            supabaseService.updateInterview(id, Map.of(
                "status", "evaluating",
                "completed_at", Instant.now().toString()
            ));

            // Get all questions with answers
            List<Map<String, Object>> questions = supabaseService.getInterviewQuestions(id);

            // Build QA pairs for report
            List<Map<String, Object>> qaPairs = new ArrayList<>();
            double totalScore = 0;
            int scoredCount = 0;

            for (Map<String, Object> q : questions) {
                if (q.get("answer_text") != null) {
                    qaPairs.add(Map.of(
                        "question", q.getOrDefault("question_text", ""),
                        "answer", q.getOrDefault("answer_text", "")
                    ));
                    if (q.get("score") != null) {
                        totalScore += ((Number) q.get("score")).doubleValue();
                        scoredCount++;
                    }
                }
            }

            double overallScore = scoredCount > 0 ? totalScore / scoredCount : 0;

            // Generate comprehensive report
            Map<String, Object> reportData = geminiService.generateReport(
                (String) interview.get("role"),
                (String) interview.get("interview_type"),
                qaPairs
            );

            // Save report
            Map<String, Object> reportRecord = new HashMap<>();
            reportRecord.put("interview_id", id);
            reportRecord.put("user_id", profile.get("id"));
            reportRecord.put("overall_score", overallScore);
            reportRecord.put("report_data", reportData);
            reportRecord.put("created_at", Instant.now().toString());

            if (reportData.containsKey("breakdown")) {
                reportRecord.put("breakdown", reportData.get("breakdown"));
            }
            if (reportData.containsKey("strengths")) {
                reportRecord.put("strengths", reportData.get("strengths"));
            }
            if (reportData.containsKey("improvements")) {
                reportRecord.put("improvements", reportData.get("improvements"));
            }
            if (reportData.containsKey("recommendations")) {
                reportRecord.put("recommendations", reportData.get("recommendations"));
            }

            Map<String, Object> savedReport = supabaseService.createReport(reportRecord);

            // Update interview with final score
            supabaseService.updateInterview(id, Map.of(
                "status", "completed",
                "overall_score", overallScore
            ));

            // Update practice activity
            String today = java.time.LocalDate.now().toString();
            String userId = (String) profile.get("id");
            supabaseService.upsertActivity(Map.of(
                "user_id", userId,
                "activity_date", today,
                "completed_count", 1,
                "interview_count", 1,
                "updated_at", Instant.now().toString()
            ));

            return ResponseEntity.ok(Map.of(
                "reportId", savedReport != null ? savedReport.get("id") : "",
                "overallScore", overallScore,
                "report", reportData
            ));

        } catch (Exception e) {
            log.error("Error completing interview {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate report"));
        }
    }

    /**
     * PATCH /api/interviews/{id}/status - Pause, resume, or abandon
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String id,
        @RequestBody Map<String, String> request
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.badRequest().build();

            Map<String, Object> interview = supabaseService.getInterview(id);
            if (interview == null) return ResponseEntity.notFound().build();

            if (!profile.get("id").equals(interview.get("user_id"))) {
                return ResponseEntity.status(403).build();
            }

            String status = request.get("status");
            if (status == null || !List.of("paused", "in_progress", "abandoned").contains(status)) {
                return ResponseEntity.badRequest().build();
            }

            supabaseService.updateInterview(id, Map.of("status", status));
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error updating interview status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
