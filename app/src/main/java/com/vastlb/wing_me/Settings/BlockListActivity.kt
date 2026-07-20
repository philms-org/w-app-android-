
package com.vastlb.wing_me.Settings

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Adapters.BlockedUsersAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.DataClasses.BlockedUserClass
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_block_list.*
import org.json.JSONException
import org.json.JSONObject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BlockListActivity: AppCompatActivity() {

    lateinit var adapter: BlockedUsersAdapter

    var array = ArrayList<BlockedUserClass>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_list)
        setViews()
        request()
    }

    fun setViews() {
        id_back.setOnClickListener {
            finish()
        }

        adapter = BlockedUsersAdapter(array) {
            index ->
            val jsonObject = array[index]
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle(getString(R.string.select_option))
            alertDialog.setNegativeButton(getString(R.string.cancel), null)
            val dialogItems = arrayOf(getString(R.string.unblock))

            alertDialog.setItems(dialogItems) { dialog, which ->
                showUnblockAlert(jsonObject.id)
            }
            alertDialog.show()
        }

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        id_recycler_view.layoutManager = linearLayoutManager
        id_recycler_view.adapter = adapter
    }

    fun showUnblockAlert(id: String) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.alert_unblock))
        alertDialog.setPositiveButton(getString(R.string.unblock)) {
            _, _ ->
            id_progress_bar.visibility = View.VISIBLE
            unblock(id)
        }
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val alert = alertDialog.create()
        alert.show()
    }

    fun request() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "block_list.php"

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
                val image = jsonObject.getString("image")
                val Id = jsonObject.getString("Id")
                val name = jsonObject.getString("name")

                array.add(BlockedUserClass(image, Id, name))
            }
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        adapter.notifyDataSetChanged()
        id_progress_bar.visibility = View.GONE
    }

    fun unblock(id: String) {
        val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "block_user.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    unblockSuccess(response, id)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                unblockError(id)
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

    fun unblockError(id: String) {
        println("Error2")
        unblock(id)
    }

    fun unblockSuccess(response: String, id: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            Constants.reloadMessages()

            val index = array.indexOfFirst {
                jsonObject ->
                jsonObject.id == id
            }
            array.removeAt(index)
            adapter.notifyItemRemoved(index)
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_progress_bar.visibility = View.GONE
    }
}
