
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
import kotlinx.android.synthetic.main.activity_change_password.*
import org.json.JSONException
import org.json.JSONObject

class ChangePasswordActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)
        setViews()
    }

    fun setViews() {
        id_back.setOnClickListener {
            finish()
        }

        id_confirm.setOnClickListener {
            if (id_old_edit_text.text.toString().isEmpty() || id_new_edit_text.text.toString().isEmpty() || id_confirm_edit_text.text.toString().isEmpty()) {
                val toast = Toast.makeText(this, getString(R.string.alert_empty), Toast.LENGTH_LONG)
                toast.show()
            } else if (id_new_edit_text.text.toString() != id_confirm_edit_text.text.toString()) {
                val toast = Toast.makeText(this, getString(R.string.alert_both), Toast.LENGTH_LONG)
                toast.show()
            } else {
                id_confirm.visibility = View.GONE
                id_confirm_progress_bar.visibility = View.VISIBLE
                change()
            }
        }
    }

    fun change() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "change_password.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    changeSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                changeError()
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
                params["old_password"] = id_old_edit_text.text.toString()
                params["new_password"] = id_new_edit_text.text.toString()
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun changeError() {
        println("Error2")
        change()
    }

    fun changeSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val message = json.getJSONObject("message")
            val token = message.getString("token")

            val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString("Token", token)
            editor.apply()

            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle(getString(R.string.alert_password_changed))
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
        id_confirm.visibility = View.VISIBLE
        id_confirm_progress_bar.visibility = View.GONE
    }
}
