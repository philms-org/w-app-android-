
package com.vastlb.wing_me.Supabase

import android.content.Context
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.vastlb.wing_me.Classes.Singleton
import org.json.JSONObject

// Supabase GoTrue auth (https://<project>.supabase.co/auth/v1/*).
// Mirrors WAPAuth.swift (iOS) / lib/auth.ts (web) but over Volley, since this app
// cannot add a new networking dependency. Endpoints/payload shapes verified live
// against the real project on 2026-07-20 (see final report for what was exercised
// end-to-end vs. only wired from the documented shape).
//
// Session storage reuses the EXACT SharedPreferences pattern already used across this
// app: getSharedPreferences("Preferences", MODE_PRIVATE), key "Token" for the bearer
// token (so every existing out-of-scope screen's "is the user logged in" check via
// preferences.getString("Token", "") keeps working unmodified). "UserID" is a new key
// (nothing outside the Supabase package reads it) holding the Supabase auth.uid().
object SupabaseAuth {

    private const val PREFS = "Preferences"
    private const val KEY_TOKEN = "Token"
    private const val KEY_REFRESH_TOKEN = "RefreshToken"
    private const val KEY_USER_ID = "UserID"

    // ---- Session storage ----

    fun saveSession(context: Context, accessToken: String, refreshToken: String?, userId: String) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(KEY_TOKEN, accessToken)
        editor.putString(KEY_USER_ID, userId)
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        editor.apply()
    }

    fun getToken(context: Context): String? {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = preferences.getString(KEY_TOKEN, "")
        return if (token.isNullOrEmpty()) null else token
    }

    fun getUserId(context: Context): String? {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = preferences.getString(KEY_USER_ID, "")
        return if (id.isNullOrEmpty()) null else id
    }

    fun isLoggedIn(context: Context): Boolean = getToken(context) != null

    fun clearSession(context: Context) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.remove(KEY_TOKEN)
        editor.remove(KEY_REFRESH_TOKEN)
        editor.remove(KEY_USER_ID)
        editor.apply()
    }

    // ---- Email + password (fallback / tested end-to-end against the live project) ----

    fun signUpEmailPassword(
        context: Context,
        email: String,
        password: String,
        onSuccess: (accessToken: String, userId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val body = JSONObject()
        body.put("email", email)
        body.put("password", password)

        authRequest(context, "${SupabaseConfig.AUTH_URL}/signup", body, SupabaseConfig.ANON_KEY, onSuccess, onError)
    }

    fun signInEmailPassword(
        context: Context,
        email: String,
        password: String,
        onSuccess: (accessToken: String, userId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val body = JSONObject()
        body.put("email", email)
        body.put("password", password)

        authRequest(context, "${SupabaseConfig.AUTH_URL}/token?grant_type=password", body, SupabaseConfig.ANON_KEY, onSuccess, onError)
    }

    // ---- Phone OTP (wired from Supabase's documented GoTrue REST shape; the request
    // shape itself was confirmed live — POST /auth/v1/otp {phone} and POST /auth/v1/verify
    // {type,phone,token} both return structured GoTrue responses on this project rather
    // than 404s — but a full send+verify round trip could NOT be exercised end-to-end
    // because this Supabase project currently has no SMS provider configured
    // ("phone_provider_disabled"). See final report.) ----

    fun sendPhoneOtp(
        context: Context,
        phone: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val body = JSONObject()
        body.put("phone", phone)

        val request = object: JsonObjectRequest(
            Request.Method.POST, "${SupabaseConfig.AUTH_URL}/otp", body,
            Response.Listener { onSuccess() },
            Response.ErrorListener { error -> onError(parseError(error)) }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> = authHeaders(SupabaseConfig.ANON_KEY)
        }
        Singleton.getInstance(context).addToRequestQueue(request)
    }

    fun verifyPhoneOtp(
        context: Context,
        phone: String,
        code: String,
        onSuccess: (accessToken: String, userId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val body = JSONObject()
        body.put("type", "sms")
        body.put("phone", phone)
        body.put("token", code)

        authRequest(context, "${SupabaseConfig.AUTH_URL}/verify", body, SupabaseConfig.ANON_KEY, onSuccess, onError)
    }

    // ---- Sign out ----

    fun signOut(context: Context, onComplete: () -> Unit) {
        val token = getToken(context)
        clearSession(context)

        if (token == null) {
            onComplete()
            return
        }
        val request = object: JsonObjectRequest(
            Request.Method.POST, "${SupabaseConfig.AUTH_URL}/logout", JSONObject(),
            Response.Listener { onComplete() },
            Response.ErrorListener { onComplete() }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> = authHeaders(token)
        }
        Singleton.getInstance(context).addToRequestQueue(request)
    }

    // ---- Shared helpers ----

    private fun authRequest(
        context: Context,
        url: String,
        body: JSONObject,
        bearer: String,
        onSuccess: (accessToken: String, userId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = object: JsonObjectRequest(
            Request.Method.POST, url, body,
            Response.Listener { response ->
                try {
                    val accessToken = response.getString("access_token")
                    val refreshToken = response.optString("refresh_token", null)
                    val user = response.getJSONObject("user")
                    val userId = user.getString("id")

                    saveSession(context, accessToken, refreshToken, userId)
                    onSuccess(accessToken, userId)
                } catch (e: Exception) {
                    onError(e.toString())
                }
            },
            Response.ErrorListener { error -> onError(parseError(error)) }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> = authHeaders(bearer)
        }
        Singleton.getInstance(context).addToRequestQueue(request)
    }

    private fun authHeaders(bearer: String): MutableMap<String, String> {
        val headers = HashMap<String, String>()
        headers["apikey"] = SupabaseConfig.ANON_KEY
        headers["Authorization"] = "Bearer $bearer"
        headers["Content-Type"] = "application/json"
        return headers
    }

    fun parseError(error: VolleyError): String {
        try {
            val data = error.networkResponse?.data ?: return error.toString()
            val json = JSONObject(String(data, Charsets.UTF_8))
            if (json.has("error_description")) return json.getString("error_description")
            if (json.has("msg")) return json.getString("msg")
            if (json.has("message")) return json.getString("message")
            return json.toString()
        } catch (e: Exception) {
            return error.toString()
        }
    }
}
