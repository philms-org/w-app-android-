
package com.vastlb.wing_me.SetupProfile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.Main.MainActivity
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_profile_setup_first.*
import org.json.JSONException
import org.json.JSONObject

class FirstProfileSetupActivity: AppCompatActivity() {

    val minimum = 1.2
    val maximum = 2.6

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
        setContentView(R.layout.activity_profile_setup_first)
        setViews()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java", ReplaceWith("showBackAlert()"))
    override fun onBackPressed() {
        showBackAlert()
    }

    @SuppressLint("SetTextI18n")
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

        if (height.isEmpty()) {
            id_height_text_view.setText("---")
        } else {
            val difference = maximum - minimum
            val progress = (height.toDouble() - minimum) / difference
            id_seek_bar.setProgress((progress * 100).toInt())
            id_height_text_view.setText("${height}m")
        }

        id_seek_bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {

            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val difference = maximum - minimum
                val height = minimum + difference * progress.toDouble()/100
                val rounded: Double = Math.round(height * 100.0) / 100.0
                this@FirstProfileSetupActivity.height = rounded.toString()
                id_height_text_view.setText("${rounded}m")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        id_back.setOnClickListener {
            showBackAlert()
        }

        id_previous.setOnClickListener {
            finish()
        }

        id_next.setOnClickListener {
            val intent = Intent(this, SecondProfileSetupActivity::class.java)
            intent.putExtra("Height", height)
            intent.putExtra("Relationship", relationship)
            intent.putExtra("DatingID", datingID)
            intent.putExtra("SocialisingID", socialisingID)
            intent.putExtra("NetworkingID", networkingID)
            intent.putExtra("Nationality", nationality)
            intent.putExtra("City", city)
            intent.putExtra("Drink", drink)
            intent.putExtra("Activity", activity)
            intent.putExtra("Profession", profession)
            startActivity(intent)
        }
    }

    fun showBackAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Do you want to save your changed info?")
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val dialogItems = arrayOf("Save Changes", "Discard Changes")

        alertDialog.setItems(dialogItems) { dialog, which ->

            when (which) {
                0 -> {
                    id_back.visibility = View.GONE
                    id_progress_bar.visibility = View.VISIBLE
                    send()
                }
                1 -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
            }
        }
        alertDialog.show()
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
                params["city"] = city
                params["drink"] = drink
                params["activity"] = activity
                params["profession"] = profession
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
            val intent = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_back.visibility = View.VISIBLE
        id_progress_bar.visibility = View.GONE
    }
}
