
package com.vastlb.wing_me.SetupProfile

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.Main.MainActivity
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_profile_setup_fifth.*
import org.json.JSONException
import org.json.JSONObject

class FifthProfileSetupActivity: AppCompatActivity() {

    var height = ""
    var relationship = ""
    var datingID = ""
    var socialisingID = ""
    var networkingID = ""
    var nationality = ""
    var city = ""
    var drink = ""
    var activity = ""
    var profession = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup_fifth)
        setViews()
    }

    fun setViews() {
        height = intent.getStringExtra("Height")!!
        relationship = intent.getStringExtra("Relationship")!!
        datingID = intent.getStringExtra("DatingID")!!
        socialisingID = intent.getStringExtra("SocialisingID")!!
        networkingID = intent.getStringExtra("NetworkingID")!!
        nationality = intent.getStringExtra("Nationality")!!
        city = intent.getStringExtra("City")!!
        drink = intent.getStringExtra("Drink")!!
        activity = intent.getStringExtra("Activity")!!
        profession = intent.getStringExtra("Profession")!!

        id_city_edit_text.setText(city)
        id_drink_edit_text.setText(drink)
        id_friday_activity_edit_text.setText(activity)
        id_profession_edit_text.setText(profession)

        id_back.setOnClickListener {
            finish()
        }

        id_save.setOnClickListener {
            id_save.visibility = View.GONE
            id_save_progress_bar.visibility = View.VISIBLE
            send()
        }
    }

    fun send() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "set_up_profile.php"

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
                params["height"] = height
                params["relationship"] = relationship
                params["dating_Id"] = datingID
                params["socialising_Id"] = socialisingID
                params["networking_Id"] = networkingID
                params["nationality"] = nationality
                params["city"] = id_city_edit_text.text.toString()
                params["drink"] = id_drink_edit_text.text.toString()
                params["activity"] = id_friday_activity_edit_text.text.toString()
                params["profession"] = id_profession_edit_text.text.toString()
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
            val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString("Setup", "true")
            editor.apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_save.visibility = View.VISIBLE
        id_save_progress_bar.visibility = View.GONE
    }
}
