package com.smartinterviewcoach.controller;

import com.smartinterviewcoach.security.AuthenticatedUser;
import com.smartinterviewcoach.service.SupabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final SupabaseService supabaseService;

    /**
     * GET /api/profile - Get current user's profile
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile(
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error fetching profile for uid={}", user.getUid(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch profile"));
        }
    }

    /**
     * PATCH /api/profile - Update profile fields
     */
    @PatchMapping
    public ResponseEntity<Map<String, Object>> updateProfile(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Object> updates
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) {
                return ResponseEntity.notFound().build();
            }

            // Only allow safe fields to be updated
            Map<String, Object> safeUpdates = new HashMap<>();
            String[] allowedFields = {
                "full_name", "target_role", "experience_level",
                "career_goals", "skills", "avatar_url", "phone_number"
            };
            for (String field : allowedFields) {
                if (updates.containsKey(field)) {
                    safeUpdates.put(field, updates.get(field));
                }
            }
            safeUpdates.put("updated_at", Instant.now().toString());

            String profileId = (String) profile.get("id");
            Map<String, Object> updated = supabaseService.updateProfile(profileId, safeUpdates);

            return ResponseEntity.ok(updated != null ? updated : safeUpdates);
        } catch (Exception e) {
            log.error("Error updating profile for uid={}", user.getUid(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update profile"));
        }
    }

    /**
     * GET /api/profile/stats - Get user's interview statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) {
                return ResponseEntity.notFound().build();
            }

            String userId = (String) profile.get("id");
            Map<String, Object> stats = supabaseService.getUserStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching stats", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch stats"));
        }
    }

    /**
     * GET /api/profile/activity?weeks=12 - Get practice heatmap data
     */
    @GetMapping("/activity")
    public ResponseEntity<Object> getActivity(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestParam(defaultValue = "12") int weeks
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.notFound().build();

            String userId = (String) profile.get("id");
            return ResponseEntity.ok(supabaseService.getPracticeActivity(userId, weeks));
        } catch (Exception e) {
            log.error("Error fetching activity", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch activity"));
        }
    }

    /**
     * GET /api/profile/notifications - Get user notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<Object> getNotifications(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.notFound().build();

            String userId = (String) profile.get("id");
            return ResponseEntity.ok(supabaseService.getUserNotifications(userId, unreadOnly));
        } catch (Exception e) {
            log.error("Error fetching notifications", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch notifications"));
        }
    }

    /**
     * POST /api/profile/notifications/read - Mark all notifications as read
     */
    @PostMapping("/notifications/read")
    public ResponseEntity<Void> markNotificationsRead(
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        try {
            Map<String, Object> profile = supabaseService.getProfileBySupabaseUid(user.getUid());
            if (profile == null) return ResponseEntity.notFound().build();

            supabaseService.markNotificationsRead((String) profile.get("id"));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking notifications read", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
