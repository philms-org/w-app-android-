
package com.vastlb.wing_me.User

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Adapters.LookingForAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.DataClasses.LookingForClass
import com.vastlb.wing_me.Main.MainActivity
import com.vastlb.wing_me.R
import com.vastlb.wing_me.SetupProfile.LexiconActivity
import kotlinx.android.synthetic.main.activity_social_settings.*
import org.json.JSONException
import org.json.JSONObject

class SocialSettingsActivity: AppCompatActivity() {

    lateinit var adapter: LookingForAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social_settings)
        setViews()
    }

    fun setViews() {
        val datingID = intent.getStringExtra("DatingID")!!
        val socialisingID = intent.getStringExtra("SocialisingID")!!
        val networkingID = intent.getStringExtra("NetworkingID")!!

        val array = Constants.getLookingForArray(getProgress(datingID), getProgress(socialisingID), getProgress(networkingID))

        id_back.setOnClickListener {
            finish()
        }

        id_info.setOnClickListener {
            val intent = Intent(this, LexiconActivity::class.java)
            startActivity(intent)
        }

        id_save.setOnClickListener {
            id_save.visibility = View.GONE
            id_save_progress_bar.visibility = View.VISIBLE
            send(array)
        }

        adapter = LookingForAdapter(array, this) {
            index, progress ->
            array[index].progress = progress
        }

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        id_recycler_view.layoutManager = linearLayoutManager
        id_recycler_view.adapter = adapter
    }

    fun getProgress(progress: String): Int {
        if (progress.isEmpty()) {
            return 0
        } else {
            return progress.toInt()
        }
    }

    fun send(array: ArrayList<LookingForClass>) {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "update_emojis.php"

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
                sendError(array)
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
                params["dating_Id"] = array[0].progress.toString()
                params["socialising_Id"] = array[1].progress.toString()
                params["networking_Id"] = array[2].progress.toString()
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun sendError(array: ArrayList<LookingForClass>) {
        println("Error2")
        send(array)
    }

    fun sendSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            Constants.inLocation = false

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
