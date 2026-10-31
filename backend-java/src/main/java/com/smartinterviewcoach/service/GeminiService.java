package com.smartinterviewcoach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    private final WebClient geminiClient;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-2.0-flash}")
    private String model;

    public GeminiService(@Qualifier("geminiWebClient") WebClient geminiClient,
                         ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Analyze a resume and return a structured AI profile.
     */
    public Map<String, Object> analyzeResume(String resumeText, String targetRole) {
        String prompt = buildResumeAnalysisPrompt(resumeText, targetRole);
        String response = callGemini(prompt);
        return parseJsonResponse(response, "resume analysis");
    }

    /**
     * Generate interview questions based on user profile and interview config.
     */
    public List<Map<String, Object>> generateQuestions(
        String role, String interviewType, String difficulty,
        int questionCount, String skills, String resumeSummary
    ) {
        String prompt = buildQuestionGenerationPrompt(
            role, interviewType, difficulty, questionCount, skills, resumeSummary);
        String response = callGemini(prompt);
        return parseQuestionsResponse(response);
    }

    /**
     * Evaluate a single answer.
     */
    public Map<String, Object> evaluateAnswer(
        String question, String answer, String role, String interviewType
    ) {
        String prompt = buildEvaluationPrompt(question, answer, role, interviewType);
        String response = callGemini(prompt);
        return parseJsonResponse(response, "answer evaluation");
    }

    /**
     * Generate a comprehensive post-interview report.
     */
    public Map<String, Object> generateReport(
        String role, String interviewType, List<Map<String, Object>> questionsAndAnswers
    ) {
        String prompt = buildReportPrompt(role, interviewType, questionsAndAnswers);
        String response = callGemini(prompt);
        return parseJsonResponse(response, "report generation");
    }

    /**
     * Generate an ideal sample answer for a question.
     */
    public String generateIdealAnswer(String question, String role, String interviewType) {
        String prompt = String.format("""
            You are an expert interview coach. Generate a model answer for the following interview question.
            
            Role: %s
            Interview Type: %s
            Question: %s
            
            Provide a comprehensive, well-structured answer that demonstrates:
            - Deep understanding of the topic
            - Clear communication and structure
            - Relevant examples (use generic but realistic examples)
            - Confidence and professionalism
            
            Return ONLY the model answer text, no JSON, no preamble.
            """, role, interviewType, question);

        return callGemini(prompt);
    }

    /**
     * Analyze a job description and match against user profile.
     */
    public Map<String, Object> analyzeJobDescription(String jobDescription, String userSkills) {
        String prompt = String.format("""
            Analyze this job description and compare it with the candidate's skills.
            
            JOB DESCRIPTION:
            %s
            
            CANDIDATE SKILLS:
            %s
            
            Return a JSON object with this exact structure:
            {
              "requiredSkills": ["skill1", "skill2"],
              "niceToHaveSkills": ["skill1"],
              "matchingSkills": ["skill1", "skill2"],
              "missingSkills": ["skill1"],
              "matchPercentage": 75,
              "recommendedTopics": ["topic1", "topic2"],
              "preparationChecklist": ["item1", "item2"],
              "summary": "brief analysis summary"
            }
            
            Return ONLY the JSON object.
            """, jobDescription, userSkills);

        String response = callGemini(prompt);
        return parseJsonResponse(response, "job description analysis");
    }

    private String callGemini(String prompt) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode content = objectMapper.createObjectNode();
            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("text", prompt);
            parts.add(part);
            content.put("role", "user");
            content.set("parts", parts);
            contents.add(content);
            requestBody.set("contents", contents);

            // Add generation config for JSON responses
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 4096);
            requestBody.set("generationConfig", generationConfig);

            String url = "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            String responseBody = geminiClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    log.error("Gemini API call failed: {}", e.getMessage());
                    return Mono.just("{\"error\": \"" + e.getMessage() + "\"}");
                })
                .block();

            // Extract text from Gemini response
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode candidate = candidates.get(0);
                JsonNode textNode = candidate.path("content").path("parts").get(0).path("text");
                return textNode.asText();
            }

            log.warn("Gemini returned no candidates: {}", responseBody);
            return "{}";

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String text, String context) {
        try {
            // Strip markdown code fences if present
            String cleaned = text.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            return objectMapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse Gemini {} response: {}", context, e.getMessage());
            return Map.of("error", "Failed to parse AI response", "raw", text);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseQuestionsResponse(String text) {
        try {
            String cleaned = text.trim();
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            cleaned = cleaned.trim();

            // Try parsing as array first
            if (cleaned.startsWith("[")) {
                return objectMapper.readValue(cleaned, List.class);
            }
            // Try parsing as object with questions array
            Map<String, Object> obj = objectMapper.readValue(cleaned, Map.class);
            if (obj.containsKey("questions")) {
                return (List<Map<String, Object>>) obj.get("questions");
            }
            return List.of();
        } catch (Exception e) {
            log.error("Failed to parse questions response: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildResumeAnalysisPrompt(String resumeText, String targetRole) {
        return String.format("""
            You are an expert technical recruiter and career coach. Analyze this resume text and extract a structured profile.
            
            TARGET ROLE (if specified): %s
            
            RESUME TEXT:
            %s
            
            Return a JSON object with EXACTLY this structure:
            {
              "candidateSummary": "2-3 sentence professional summary",
              "experienceLevel": "Entry|Junior|Mid|Senior|Expert",
              "yearsOfExperience": 0,
              "technicalSkills": ["skill1", "skill2"],
              "softSkills": ["skill1", "skill2"],
              "recommendedRoles": ["role1", "role2"],
              "projects": ["project description 1"],
              "education": "highest education level",
              "certifications": ["cert1"],
              "strengths": ["strength1", "strength2"],
              "skillGaps": ["gap1", "gap2"],
              "interviewFocusAreas": ["area1", "area2"],
              "suggestedDifficulty": "Beginner|Intermediate|Advanced|Expert",
              "suggestedInterviewTypes": ["Technical", "Behavioral", "HR"]
            }
            
            Return ONLY the JSON object, no markdown, no explanation.
            """, targetRole != null ? targetRole : "not specified", resumeText);
    }

    private String buildQuestionGenerationPrompt(
        String role, String type, String difficulty,
        int count, String skills, String resumeSummary
    ) {
        return String.format("""
            You are a senior technical interviewer. Generate exactly %d interview questions.
            
            Role: %s
            Interview Type: %s
            Difficulty: %s
            Candidate Skills: %s
            Candidate Background: %s
            
            Return a JSON array with EXACTLY this structure for each question:
            [
              {
                "position": 1,
                "questionText": "The full interview question",
                "category": "Technical|Behavioral|HR|Situational",
                "difficulty": "Easy|Medium|Hard",
                "expectedTopics": ["topic1", "topic2"],
                "estimatedMinutes": 3,
                "followUpHints": ["hint if needed"]
              }
            ]
            
            For BEHAVIORAL interviews, use STAR method questions.
            For TECHNICAL interviews, include coding/design/problem-solving questions appropriate for the role.
            For HR interviews, include cultural fit, motivation, and soft skill questions.
            
            Return ONLY the JSON array.
            """, count, role, type, difficulty, skills, resumeSummary);
    }

    private String buildEvaluationPrompt(
        String question, String answer, String role, String interviewType
    ) {
        return String.format("""
            You are an expert interview evaluator. Evaluate this candidate's answer.
            
            Role: %s
            Interview Type: %s
            Question: %s
            
            Candidate's Answer: %s
            
            Return a JSON object with EXACTLY this structure:
            {
              "overallScore": 75,
              "relevanceScore": 80,
              "communicationScore": 70,
              "technicalScore": 75,
              "confidenceScore": 65,
              "feedback": "Detailed feedback paragraph",
              "strengths": ["strength1", "strength2"],
              "improvements": ["area1 to improve", "area2"],
              "fillerWords": ["um", "like"],
              "suggestedPhrasing": "Better way to phrase key points",
              "followUpQuestion": "A relevant follow-up question",
              "grade": "A|B|C|D|F"
            }
            
            Score from 0-100 for each dimension. Be constructive and specific.
            Return ONLY the JSON object.
            """, role, interviewType, question, answer);
    }

    private String buildReportPrompt(
        String role, String interviewType,
        List<Map<String, Object>> questionsAndAnswers
    ) {
        StringBuilder qaBuilder = new StringBuilder();
        int i = 1;
        for (Map<String, Object> qa : questionsAndAnswers) {
            qaBuilder.append(String.format("Q%d: %s%nA: %s%n%n",
                i++, qa.get("question"), qa.get("answer")));
        }

        return String.format("""
            You are an expert career coach generating a comprehensive post-interview report.
            
            Role: %s
            Interview Type: %s
            
            Questions and Answers:
            %s
            
            Return a JSON object with EXACTLY this structure:
            {
              "overallScore": 75,
              "breakdown": {
                "technical": 70,
                "communication": 80,
                "behavioral": 75,
                "problemSolving": 72,
                "confidence": 78
              },
              "strengths": ["strength1", "strength2", "strength3"],
              "improvements": ["area1", "area2", "area3"],
              "recommendations": ["action1", "action2", "action3"],
              "learningPlan": [
                {"topic": "topic1", "resources": ["resource1"], "priority": "High"},
                {"topic": "topic2", "resources": ["resource1"], "priority": "Medium"}
              ],
              "summary": "Overall performance summary paragraph",
              "nextSteps": "What to focus on next",
              "readinessLevel": "Not Ready|Developing|Ready|Highly Ready"
            }
            
            Return ONLY the JSON object.
            """, role, interviewType, qaBuilder.toString());
    }
}
