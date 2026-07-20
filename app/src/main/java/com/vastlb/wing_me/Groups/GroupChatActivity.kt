
package com.vastlb.wing_me.Groups

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.squareup.picasso.Picasso
import com.vastlb.wing_me.Adapters.GroupChatAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.CoreData.Message
import com.vastlb.wing_me.CoreData.MessageDao
import com.vastlb.wing_me.CoreData.MessageDatabase
import com.vastlb.wing_me.DataClasses.ChatDateClass
import com.vastlb.wing_me.DataClasses.GroupChatMessageClass
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_chat_group.*
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class GroupChatActivity: AppCompatActivity() {

    lateinit var groupsDao: MessageDao

    lateinit var chatAdapter: GroupChatAdapter

    var array = ArrayList<Any>()

    var id = ""
    var lastDate = ""
    var imageURL = ""
    var titleString = ""
    var isAdmin = false
    var isMute = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_group)
        setViews()
        request()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Constants.setChatMessage = null
        finish()
    }

    fun setViews() {
        Constants.reloadGroup = {
            groupsDao = Room.databaseBuilder(this, MessageDatabase::class.java, "Groups").allowMainThreadQueries().build().dataDao()

            array.clear()
            lastDate = ""

            chatAdapter.notifyDataSetChanged()
            id_progress_bar.visibility = View.VISIBLE
            request()
        }

        id_name_text_view.setText("")

        if (intent.getStringExtra("ID") == null) {
            val extras = intent.extras!!
            id = extras.getString("comment_Id", "")
        } else {
            id = intent.getStringExtra("ID")!!
        }
        Constants.chatGroupID = id

        Constants.setGroupMessage = {
            userName, gender, messageID, message, date ->

            runOnUiThread {
                setMessage("receiver", userName, gender, message, date)
                saveLastMessage(messageID)

                if (Constants.setLastGroup != null) {
                    Constants.setLastGroup!!(id, message, date, false)
                }
                chatAdapter.notifyDataSetChanged()
            }
        }

        groupsDao = Room.databaseBuilder(this, MessageDatabase::class.java, "Groups").allowMainThreadQueries().build().dataDao()

        id_back.setOnClickListener {
            Constants.setGroupMessage = null
            finish()
        }

        id_menu.setOnClickListener {
            showMenu()
        }

        id_send.setOnClickListener {
            if (!id_message_edit_text.text.toString().isEmpty()) {
                id_send.visibility = View.GONE
                id_send_progress_bar.visibility = View.VISIBLE
                send()
            }
        }

        chatAdapter = GroupChatAdapter(array)

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        id_recycler_view.layoutManager = linearLayoutManager
        id_recycler_view.adapter = chatAdapter
    }

    fun showMenu() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.select_option))
        alertDialog.setNegativeButton(getString(R.string.cancel), null)

        val dialogItems = ArrayList<String>()

        if (isAdmin) {
            dialogItems.add("Edit Group")
        }
        dialogItems.add("Group Members")

        if (isMute) {
            dialogItems.add("Unmute Group")
        } else {
            dialogItems.add("Mute Group")
        }

        if (isAdmin) {
            dialogItems.add("Delete Group")
        } else {
            dialogItems.add("Leave Group")
        }

        alertDialog.setItems(dialogItems.toTypedArray()) {
            dialog, which ->
            if (dialogItems[which] == "Edit Group") {
                val intent = Intent(this, EditGroupActivity::class.java)
                intent.putExtra("GroupID", id)
                intent.putExtra("Image", imageURL)
                intent.putExtra("Name", titleString)
                startActivity(intent)
            } else if (dialogItems[which] == "Group Members") {
                val intent = Intent(this, GroupMembersActivity::class.java)
                intent.putExtra("ID", id)
                intent.putExtra("IsAdmin", isAdmin)
                startActivity(intent)
            } else if (dialogItems[which] == "Unmute Group") {
                id_progress_bar.visibility = View.VISIBLE
                mute(false)
            } else if (dialogItems[which] == "Mute Group") {
                id_progress_bar.visibility = View.VISIBLE
                mute(true)
            } else if (dialogItems[which] == "Delete Group") {
                showDeleteAlert()
            } else if (dialogItems[which] == "Leave Group") {
                showLeaveAlert()
            }
        }
        alertDialog.show()
    }

    fun showDeleteAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.alert_delete_group))
        alertDialog.setPositiveButton(getString(R.string.yes)) {
            _, _ ->
            id_progress_bar.visibility = View.VISIBLE
            delete()
        }
        alertDialog.setNegativeButton(getString(R.string.no), null)
        val alert = alertDialog.create()
        alert.show()
    }

    fun showLeaveAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.alert_leave_group))
        alertDialog.setPositiveButton(getString(R.string.yes)) {
            _, _ ->
            id_progress_bar.visibility = View.VISIBLE
            leave()
        }
        alertDialog.setNegativeButton(getString(R.string.no), null)
        val alert = alertDialog.create()
        alert.show()
    }

    fun reload() {

    }

    fun request() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_group_inbox.php"

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
                params["comment_Id"] = id
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun connectionError() {
        println("Error2")
        request()
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    fun requestSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val message = json.getJSONObject("message")
            val messages = message.getJSONArray("messages")
            imageURL = message.getString("group_image")
            titleString = message.getString("title")
            isAdmin = message.getBoolean("is_admin")
            isMute = message.getBoolean("is_mute")

            Picasso.get().load(Constants.url + imageURL).into(id_image_view)

            id_name_text_view.setText(titleString)

            for (index in 0..(messages.length() - 1)) {
                val jsonObject = messages[index] as JSONObject
                val user = jsonObject.getJSONObject("user")
                val message_Id = jsonObject.getString("message_Id")
                val type = jsonObject.getString("message_type")
                val name = user.getString("name")
                val gender = user.getString("gender")
                val text = jsonObject.getString("text")
                val insert_at = jsonObject.getString("insert_at")

                setMessage(type, name, gender, text, insert_at)

                if (index == messages.length() - 1) {
                    saveLastMessage(message_Id)
                }
            }
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        chatAdapter.notifyDataSetChanged()
        id_progress_bar.visibility = View.GONE
    }

    fun setMessage(type: String, userName: String, gender: String, message: String, date: String) {
        val dateParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("hh:mm aa", Locale.getDefault())

        val date = dateParser.parse(date)!!
        val today = Calendar.getInstance().time

        val dateString = dateFormatter.format(date)
        val timeString = timeFormatter.format(date)

        val difference = getStartOfDay(today).time - getStartOfDay(date).time
        val days = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS).toInt()

        if (dateString != lastDate) {
            if (days == 0) {
                array.add(0, ChatDateClass("Today"))
            } else if (days == 1) {
                array.add(0, ChatDateClass("Yesterday"))
            } else {
                array.add(0, ChatDateClass(dateString))
            }
            lastDate = dateString
        }
        array.add(0, GroupChatMessageClass(type, userName, gender, message, timeString))
    }

    fun saveLastMessage(messageID: String) {
        val array = groupsDao.getAll()

        if (array.any {
            jsonObject ->
            jsonObject.userID == id
        }) {
            val jsonObject = groupsDao.getItemByUserID(id)
            jsonObject.lastMessageID = messageID
            groupsDao.update(jsonObject)
        } else {
            groupsDao.insert(Message(null, id, messageID))
        }
    }

    fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return calendar.time
    }

    fun send() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "send_group_inbox.php"

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
                params["comment_Id"] = id
                params["text"] = id_message_edit_text.text.toString()
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
            val message = json.getJSONObject("message")
            val date = message.getString("date")
            val message_Id = message.getString("message_Id")

            setMessage("sender", "", "", id_message_edit_text.text.toString(), date)
            saveLastMessage(message_Id)

            if (Constants.setLastGroup != null) {
                Constants.setLastGroup!!(id, id_message_edit_text.text.toString(), date, false)
            }
            id_message_edit_text.setText("")
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        chatAdapter.notifyDataSetChanged()
        id_send.visibility = View.VISIBLE
        id_send_progress_bar.visibility = View.GONE
    }

    fun mute(mute: Boolean) {
        val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "mute_group.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    muteSuccess(response, mute)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                muteError(mute)
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
                params["comment_Id"] = id
                params["mute"] = if (mute) "1" else "0"
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun muteError(mute: Boolean) {
        println("Error2")
        mute(mute)
    }

    fun muteSuccess(response: String, mute: Boolean) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            isMute = mute

            if (Constants.reloadGroups != null) {
                Constants.reloadGroups()
            }
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_progress_bar.visibility = View.GONE
    }

    fun delete() {
        val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "delete_group.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    deleteSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                deleteError()
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
                params["comment_Id"] = id
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun deleteError() {
        println("Error2")
        delete()
    }

    fun deleteSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            if (Constants.reloadGroups != null) {
                Constants.reloadGroups()
            }
            Constants.setGroupMessage = null
            finish()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_progress_bar.visibility = View.GONE
    }

    fun leave() {
        val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "leave_group.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    leaveSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                leaveError()
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
                params["comment_Id"] = id
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun leaveError() {
        println("Error2")
        delete()
    }

    fun leaveSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            if (Constants.reloadGroups != null) {
                Constants.reloadGroups()
            }
            Constants.setGroupMessage = null
            finish()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_progress_bar.visibility = View.GONE
    }
}
