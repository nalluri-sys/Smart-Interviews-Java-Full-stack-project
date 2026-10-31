/**
 * api.js
 * Centralized API client for the Spring Boot backend.
 * All backend calls go through this module.
 */

// ============================================================
// CONFIGURATION - Update to match your backend URL
// ============================================================
const API_BASE_URL = 'http://localhost:8080/api';

// ============================================================
// CORE HTTP HELPERS
// ============================================================

async function getAuthToken() {
    const { supabase } = await import('./supabase-config.js');
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) throw new Error('Not authenticated');
    const { data: { session }, error } = await supabase.auth.getSession();
    if (error || !session) throw error || new Error('Not authenticated');
    return session.access_token;
}

async function apiRequest(method, endpoint, body = null, isFormData = false) {
    const token = await getAuthToken();

    const headers = {
        'Authorization': `Bearer ${token}`
    };

    if (!isFormData && body) {
        headers['Content-Type'] = 'application/json';
    }

    const options = {
        method,
        headers,
    };

    if (body) {
        options.body = isFormData ? body : JSON.stringify(body);
    }

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, options);

        if (response.status === 204) return null;

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(data.error || `HTTP ${response.status}: ${response.statusText}`);
        }

        return data;
    } catch (err) {
        if (err.name === 'TypeError') {
            throw new Error('Cannot connect to backend server. Make sure it is running on port 8080.');
        }
        throw err;
    }
}

const get = (endpoint) => apiRequest('GET', endpoint);
const post = (endpoint, body) => apiRequest('POST', endpoint, body);
const patch = (endpoint, body) => apiRequest('PATCH', endpoint, body);
const del = (endpoint) => apiRequest('DELETE', endpoint);

async function postFormData(endpoint, formData) {
    return apiRequest('POST', endpoint, formData, true);
}

// ============================================================
// HEALTH CHECK
// ============================================================
export async function checkHealth() {
    const response = await fetch(`${API_BASE_URL}/health`);
    return response.json();
}

// ============================================================
// AUTH / USER SYNC
// ============================================================

/** Sync the Supabase Auth user to the application profile after login/registration. */
export async function syncUser(profileData = {}) {
    return post('/auth/sync', profileData);
}

/** Upload resume and trigger AI analysis */
export async function uploadResume(file, targetRole = '') {
    const formData = new FormData();
    formData.append('file', file);
    if (targetRole) formData.append('targetRole', targetRole);
    return postFormData('/auth/resume', formData);
}

// ============================================================
// PROFILE
// ============================================================

export async function getProfile() {
    return get('/profile');
}

export async function updateProfile(updates) {
    return patch('/profile', updates);
}

export async function getStats() {
    return get('/profile/stats');
}

export async function getActivity(weeks = 12) {
    return get(`/profile/activity?weeks=${weeks}`);
}

export async function getNotifications(unreadOnly = false) {
    return get(`/profile/notifications?unreadOnly=${unreadOnly}`);
}

export async function markNotificationsRead() {
    return post('/profile/notifications/read');
}

// ============================================================
// INTERVIEWS
// ============================================================

/** Create a new interview session with AI-generated questions */
export async function createInterview(config) {
    return post('/interviews', config);
}

/** Get paginated list of user's interviews */
export async function getInterviews(page = 0, size = 10) {
    return get(`/interviews?page=${page}&size=${size}`);
}

/** Get a specific interview with all questions */
export async function getInterview(id) {
    return get(`/interviews/${id}`);
}

/** Submit an answer for a question (triggers AI evaluation) */
export async function submitAnswer(interviewId, questionId, questionText, answerText) {
    return post(`/interviews/${interviewId}/answers`, {
        questionId,
        questionText,
        answerText
    });
}

/** Mark interview as complete and generate the AI report */
export async function completeInterview(interviewId) {
    return post(`/interviews/${interviewId}/complete`);
}

/** Update interview status (pause, resume, abandon) */
export async function updateInterviewStatus(interviewId, status) {
    return patch(`/interviews/${interviewId}/status`, { status });
}

// ============================================================
// REPORTS
// ============================================================

export async function getAllReports() {
    return get('/reports');
}

export async function getReport(interviewId) {
    return get(`/reports/${interviewId}`);
}

export async function generateIdealAnswer(interviewId, question) {
    return post(`/reports/${interviewId}/ideal-answer`, { question });
}

export async function analyzeJobDescription(jobDescription) {
    return post('/reports/job-analysis', { jobDescription });
}
