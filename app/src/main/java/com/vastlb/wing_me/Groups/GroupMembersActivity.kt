
package com.vastlb.wing_me.Groups

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Adapters.BlockedUsersAdapter
import com.vastlb.wing_me.Adapters.GroupMembersAdapter
import com.vastlb.wing_me.Adapters.UsersAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.DataClasses.BlockedUserClass
import com.vastlb.wing_me.DataClasses.UserClass
import com.vastlb.wing_me.R
import com.vastlb.wing_me.User.ChatActivity
import com.vastlb.wing_me.User.UserProfileActivity
import kotlinx.android.synthetic.main.activity_chat_group.id_progress_bar
import kotlinx.android.synthetic.main.activity_group_members.*
import org.json.JSONException
import org.json.JSONObject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class GroupMembersActivity: AppCompatActivity() {

    lateinit var adapter: GroupMembersAdapter

    var array = ArrayList<UserClass>()

    var groupID = ""
    var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_members)
        setViews()
        request()
    }

    fun setViews() {
        groupID = intent.getStringExtra("ID")!!
        isAdmin = intent.getBooleanExtra("IsAdmin", false)

        id_back.setOnClickListener {
            finish()
        }

        adapter = GroupMembersAdapter(this, array, select = {
            index ->
            val jsonObject = array[index]

            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("ID", jsonObject.id)
            startActivity(intent)
        }, longClick = {
            index ->
            val jsonObject = array[index]

            if (isAdmin) {
                showDeleteSheet(jsonObject.id)
            }
        })

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        id_recycler_view.layoutManager = linearLayoutManager
        id_recycler_view.adapter = adapter
    }

    fun showDeleteSheet(id: String) {
        val alertDialog = android.app.AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.select_option))
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val dialogItems = arrayOf("Remove Member")

        alertDialog.setItems(dialogItems) {
            dialog, which ->

            when (which) {
                0 -> {
                    id_progress_bar.visibility = View.VISIBLE
                    removeMember(id)
                }
            }
        }
        alertDialog.show()
    }

    fun reload() {
        array.clear()
        adapter.notifyDataSetChanged()

        id_progress_bar.visibility = View.VISIBLE
        request()
    }

    fun request() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_group_users.php"

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
                params["comment_Id"] = groupID
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
                val image = jsonObject.getString("image")
                val Id = jsonObject.getString("Id")
                val name = jsonObject.getString("name")
                val gender = jsonObject.getString("gender")
                val age = jsonObject.getString("age")
                val city = jsonObject.getString("city")
                val nationality = jsonObject.getString("nationality")
                val is_master_account = jsonObject.getString("is_master_account")

                var details = ""

                if (age.isEmpty()) {
                    if (city.isEmpty()) {
                        if (nationality.isEmpty()) {
                            details = ""
                        } else {
                            val flags = Constants.getFlags()
                            val jsonObject = flags.first {
                                jsonObject ->
                                jsonObject.id == nationality
                            }
                            details = jsonObject.emoji
                        }
                    } else {
                        if (nationality.isEmpty()) {
                            details = city
                        } else {
                            val flags = Constants.getFlags()
                            val jsonObject = flags.first {
                                jsonObject ->
                                jsonObject.id == nationality
                            }
                            details = "${city} ${jsonObject.emoji}"
                        }
                    }
                } else {
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
                }
                val isMaster = (is_master_account == "1")

                array.add(UserClass(image, Id, name, details, gender, isMaster))
            }
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        adapter.notifyDataSetChanged()
        id_progress_bar.visibility = View.GONE
    }

    fun removeMember(id: String) {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "remove_user_group.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    removeMemberSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                removeMemberError(id)
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
                params["comment_Id"] = groupID
                params["user_Id"] = id
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun removeMemberError(id: String) {
        println("Error2")
        removeMember(id)
    }

    fun removeMemberSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            reload()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_progress_bar.visibility = View.GONE
    }
}
