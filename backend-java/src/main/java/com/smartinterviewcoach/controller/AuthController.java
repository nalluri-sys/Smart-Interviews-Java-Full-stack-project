package com.smartinterviewcoach.controller;

import com.smartinterviewcoach.security.AuthenticatedUser;
import com.smartinterviewcoach.service.GeminiService;
import com.smartinterviewcoach.service.ResumeParserService;
import com.smartinterviewcoach.service.SupabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final SupabaseService supabaseService;
    private final GeminiService geminiService;
    private final ResumeParserService resumeParser;

    /**
     * Called after Supabase Auth to sync the user profile into application tables.
     * POST /api/auth/sync
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncUser(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        try {
            // Check if profile already exists
            Map<String, Object> existing = supabaseService.getProfileBySupabaseUid(user.getUid());

            if (existing != null) {
                return ResponseEntity.ok(Map.of(
                    "profile", existing,
                    "isNew", false
                ));
            }

            // Create new profile
            Map<String, Object> profile = new HashMap<>();
            profile.put("supabase_uid", user.getUid());
            profile.put("email", user.getEmail());
            profile.put("full_name", user.getDisplayName() != null ? user.getDisplayName() : "");
            profile.put("avatar_url", user.getPhotoUrl());
            profile.put("profile_status", "onboarding");
            profile.put("created_at", Instant.now().toString());
            profile.put("updated_at", Instant.now().toString());

            // Merge optional body fields
            if (body != null) {
                if (body.containsKey("target_role")) profile.put("target_role", body.get("target_role"));
                if (body.containsKey("experience_level")) profile.put("experience_level", body.get("experience_level"));
                if (body.containsKey("skills")) profile.put("skills", body.get("skills"));
                if (body.containsKey("career_goals")) profile.put("career_goals", body.get("career_goals"));
                if (body.containsKey("full_name")) profile.put("full_name", body.get("full_name"));
            }

            Map<String, Object> created = supabaseService.upsertProfile(profile);
            return ResponseEntity.ok(Map.of(
                "profile", created != null ? created : profile,
                "isNew", true
            ));

        } catch (Exception e) {
            log.error("Error syncing user profile: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to sync user profile"));
        }
    }

    /**
     * Upload resume + trigger AI analysis during onboarding.
     * POST /api/auth/resume
     */
    @PostMapping("/resume")
    public ResponseEntity<Map<String, Object>> uploadResume(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "targetRole", required = false) String targetRole
    ) {
        try {
            resumeParser.validate(file);

            String text = resumeParser.extractText(file);

            // Get or create profile
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Profile not found. Please sync first."));
            }

            String profileId = (String) profile.get("id");
            String safeFileName = UUID.randomUUID() + "_" + file.getOriginalFilename()
                .replaceAll("[^a-zA-Z0-9._-]", "_");

            // Upload to Supabase Storage
            String storagePath = supabaseService.uploadResume(
                profileId, safeFileName, file.getBytes(), file.getContentType());

            // Create resume record
            Map<String, Object> resumeData = new HashMap<>();
            resumeData.put("user_id", profileId);
            resumeData.put("original_name", file.getOriginalFilename());
            resumeData.put("storage_path", storagePath);
            resumeData.put("mime_type", file.getContentType());
            resumeData.put("file_size", (int) file.getSize());
            resumeData.put("extracted_text", text.substring(0, Math.min(text.length(), 5000)));
            resumeData.put("analysis_status", "processing");
            resumeData.put("created_at", Instant.now().toString());

            Map<String, Object> resume = supabaseService.createResume(resumeData);
            String resumeId = resume != null ? (String) resume.get("id") : null;

            // Run AI analysis
            Map<String, Object> aiProfile = geminiService.analyzeResume(text,
                targetRole != null ? targetRole : (String) profile.get("target_role"));

            // Update resume with AI analysis
            if (resumeId != null) {
                supabaseService.updateResume(resumeId, Map.of(
                    "parsed_profile", aiProfile,
                    "analysis_status", "completed",
                    "analyzed_at", Instant.now().toString()
                ));
            }

            // Update user profile with AI insights
            Map<String, Object> profileUpdates = new HashMap<>();
            profileUpdates.put("ai_profile", aiProfile);
            profileUpdates.put("profile_status", "completed");
            profileUpdates.put("profile_source", "resume");
            profileUpdates.put("updated_at", Instant.now().toString());

            if (aiProfile.containsKey("experienceLevel")) {
                profileUpdates.put("experience_level", aiProfile.get("experienceLevel"));
            }
            if (aiProfile.containsKey("technicalSkills") && !profileUpdates.containsKey("skills")) {
                profileUpdates.put("skills", aiProfile.get("technicalSkills"));
            }

            supabaseService.updateProfile(profileId, profileUpdates);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "aiProfile", aiProfile,
                "resumeId", resumeId != null ? resumeId : ""
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing resume upload", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to process resume. Please try again."));
        }
    }
}
