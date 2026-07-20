
package com.vastlb.wing_me.Settings

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Adapters.NotificationsAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.DataClasses.NotificationClass
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_notifications.*
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class NotificationsActivity: AppCompatActivity() {

    lateinit var adapter: NotificationsAdapter

    var array = ArrayList<NotificationClass>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        setViews()
        request()
    }

    fun setViews() {
        id_back.setOnClickListener {
            finish()
        }

        adapter = NotificationsAdapter(array)

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        id_recycler_view.layoutManager = linearLayoutManager
        id_recycler_view.adapter = adapter
    }

    fun request() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_notification.php"

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
            val message = json.getJSONArray("message")

            for (index in 0..(message.length() - 1)) {
                val jsonObject = message[index] as JSONObject
                val title = jsonObject.getString("title")
                val text = jsonObject.getString("text")
                val insert_at = jsonObject.getString("insert_at")

                val dateParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                val dateFormatter = SimpleDateFormat("dd MMMM yyyy - hh:mm a", Locale.getDefault())
                val dateString = dateFormatter.format(dateParser.parse(insert_at)!!)

                array.add(NotificationClass(title, text, dateString))
            }
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        adapter.notifyDataSetChanged()
        id_progress_bar.visibility = View.GONE
    }
}
