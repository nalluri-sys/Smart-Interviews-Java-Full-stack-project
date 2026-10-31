-- ============================================================
-- SMART INTERVIEW COACH - SUPABASE DATABASE SCHEMA
-- Run this in your Supabase SQL Editor
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- PROFILES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    supabase_uid UUID UNIQUE NOT NULL,
    email TEXT,
    full_name TEXT DEFAULT '',
    phone_number TEXT,
    phone_verified BOOLEAN DEFAULT FALSE,
    avatar_url TEXT,
    target_role TEXT,
    experience_level TEXT,
    profile_source TEXT DEFAULT 'manual',
    career_goals TEXT,
    skills TEXT[],
    ai_profile JSONB,
    profile_status TEXT DEFAULT 'onboarding',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Migrate installations created by the previous Firebase-backed schema.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'firebase_uid'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'supabase_uid'
    ) THEN
        ALTER TABLE profiles RENAME COLUMN firebase_uid TO supabase_uid;
    END IF;
END $$;

-- ============================================================
-- RESUMES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS resumes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    original_name TEXT,
    storage_path TEXT,
    mime_type TEXT,
    file_size INTEGER,
    extracted_text TEXT,
    parsed_profile JSONB,
    analysis_status TEXT DEFAULT 'pending',
    analysis_error TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    analyzed_at TIMESTAMPTZ
);

-- ============================================================
-- INTERVIEWS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS interviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    role TEXT NOT NULL,
    interview_type TEXT NOT NULL,
    difficulty TEXT NOT NULL,
    experience_level TEXT,
    language TEXT DEFAULT 'English',
    duration_seconds INTEGER,
    question_count INTEGER DEFAULT 8,
    status TEXT DEFAULT 'draft',
    overall_score NUMERIC,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- INTERVIEW QUESTIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS interview_questions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    interview_id UUID REFERENCES interviews(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    question_text TEXT NOT NULL,
    category TEXT,
    difficulty TEXT,
    expected_topics TEXT[],
    answer_text TEXT,
    transcript TEXT,
    score NUMERIC,
    evaluation JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- REPORTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    interview_id UUID REFERENCES interviews(id) ON DELETE CASCADE,
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    overall_score NUMERIC,
    breakdown JSONB,
    strengths TEXT[],
    improvements TEXT[],
    recommendations TEXT[],
    report_data JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- PRACTICE ACTIVITY TABLE (for heatmap)
-- ============================================================
CREATE TABLE IF NOT EXISTS practice_activity (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    activity_date DATE NOT NULL,
    interview_count INTEGER DEFAULT 0,
    completed_count INTEGER DEFAULT 0,
    total_practice_seconds INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, activity_date)
);

-- ============================================================
-- GOALS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS goals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    goal_type TEXT NOT NULL,
    target_value INTEGER NOT NULL,
    current_value INTEGER DEFAULT 0,
    period_start DATE,
    period_end DATE,
    status TEXT DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- NOTIFICATIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    notification_type TEXT,
    title TEXT,
    message TEXT,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- ACHIEVEMENTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS achievements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    achievement_code TEXT NOT NULL,
    unlocked_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- BOOKMARKS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS bookmarks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    question_id UUID REFERENCES interview_questions(id) ON DELETE CASCADE,
    note TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- JOB DESCRIPTIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS job_descriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT,
    company TEXT,
    description TEXT,
    extracted_requirements JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- ROW LEVEL SECURITY POLICIES
-- ============================================================

-- Enable RLS on all tables
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE resumes ENABLE ROW LEVEL SECURITY;
ALTER TABLE interviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE interview_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE practice_activity ENABLE ROW LEVEL SECURITY;
ALTER TABLE goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE bookmarks ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_descriptions ENABLE ROW LEVEL SECURITY;

-- NOTE: Since we use service_role key from the backend, these policies
-- apply to direct Supabase client calls only. Backend bypasses RLS.
-- The policies below allow service_role full access (default behavior).

-- ============================================================
-- STORAGE BUCKET SETUP
-- Run this in Supabase Dashboard > Storage
-- ============================================================
-- Create bucket: resumes (private)
-- INSERT INTO storage.buckets (id, name, public) VALUES ('resumes', 'resumes', false);

-- ============================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_profiles_supabase_uid ON profiles(supabase_uid);
CREATE INDEX IF NOT EXISTS idx_interviews_user_id ON interviews(user_id);
CREATE INDEX IF NOT EXISTS idx_interviews_status ON interviews(status);
CREATE INDEX IF NOT EXISTS idx_interview_questions_interview_id ON interview_questions(interview_id);
CREATE INDEX IF NOT EXISTS idx_reports_interview_id ON reports(interview_id);
CREATE INDEX IF NOT EXISTS idx_reports_user_id ON reports(user_id);
CREATE INDEX IF NOT EXISTS idx_practice_activity_user_date ON practice_activity(user_id, activity_date);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id, read_at);
