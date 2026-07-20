
package com.vastlb.wing_me.Settings

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_contact.*
import org.json.JSONException
import org.json.JSONObject

class ContactActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        setViews()
    }

    fun setViews() {
        id_back.setOnClickListener {
            finish()
        }

        id_send.setOnClickListener {
            if (id_name_edit_text.text.toString().isEmpty() || id_email_edit_text.text.toString().isEmpty() || id_message_edit_text.text.toString().isEmpty()) {
                val toast = Toast.makeText(this, getString(R.string.alert_empty), Toast.LENGTH_LONG)
                toast.show()
            } else {
                id_send.visibility = View.GONE
                id_send_progress_bar.visibility = View.VISIBLE
                send()
            }
        }
    }

    fun send() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "contact_us.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    sendSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                sendError()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = token!!
                return headers
            }
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["language"] = getString(R.string.language)
                params["name"] = id_name_edit_text.text.toString()
                params["phone"] = id_email_edit_text.text.toString()
                params["text"] = id_message_edit_text.text.toString()
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun sendError() {
        println("Error2")
        send()
    }

    fun sendSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle(getString(R.string.alert_message_sent))
            alertDialog.setPositiveButton(getString(R.string.ok)) {
                _, _ ->
                finish()
            }
            val alert = alertDialog.create()
            alert.show()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_send.visibility = View.VISIBLE
        id_send_progress_bar.visibility = View.GONE
    }
}
