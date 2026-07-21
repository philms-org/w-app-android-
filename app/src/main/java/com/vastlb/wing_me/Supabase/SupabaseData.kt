
package com.vastlb.wing_me.Supabase

import android.content.Context
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Classes.Singleton
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Central Supabase REST data layer (PostgREST under {url}/rest/v1/*). Mirrors
// lib/data.ts (web) / WAPData.swift (iOS) — same tables, same columns, same method
// shapes — but returns raw org.json JSONObject/JSONArray (like every other screen in
// this codebase already does) instead of introducing a new model/serialization layer.
//
// IMPORTANT schema note: the shared `profiles` table on the live project was verified
// column-by-column against the real REST API (2026-07-20) and does NOT yet have
// `email`, `fave_drink`, `friday_night`, `profession`, the four `*_visible` flags, or
// `is_verified` — it DOES have `gender` and `date_of_birth` (present on this table even
// though not listed in the nominal shared schema). upsertProfile() below only ever
// sends columns confirmed to exist; see the final report for the full diff against the
// nominal schema.
object SupabaseData {

    private fun currentUserId(context: Context): String? = SupabaseAuth.getUserId(context)

    private fun headers(context: Context, extra: Map<String, String> = emptyMap()): MutableMap<String, String> {
        val headers = HashMap<String, String>()
        headers["apikey"] = SupabaseConfig.ANON_KEY
        headers["Authorization"] = "Bearer ${SupabaseAuth.getToken(context) ?: SupabaseConfig.ANON_KEY}"
        headers["Content-Type"] = "application/json"
        headers.putAll(extra)
        return headers
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun arrayRequest(
        context: Context,
        method: Int,
        path: String,
        body: JSONArray? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        onSuccess: (JSONArray) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = object: JsonArrayRequest(
            method, "${SupabaseConfig.REST_URL}$path", body,
            Response.Listener { response -> onSuccess(response) },
            Response.ErrorListener { error -> onError(SupabaseAuth.parseError(error)) }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> = headers(context, extraHeaders)
        }
        Singleton.getInstance(context).addToRequestQueue(request)
    }

    // Volley's JsonRequest subclasses parse the response body strictly as either
    // object or array. PostgREST returns an ARRAY for POST/PATCH with
    // `Prefer: return=representation`, so every write below uses JsonArrayRequest with
    // a single-element body array (PostgREST accepts a JSON array of rows to insert).
    private fun writeRequest(
        context: Context,
        method: Int,
        path: String,
        row: JSONObject,
        prefer: String,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val body = JSONArray()
        body.put(row)

        arrayRequest(
            context, method, path, body,
            mapOf("Prefer" to prefer),
            onSuccess = { array -> onSuccess(if (array.length() > 0) array.getJSONObject(0) else null) },
            onError = onError
        )
    }

    // ---- Profile ----

    fun fetchProfile(context: Context, userId: String, onSuccess: (JSONObject?) -> Unit, onError: (String) -> Unit) {
        arrayRequest(
            context, Request.Method.GET, "/profiles?id=eq.${enc(userId)}&select=*",
            onSuccess = { array -> onSuccess(if (array.length() > 0) array.getJSONObject(0) else null) },
            onError = onError
        )
    }

    // `fields` must include "id". Only columns confirmed to exist on the live
    // `profiles` table should be included by callers (see class doc above).
    fun upsertProfile(context: Context, fields: JSONObject, onSuccess: (JSONObject?) -> Unit, onError: (String) -> Unit) {
        writeRequest(context, Request.Method.POST, "/profiles", fields, "resolution=merge-duplicates,return=representation", onSuccess, onError)
    }

    // ---- Venues (locations) ----

    fun fetchVenues(context: Context, onSuccess: (JSONArray) -> Unit, onError: (String) -> Unit) {
        arrayRequest(context, Request.Method.GET, "/locations?select=*&order=name", onSuccess = onSuccess, onError = onError)
    }

    fun fetchVenue(context: Context, id: String, onSuccess: (JSONObject?) -> Unit, onError: (String) -> Unit) {
        arrayRequest(
            context, Request.Method.GET, "/locations?id=eq.${enc(id)}&select=*",
            onSuccess = { array -> onSuccess(if (array.length() > 0) array.getJSONObject(0) else null) },
            onError = onError
        )
    }

    // ---- Banners (Home tab carousel) ----

    fun fetchBanners(context: Context, onSuccess: (JSONArray) -> Unit, onError: (String) -> Unit) {
        arrayRequest(
            context, Request.Method.GET,
            "/banners?select=*&is_active=eq.true&order=display_order",
            onSuccess = onSuccess, onError = onError
        )
    }

    // ---- Check-in / presence (location_checkins) ----

    fun checkIn(context: Context, locationId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = currentUserId(context) ?: return onError("Not signed in")
        val row = JSONObject()
        row.put("user_id", uid)
        row.put("location_id", locationId)
        row.put("mode", "live")

        writeRequest(context, Request.Method.POST, "/location_checkins", row, "return=representation", { onSuccess() }, onError)
    }

    fun checkOut(context: Context, locationId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = currentUserId(context) ?: return onError("Not signed in")
        val now = isoNow()
        val body = JSONObject()
        body.put("checked_out_at", now)

        val path = "/location_checkins?user_id=eq.${enc(uid)}&location_id=eq.${enc(locationId)}&checked_out_at=is.null"

        // PATCH without `Prefer: return=representation` comes back 204 with an empty
        // body, which Volley's JsonObjectRequest/JsonArrayRequest would fail to parse
        // as JSON — use a plain StringRequest (same pattern as every other network call
        // in this codebase) and treat any non-error HTTP response as success.
        val request = object: StringRequest(
            Method.PATCH, "${SupabaseConfig.REST_URL}$path",
            Response.Listener { onSuccess() },
            Response.ErrorListener { error -> onError(SupabaseAuth.parseError(error)) }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> = headers(context)

            @Throws(AuthFailureError::class)
            override fun getBody(): ByteArray = body.toString().toByteArray(Charsets.UTF_8)

            override fun getBodyContentType(): String = "application/json; charset=utf-8"
        }
        Singleton.getInstance(context).addToRequestQueue(request)
    }

    fun fetchPresence(context: Context, locationId: String, onSuccess: (JSONArray) -> Unit, onError: (String) -> Unit) {
        arrayRequest(
            context, Request.Method.GET,
            "/location_checkins?select=*,profiles(*)&location_id=eq.${enc(locationId)}&checked_out_at=is.null",
            onSuccess = onSuccess, onError = onError
        )
    }

    // ---- Contact methods ----

    fun fetchContactMethods(context: Context, userId: String, onSuccess: (JSONArray) -> Unit, onError: (String) -> Unit) {
        arrayRequest(context, Request.Method.GET, "/contact_methods?user_id=eq.${enc(userId)}&select=*&order=slot_order", onSuccess = onSuccess, onError = onError)
    }

    fun upsertContactMethod(context: Context, fields: JSONObject, onSuccess: (JSONObject?) -> Unit, onError: (String) -> Unit) {
        writeRequest(context, Request.Method.POST, "/contact_methods", fields, "resolution=merge-duplicates,return=representation", onSuccess, onError)
    }

    // ---- Rewards ----

    fun fetchRewards(context: Context, locationId: String, onSuccess: (JSONArray) -> Unit, onError: (String) -> Unit) {
        arrayRequest(
            context, Request.Method.GET,
            "/rewards?location_id=eq.${enc(locationId)}&is_active=eq.true&select=*&order=display_order",
            onSuccess = onSuccess, onError = onError
        )
    }

    // ---- Messaging ----

    // Returns a normalized JSONArray of conversation summaries, each shaped like:
    // { id, is_group, name, last_message, last_message_at, my_status,
    //   other_user_id, other_display_name, other_avatar_url }
    fun fetchConversations(context: Context, onSuccess: (JSONArray) -> Unit, onError: (String) -> Unit) {
        val uid = currentUserId(context) ?: return onSuccess(JSONArray())

        arrayRequest(
            context, Request.Method.GET,
            "/conversation_participants?select=conversation_id,status,conversations(id,is_group,name)&user_id=eq.${enc(uid)}",
            onSuccess = { rows ->
                if (rows.length() == 0) {
                    onSuccess(JSONArray())
                    return@arrayRequest
                }
                val ids = ArrayList<String>()
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    ids.add(row.getString("conversation_id"))
                }
                val idsParam = ids.joinToString(",")

                arrayRequest(
                    context, Request.Method.GET,
                    "/messages?conversation_id=in.(${idsParam})&order=created_at.desc&select=conversation_id,content,created_at",
                    onSuccess = { messages ->
                        val lastByConversation = HashMap<String, JSONObject>()
                        for (index in 0 until messages.length()) {
                            val message = messages.getJSONObject(index)
                            val cid = message.getString("conversation_id")
                            if (!lastByConversation.containsKey(cid)) {
                                lastByConversation[cid] = message
                            }
                        }

                        arrayRequest(
                            context, Request.Method.GET,
                            "/conversation_participants?select=conversation_id,user_id,profiles(display_name,avatar_url)&conversation_id=in.(${idsParam})&user_id=neq.${enc(uid)}",
                            onSuccess = { others ->
                                val otherByConversation = HashMap<String, JSONObject>()
                                for (index in 0 until others.length()) {
                                    val other = others.getJSONObject(index)
                                    val cid = other.getString("conversation_id")
                                    if (!otherByConversation.containsKey(cid)) {
                                        otherByConversation[cid] = other
                                    }
                                }

                                val result = JSONArray()
                                for (index in 0 until rows.length()) {
                                    val row = rows.getJSONObject(index)
                                    val conversation = row.getJSONObject("conversations")
                                    val cid = conversation.getString("id")

                                    val summary = JSONObject()
                                    summary.put("id", cid)
                                    summary.put("is_group", conversation.optBoolean("is_group", false))
                                    summary.put("name", conversation.opt("name"))
                                    summary.put("my_status", row.optString("status", "accepted"))

                                    val lastMessage = lastByConversation[cid]
                                    summary.put("last_message", lastMessage?.optString("content"))
                                    summary.put("last_message_at", lastMessage?.optString("created_at"))

                                    val other = otherByConversation[cid]
                                    val otherProfile = other?.optJSONObject("profiles")
                                    summary.put("other_user_id", other?.optString("user_id"))
                                    summary.put("other_display_name", otherProfile?.optString("display_name"))
                                    summary.put("other_avatar_url", otherProfile?.optString("avatar_url"))

                                    result.put(summary)
                                }
                                onSuccess(result)
                            },
                            onError = onError
                        )
                    },
                    onError = onError
                )
            },
            onError = onError
        )
    }

    fun fetchMessages(context: Context, conversationId: String, onSuccess: (JSONArray) -> Unit, onError: (String) -> Unit) {
        arrayRequest(
            context, Request.Method.GET,
            "/messages?conversation_id=eq.${enc(conversationId)}&select=*,profiles(*)&order=created_at.asc",
            onSuccess = onSuccess, onError = onError
        )
    }

    fun sendMessage(context: Context, conversationId: String, content: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = currentUserId(context) ?: return onError("Not signed in")
        val row = JSONObject()
        row.put("conversation_id", conversationId)
        row.put("sender_id", uid)
        row.put("content", content)

        writeRequest(context, Request.Method.POST, "/messages", row, "return=representation", { onSuccess() }, onError)
    }

    private fun isoNow(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(java.util.Date())
    }
}
