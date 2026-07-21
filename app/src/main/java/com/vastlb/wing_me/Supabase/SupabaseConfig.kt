
package com.vastlb.wing_me.Supabase

// Same Supabase project already used (live) by the iOS app and the Next.js web app.
// Anon key is safe to embed client-side (mirrors WAPSecrets.swift / .env.local on web).
object SupabaseConfig {
    const val URL = "https://yatixschvikugckkpfum.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlhdGl4c2NodmlrdWdja2twZnVtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODMzNjMyNjQsImV4cCI6MjA5ODkzOTI2NH0.pB6kDTA6WPpVjmVgWQPQulbktxeuxlQt21z53FifsNE"

    const val AUTH_URL = "$URL/auth/v1"
    const val REST_URL = "$URL/rest/v1"
}
