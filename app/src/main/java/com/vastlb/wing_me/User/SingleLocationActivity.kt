
package com.vastlb.wing_me.User

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Adapters.LocationBannerAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_single_location.*
import org.json.JSONException
import org.json.JSONObject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SingleLocationActivity: AppCompatActivity() {

    lateinit var adapter: LocationBannerAdapter

    var array = ArrayList<String>()

    var id = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_location)
        setViews()
        request()
    }

    fun setViews() {
        id_scroll_view.visibility = View.GONE
        id_bottom_layout.visibility = View.GONE

        id = intent.getStringExtra("ID")!!

        id_back.setOnClickListener {
            finish()
        }

        adapter = LocationBannerAdapter(array)

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val snapHelper = PagerSnapHelper()
        id_recycler_view.layoutManager = linearLayoutManager
        id_recycler_view.adapter = adapter
        snapHelper.attachToRecyclerView(id_recycler_view)
    }

    fun request() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_location.php"

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
                params["Id"] = id
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun connectionError() {
        println("Error2")
        request()
    }

    @SuppressLint("SetTextI18n")
    fun requestSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val message = json.getJSONObject("message")
            val banner = message.getJSONArray("banner")
            val name = message.getString("name")
            val description = message.getString("description")
            val address = message.getString("address")
            val country_name = message.getString("country_name")
            val name_city = message.getString("name_city")
            val whatsapp = message.getString("whatsapp")

            for (index in 0..(banner.length() - 1)) {
                val jsonObject = banner[index] as JSONObject
                val image = jsonObject.getString("image")
                array.add(image)
            }
            if (array.isEmpty()) {
                val image = message.getString("image")
                array.add(image)
            }
            id_name_text_view.setText(name)
            id_details_text_view.setText(description)
            id_city_text_view.setText("${country_name} - ${name_city}")
            id_address_text_view.setText(address)

            if (whatsapp.isEmpty()) {
                id_bottom_layout.visibility = View.GONE
            } else {
                id_bottom_layout.visibility = View.VISIBLE

                id_book.setOnClickListener {
                    openURL(whatsapp)
                }
            }
            id_scroll_view.visibility = View.VISIBLE
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        adapter.notifyDataSetChanged()
        id_progress_bar.visibility = View.GONE
    }

    fun openURL(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        try {
            startActivity(intent)
        } catch (e: Exception) {

        }
    }
}
