
package com.vastlb.wing_me.Main

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Adapters.UsersAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.DataClasses.UserClass
import com.vastlb.wing_me.R
import com.vastlb.wing_me.SetupProfile.LexiconActivity
import com.vastlb.wing_me.User.ChatActivity
import com.vastlb.wing_me.User.SocialSettingsActivity
import com.vastlb.wing_me.User.UserProfileActivity
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MyLocationFragment: Fragment() {

    lateinit var adapter: UsersAdapter

    lateinit var noUsersLayout: RelativeLayout
    lateinit var noLocationLayout: RelativeLayout
    lateinit var progressBar: ProgressBar

    val array = ArrayList<UserClass>()

    var timer: CountDownTimer? = null

    var inLocation = false
    var locationID = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_my_location, container, false)
        setViews(view)
        return view
    }

    companion object {
        fun newInstance(): MyLocationFragment = MyLocationFragment()
    }

    fun setViews(view: View) {
        val locationLayout: RelativeLayout = view.findViewById(R.id.id_location_layout)
        val locationTitleTextView: TextView = view.findViewById(R.id.id_location_title_text_view)
        val info: ImageView = view.findViewById(R.id.id_info)

        val recyclerView: RecyclerView = view.findViewById(R.id.id_recycler_view)
        noLocationLayout = view.findViewById(R.id.id_no_location_layout)
        noUsersLayout = view.findViewById(R.id.id_no_users_layout)
        val share: RelativeLayout = view.findViewById(R.id.id_share)
        val shareApp: RelativeLayout = view.findViewById(R.id.id_share_app)
        progressBar = view.findViewById(R.id.id_progress_bar)

        Constants.reloadLocation = {
            reload()
        }

        Constants.wingMe = {
            jsonObject ->
            inLocation = true
            locationID = jsonObject.id

            locationTitleTextView.setText(jsonObject.name)

            val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString("LocationID", jsonObject.id)
            editor.putString("LocationName", jsonObject.name)
            editor.apply()

            noLocationLayout.visibility = View.GONE
            setTimer()
            reload()

//            if (isNewDate()) {
//                val calendar = Calendar.getInstance()
//                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                val dateString = dateFormatter.format(calendar.time)
//
//                editor.putString("LastDate", dateString)
//                editor.apply()
//
//                val alertTitle = "Looking for something different this time?"
//                val alertBody = "Adjust your social settings for 24hrs"
//
//                val alertDialog = AlertDialog.Builder(context)
//                alertDialog.setTitle(alertTitle)
//                alertDialog.setMessage(alertBody)
//                alertDialog.setPositiveButton(getString(R.string.yes)) {
//                    _, _ ->
//                    openSocialSettings()
//                }
//                alertDialog.setNegativeButton(getString(R.string.no), null)
//                val alert = alertDialog.create()
//                alert.show()
//            }
        }

        Constants.hideLocation = {
            hideLocation()
        }

        adapter = UsersAdapter(requireContext(), array) {
            index ->
            val jsonObject = array[index]

            if (jsonObject.id == "0") {
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra("ID", jsonObject.id)
                startActivity(intent)
            } else {
                val intent = Intent(context, UserProfileActivity::class.java)
                intent.putExtra("ID", jsonObject.id)
                startActivity(intent)
            }
        }

        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter

        info.setOnClickListener {
            val intent = Intent(context, LexiconActivity::class.java)
            startActivity(intent)
        }

        locationLayout.setOnClickListener {
            val alertDialog = AlertDialog.Builder(context)
            alertDialog.setTitle(getString(R.string.select_option))
            alertDialog.setNegativeButton(getString(R.string.cancel), null)
            val dialogItems = arrayOf("Wing Out")

            alertDialog.setItems(dialogItems) {
                _, _ ->
                Constants.inLocation = false
                hideLocation()

                val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
                val editor = preferences.edit()
                editor.remove("LocationID")
                editor.remove("LocationName")
                editor.apply()
            }
            alertDialog.show()
        }

        share.setOnClickListener {
            val url = Constants.appURL
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
            startActivity(Intent.createChooser(shareIntent, "Share link using"))
        }

        shareApp.setOnClickListener {
            val url = Constants.appURL
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
            startActivity(Intent.createChooser(shareIntent, "Share link using"))
        }
    }

    fun hideLocation() {
        inLocation = false
        noLocationLayout.visibility = View.VISIBLE

        if (timer != null) {
            timer!!.cancel()
        }
        wingOff()
    }

    fun setTimer() {
        timer = object: CountDownTimer(1000000, 10000) {

            override fun onTick(millisUntilFinished: Long) {
                reload()
            }

            override fun onFinish() {

            }
        }
        timer!!.start()
    }

    fun isNewDate(): Boolean {
        val context = context ?: return true

        val preferences = context.getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val lastDate = preferences.getString("LastDate", "")!!

        if (lastDate.isEmpty()) {
            return true
        }
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormatter.format(calendar.time)

        if (dateString == lastDate) {
            return false
        }
        return true
    }

    fun openSocialSettings() {
        val intent = Intent(context, SocialSettingsActivity::class.java)
        intent.putExtra("DatingID", Constants.datingID)
        intent.putExtra("SocialisingID", Constants.socialisingID)
        intent.putExtra("NetworkingID", Constants.networkingID)
        startActivity(intent)
    }

    fun reload() {
        if (!inLocation) {
            return
        }
        array.clear()
        adapter.notifyDataSetChanged()
        progressBar.visibility = View.VISIBLE
        request()
    }

    fun request() {
        val context = context ?: return

        val preferences = context.getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_users.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    requestSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
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
                params["location_Id"] = locationID
                return params
            }
        }
        Singleton.getInstance(requireContext()).addToRequestQueue(request)
    }

    fun connectionError() {
        println("Error2")
        request()
    }

    fun requestSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            array.clear()

            val message = json.getJSONArray("message")

            for (index in 0..(message.length() - 1)) {
                val jsonObject = message[index] as JSONObject
                val image = jsonObject.getString("image")
                val Id = jsonObject.getString("Id")
                val name = jsonObject.getString("name")
                val gender = jsonObject.getString("gender")
                val age = jsonObject.getString("age")
                val city = jsonObject.getString("city")
                val nationality = jsonObject.getString("nationality")
                val is_master_account = jsonObject.getString("is_master_account")

                var details = ""

                if (city.isEmpty()) {
                    if (nationality.isEmpty()) {
                        details = "Age: ${age}"
                    } else {
                        val flags = Constants.getFlags()
                        val jsonObject = flags.first {
                            jsonObject ->
                            jsonObject.id == nationality
                        }
                        details = "Age: ${age} ${jsonObject.emoji}"
                    }
                } else {
                    if (nationality.isEmpty()) {
                        details = "Age: ${age}, ${city}"
                    } else {
                        val flags = Constants.getFlags()
                        val jsonObject = flags.first {
                            jsonObject ->
                            jsonObject.id == nationality
                        }
                        details = "Age: ${age}, ${city} ${jsonObject.emoji}"
                    }
                }
                val isMaster = (is_master_account == "1")

                array.add(UserClass(image, Id, name, details, gender, isMaster))
            }
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
        }
        if (array.isEmpty()) {
            noUsersLayout.visibility = View.VISIBLE
        } else {
            noUsersLayout.visibility = View.GONE
        }
        adapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
    }

    fun wingOff() {
        val context = context ?: return

        val preferences = context.getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "wing_off.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    wingOffSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                wingOffError()
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
                params["location_Id"] = locationID
                return params
            }
        }
        Singleton.getInstance(requireContext()).addToRequestQueue(request)
    }

    fun wingOffError() {
        println("Error2")
        wingOff()
    }

    fun wingOffSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {

        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
        }
    }
}
