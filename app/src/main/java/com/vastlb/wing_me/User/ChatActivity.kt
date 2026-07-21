
package com.vastlb.wing_me.User

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.squareup.picasso.Picasso
import com.vastlb.wing_me.Adapters.ChatAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.LocalMessage
import com.vastlb.wing_me.Classes.LocalMessageStore
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.DataClasses.ChatDateClass
import com.vastlb.wing_me.DataClasses.ChatMessageClass
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_chat.*
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ChatActivity: AppCompatActivity() {

    lateinit var messageDao: LocalMessageStore

    lateinit var chatAdapter: ChatAdapter

    var array = ArrayList<Any>()

    var id = ""
    var blocked = ""
    var lastDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
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
        id_name_text_view.setText("")
        id_details_text_view.setText("")

        if (intent.getStringExtra("ID") == null) {
            val extras = intent.extras!!
            id = extras.getString("user_Id", "")
        } else {
            id = intent.getStringExtra("ID")!!
        }
        if (id == "0") {
            id_menu.visibility = View.GONE
            id_details_text_view.visibility = View.GONE
        }
        Constants.chatUserID = id

        Constants.setChatMessage = {
            messageID, message, date ->

            runOnUiThread {
                setMessage("receive", message, date)
                saveLastMessage(messageID)

                if (Constants.setLastMessage != null) {
                    Constants.setLastMessage!!(id, message, date, false)
                }
                chatAdapter.notifyDataSetChanged()
            }
        }

        messageDao = LocalMessageStore.named("Messages")

        id_back.setOnClickListener {
            Constants.setChatMessage = null
            finish()
        }

        id_menu.setOnClickListener {
            showMenu()
        }

        id_send.setOnClickListener {
            if (id_message_edit_text.text.toString().isEmpty()) {
                return@setOnClickListener
            }
            id_send.visibility = View.GONE
            id_send_progress_bar.visibility = View.VISIBLE

            if (id == "0") {
                sendMaster()
            } else {
                send()
            }
        }

        chatAdapter = ChatAdapter(array)

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        id_recycler_view.layoutManager = linearLayoutManager
        id_recycler_view.adapter = chatAdapter
    }

    fun showMenu() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.select_option))
        alertDialog.setNegativeButton(getString(R.string.cancel), null)

        if (blocked == "1") {
            val dialogItems = arrayOf(getString(R.string.unblock))

            alertDialog.setItems(dialogItems) { dialog, which ->
                showUnblockAlert()
            }
            alertDialog.show()
        } else {
            val dialogItems = arrayOf("View Profile", getString(R.string.block), getString(R.string.report))

            alertDialog.setItems(dialogItems) { dialog, which ->

                when (which) {
                    0 -> {
                        val intent = Intent(this, UserProfileActivity::class.java)
                        intent.putExtra("ID", id)
                        intent.putExtra("FromChat", true)
                        startActivity(intent)
                    }
                    1 -> {
                        showBlockAlert()
                    }
                    2 -> {
                        showReportAlert()
                    }
                }
            }
            alertDialog.show()
        }
    }

    fun showUnblockAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.alert_unblock))
        alertDialog.setPositiveButton(getString(R.string.unblock)) {
            _, _ ->
            id_progress_bar.visibility = View.VISIBLE
            block()
        }
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val alert = alertDialog.create()
        alert.show()
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
        val url = Constants.url + "get_inbox.php"

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

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    fun requestSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val message = json.getJSONObject("message")
            val users = message.getJSONObject("users")
            val inbox = message.getJSONArray("inbox")
            val image = users.getString("image")
            val name = users.getString("name")
            val datingID = message.getString("dating_Id")
            val socialisingID = message.getString("socialising_Id")
            val networkingID = message.getString("networking_Id")
            val gender = users.getString("gender")
            val nationality = users.getString("nationality")
            val city = users.getString("city")
            val age = users.getString("age")
            blocked = users.getString("blocked")

            chatAdapter.gender = gender

            if (id == "0") {
                id_image_view.setImageDrawable(getDrawable(R.drawable.icon_logo_profile))
            } else {
                Picasso.get().load(Constants.url + image).into(id_image_view)
            }
            val lookingFor = Constants.getLookingFor(datingID, socialisingID, networkingID)
            id_name_text_view.setText(name + lookingFor)

            var details = ""

            if (age.isEmpty()) {
                if (city.isEmpty()) {
                    if (nationality.isEmpty()) {
                        id_details_text_view.visibility = View.GONE
                    } else {
                        val flags = Constants.getFlags()
                        val flag = flags.first {
                            flag ->
                            flag.id == nationality
                        }
                        details = flag.emoji
                    }
                } else {
                    if (nationality.isEmpty()) {
                        details = city
                    } else {
                        val flags = Constants.getFlags()
                        val flag = flags.first {
                            flag ->
                            flag.id == nationality
                        }
                        details = "${city} ${flag.emoji}"
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
            id_details_text_view.setText(details)

            if (gender =="F") {
                id_image_layout.setBackgroundResource(R.drawable.view_drawable_user_image_pink)
            } else {
                id_image_layout.setBackgroundResource(R.drawable.view_drawable_user_image_blue)
            }
            if (blocked == "1") {
                id_message_layout.visibility = View.GONE
            }

            for (index in 0..(inbox.length() - 1)) {
                val jsonObject = inbox[index] as JSONObject
                val message_Id = jsonObject.getString("message_Id")
                val type = jsonObject.getString("type")
                val text = jsonObject.getString("text")
                val insert_at = jsonObject.getString("insert_at")

                setMessage(type, text, insert_at)

                if (index == inbox.length() - 1) {
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

    fun setMessage(type: String, message: String, date: String) {
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
        array.add(0, ChatMessageClass(type, message, timeString))
    }

    fun saveLastMessage(messageID: String) {
        val array = messageDao.getAll()

        if (array.any {
            jsonObject ->
            jsonObject.userID == id
        }) {
            val jsonObject = messageDao.getItemByUserID(id)
            jsonObject.lastMessageID = messageID
            messageDao.update(jsonObject)
        } else {
            messageDao.insert(LocalMessage(null, id, messageID))
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
        val url = Constants.url + "send_inbox.php"

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
                params["receiver_Id"] = id
                params["text"] = id_message_edit_text.text.toString()
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun sendMaster() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "send_inbox_master.php"

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

            setMessage("send", id_message_edit_text.text.toString(), date)
            saveLastMessage(message_Id)

            if (Constants.setLastMessage != null) {
                Constants.setLastMessage!!(id, id_message_edit_text.text.toString(), date, false)
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
            if (Constants.reloadLocation != null && Constants.reloadMessages != null) {
                Constants.reloadLocation()
                Constants.reloadMessages()
            }
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
