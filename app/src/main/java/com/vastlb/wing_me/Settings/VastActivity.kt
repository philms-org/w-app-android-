
package com.vastlb.wing_me.Settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_vast.*
import org.json.JSONException
import org.json.JSONObject

class VastActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vast)
        setViews()
        request()
    }

    fun setViews() {
        id_bottom_layout.visibility = View.GONE

        id_back.setOnClickListener {
            finish()
        }
    }

    fun request() {
        val url = Constants.url + "vast.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    requestSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                connectionError()
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["language"] = getString(R.string.language)
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun connectionError() {
        println("Error2")
        request()
    }

    fun requestSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val message = json.getJSONObject("message")
            val text = message.getString("text")
            val facebook = message.getString("facebook")
            val instagram = message.getString("instagram")
            val twitter = message.getString("twitter")
            val whatsapp = message.getString("whatsapp")
            val phone = message.getString("phone")

            id_text_view.setText(text)

            if (facebook.isEmpty()) {
                id_facebook.visibility = View.GONE
            } else {
                id_facebook.visibility = View.VISIBLE

                id_facebook.setOnClickListener {
                    openURL(facebook)
                }
            }

            if (instagram.isEmpty()) {
                id_instagram.visibility = View.GONE
            } else {
                id_instagram.visibility = View.VISIBLE

                id_instagram.setOnClickListener {
                    openURL(instagram)
                }
            }

            if (twitter.isEmpty()) {
                id_twitter.visibility = View.GONE
            } else {
                id_twitter.visibility = View.VISIBLE

                id_twitter.setOnClickListener {
                    openURL(twitter)
                }
            }

            if (whatsapp.isEmpty()) {
                id_whatsapp.visibility = View.GONE
            } else {
                id_whatsapp.visibility = View.VISIBLE

                id_whatsapp.setOnClickListener {
                    openURL(whatsapp)
                }
            }

            if (phone.isEmpty()) {
                id_call.visibility = View.GONE
            } else {
                id_call.visibility = View.VISIBLE

                id_call.setOnClickListener {
                    call(phone)
                }
            }
            id_bottom_layout.visibility = View.VISIBLE
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_progress_bar.visibility = View.GONE
    }

    fun openURL(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        try {
            startActivity(intent)
        } catch (e: Exception) {

        }
    }

    fun call(phone: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 42)
            return
        }
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone))

        try {
            startActivity(intent)
        } catch (e: java.lang.Exception) {

        }
    }
}
