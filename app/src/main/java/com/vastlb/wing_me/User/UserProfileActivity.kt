
package com.vastlb.wing_me.User

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.squareup.picasso.Picasso
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_user_profile.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class UserProfileActivity: AppCompatActivity() {

    var id = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        setViews()
        request()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Constants.reloadMessages()
        finish()
    }

    fun setViews() {
        id_scroll_view.visibility = View.GONE

        id = intent.getStringExtra("ID")!!
        val fromChat = intent.getBooleanExtra("FromChat", false)

        id_back.setOnClickListener {
            Constants.reloadMessages()
            finish()
        }

        id_menu.setOnClickListener {
            showMenu()
        }

        id_send_message.setOnClickListener {
            if (fromChat) {
                finish()
            } else {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("ID", id)
                startActivity(intent)
            }
        }
    }

    fun showMenu() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.select_option))
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val dialogItems = arrayOf(getString(R.string.block), getString(R.string.report))

        alertDialog.setItems(dialogItems) { dialog, which ->

            when (which) {
                0 -> {
                    showBlockAlert()
                }
                1 -> {
                    showReportAlert()
                }
            }
        }
        alertDialog.show()
    }

    fun showBlockAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.alert_block))
        alertDialog.setPositiveButton(getString(R.string.block)) {
            _, _ ->
            id_progress_bar.visibility = View.VISIBLE
            block()
        }
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val alert = alertDialog.create()
        alert.show()
    }

    fun showReportAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.alert_report))
        alertDialog.setPositiveButton(getString(R.string.report)) {
            _, _ ->
            id_progress_bar.visibility = View.VISIBLE
            report()
        }
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val alert = alertDialog.create()
        alert.show()
    }

    fun request() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_user_info.php"

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
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = token!!
                return headers
            }
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["language"] = getString(R.string.language)
                params["user_Id"] = id
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
            val image = message.getString("image")
            Picasso.get().load(Constants.url + image).into(id_image_view)

            val name = message.getString("name")
            val city = message.getString("city")
            val age = message.getString("age")
            val drink = message.getString("drink")
            val activity = message.getString("activity")
            val profession = message.getString("profession")

            id_name_text_view.setText(name)
            id_city_text_view.setText(city)
            id_age_text_view.setText(age)
            id_drink_text_view.setText(drink)
            id_friday_night_text_view.setText(activity)
            id_profession_text_view.setText(profession)

            val nationality = message.getString("nationality")

            if (nationality.isEmpty()) {
                id_nationality_text_view.setText("---")
            } else {
                val flags = Constants.getFlags()
                val jsonObject = flags.first {
                        jsonObject ->
                    jsonObject.id == nationality
                }
                val locale = Locale("en_US", jsonObject.id)
                val name = locale.getDisplayCountry()
                id_nationality_text_view.setText("${name} ${jsonObject.emoji}")
            }

            val gender = message.getString("gender")
            val genderString = getGender(gender)
            id_gender_text_view.setText(genderString)

            val height = message.getString("height")

            if (height.isEmpty()) {
                id_height_text_view.setText("---")
            } else {
                id_height_text_view.setText("${height}m")
            }

            val relationship = message.getString("relationship")

            if (relationship.isEmpty()) {
                id_relationship_text_view.setText("---")
            } else {
                val array = Constants.getRelationships(0)
                val jsonObject = array.first {
                        jsonObject ->
                    jsonObject.id == relationship
                }
                id_relationship_text_view.setText(jsonObject.title)
            }

            val datingID = message.getString("dating_Id")
            val socialisingID = message.getString("socialising_Id")
            val networkingID = message.getString("networking_Id")
            val lookingFor = Constants.getLookingFor(datingID, socialisingID, networkingID)

            if (lookingFor.isEmpty()) {
                id_looking_for_text_view.setText("---")
            } else {
                id_looking_for_text_view.setText(lookingFor)
            }

            if (gender == "F") {
                id_send_message_layout.setBackgroundResource(R.drawable.view_drawable_profile_button_pink)
            } else {
                id_send_message_layout.setBackgroundResource(R.drawable.view_drawable_profile_button_blue)
            }
            id_scroll_view.visibility = View.VISIBLE
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_progress_bar.visibility = View.GONE
    }

    fun getGender(gender: String): String {
        if (gender == "M") {
            return "Male"
        } else if (gender == "F") {
            return "Female"
        } else if (gender == "O") {
            return "Other"
        } else {
            return ""
        }
    }

    fun block() {
        val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "block_user.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    blockSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                blockError()
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
                params["user_Id"] = id
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun blockError() {
        println("Error2")
        block()
    }

    fun blockSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            Constants.reloadLocation()
            Constants.reloadMessages()
            finish()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_progress_bar.visibility = View.GONE
    }

    fun report() {
        val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "report_user.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    reportSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                reportError()
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
                params["user_Id"] = id
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun reportError() {
        println("Error2")
        report()
    }

    fun reportSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle(getString(R.string.alert_reported))
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
        id_progress_bar.visibility = View.GONE
    }
}
