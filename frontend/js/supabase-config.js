/**
 * Supabase Auth client used by the browser application.
 * The anon key is safe to expose in frontend code; never expose the service role key.
 */
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

const SUPABASE_URL = 'https://YOUR_PROJECT_REF.supabase.co';
const SUPABASE_ANON_KEY = 'YOUR_SUPABASE_ANON_KEY';

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

export const auth = {
    get currentUser() {
        return supabase.auth.getUser().then(({ data }) => data.user);
    }
};

function toUser(user) {
    if (!user) return null;
    return {
        ...user,
        displayName: user.user_metadata?.full_name || user.user_metadata?.name || '',
        photoURL: user.user_metadata?.avatar_url || null,
        async getIdToken() {
            const { data, error } = await supabase.auth.getSession();
            if (error || !data.session) throw error || new Error('Not authenticated');
            return data.session.access_token;
        }
    };
}

export function onAuthStateChanged(_auth, callback) {
    supabase.auth.getUser().then(({ data }) => callback(toUser(data.user)));
    return supabase.auth.onAuthStateChange((_event, session) => callback(toUser(session?.user)));
}

export async function signInWithEmailAndPassword(_auth, email, password) {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password });
    if (error) throw error;
    return { user: toUser(data.user), session: data.session };
}

export async function createUserWithEmailAndPassword(_auth, email, password) {
    const { data, error } = await supabase.auth.signUp({ email, password });
    if (error) throw error;
    return { user: toUser(data.user), session: data.session };
}

export async function updateProfile(user, updates) {
    const { data, error } = await supabase.auth.updateUser({
        data: { full_name: updates.displayName }
    });
    if (error) throw error;
    Object.assign(user, toUser(data.user));
    return { user };
}

export async function sendPasswordResetEmail(_auth, email) {
    const { error } = await supabase.auth.resetPasswordForEmail(email, {
        redirectTo: `${window.location.origin}/login.html`
    });
    if (error) throw error;
}

export async function signOut() {
    const { error } = await supabase.auth.signOut();
    if (error) throw error;
}

export async function signInWithPopup(_auth, provider) {
    const { error } = await supabase.auth.signInWithOAuth({
        provider: provider.id,
        options: { redirectTo: `${window.location.origin}/login.html` }
    });
    if (error) throw error;
    return { user: null };
}

export const googleProvider = { id: 'google' };
export const githubProvider = { id: 'github' };
