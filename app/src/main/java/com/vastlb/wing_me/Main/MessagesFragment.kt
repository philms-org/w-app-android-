
package com.vastlb.wing_me.Main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.vastlb.wing_me.Adapters.GroupsAdapter
import com.vastlb.wing_me.Adapters.MessagesAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.CoreData.MessageDao
import com.vastlb.wing_me.CoreData.MessageDatabase
import com.vastlb.wing_me.DataClasses.GroupClass
import com.vastlb.wing_me.DataClasses.MessageClass
import com.vastlb.wing_me.Groups.GroupChatActivity
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Supabase.SupabaseData
import com.vastlb.wing_me.User.ChatActivity
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MessagesFragment: Fragment() {

    lateinit var showBadge: () -> Unit

    lateinit var messageDao: MessageDao
    lateinit var groupDao: MessageDao

    lateinit var messagesAdapter: MessagesAdapter
    lateinit var groupsAdapter: GroupsAdapter

    lateinit var messages: TextView
    lateinit var messagesSelectedView: CardView
    lateinit var groups: TextView
    lateinit var groupsSelectedView: CardView

    lateinit var messagesRefreshLayout: SwipeRefreshLayout
    lateinit var groupsRefreshLayout: SwipeRefreshLayout
    lateinit var noMessagesLayout: RelativeLayout
    lateinit var progressBar: ProgressBar

    val messagesArray = ArrayList<MessageClass>()
    val groupsArray = ArrayList<GroupClass>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_messages, container, false)
        setViews(view)
        return view
    }

    companion object {
        fun newInstance(): MessagesFragment = MessagesFragment()
    }

    fun setViews(view: View) {
        messages = view.findViewById(R.id.id_messages)
        messagesSelectedView = view.findViewById(R.id.id_messages_selected_view)
        groups = view.findViewById(R.id.id_groups)
        groupsSelectedView = view.findViewById(R.id.id_groups_selected_view)

        messagesRefreshLayout = view.findViewById(R.id.id_messages_refresh_layout)
        groupsRefreshLayout = view.findViewById(R.id.id_groups_refresh_layout)
        val messagesRecyclerView: RecyclerView = view.findViewById(R.id.id_messages_recycler_view)
        val groupsRecyclerView: RecyclerView = view.findViewById(R.id.id_groups_recycler_view)
        noMessagesLayout = view.findViewById(R.id.id_no_messages_layout)
        val share: RelativeLayout = view.findViewById(R.id.id_share)
        progressBar = view.findViewById(R.id.id_progress_bar)

        Constants.setLastMessage = {
            userID, message, date, badge ->

            requireActivity().runOnUiThread {
                setInbox(userID, message, date, badge)
            }
        }

        Constants.setLastGroup = {
            groupID, message, date, badge ->

            requireActivity().runOnUiThread {
                setGroup(groupID, message, date, badge)
            }
        }

        messageDao = Room.databaseBuilder(requireContext(), MessageDatabase::class.java, "Messages").allowMainThreadQueries().build().dataDao()
        groupDao = Room.databaseBuilder(requireContext(), MessageDatabase::class.java, "Groups").allowMainThreadQueries().build().dataDao()

        Constants.reloadMessages = {
            reloadMessages()
        }

        Constants.reloadGroups = {
            refreshGroups()
        }

        messages.setOnClickListener {
            selectTab(0)
        }

        groups.setOnClickListener {
            selectTab(1)
        }

        messagesRefreshLayout.setOnRefreshListener {
            refreshMessages()
        }

        groupsRefreshLayout.setOnRefreshListener {
            reloadGroups()
        }

        messagesAdapter = MessagesAdapter(requireContext(), messagesArray, {
            index ->
            val jsonObject = messagesArray[index]
            jsonObject.isSelected = false
            messagesAdapter.notifyDataSetChanged()

            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("ID", jsonObject.id)
            startActivity(intent)
        }, {
            index ->
            val jsonObject = messagesArray[index]
            val alertDialog = AlertDialog.Builder(requireContext())
            alertDialog.setTitle(getString(R.string.select_option))
            alertDialog.setNegativeButton(getString(R.string.cancel), null)
            val dialogItems = arrayOf(getString(R.string.delete))

            alertDialog.setItems(dialogItems) { dialog, which ->
                showDeleteAlert(jsonObject.id)
            }
            alertDialog.show()
        })

        val messagesLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        messagesRecyclerView.layoutManager = messagesLayoutManager
        messagesRecyclerView.adapter = messagesAdapter

        groupsAdapter = GroupsAdapter(requireContext(), groupsArray) {
            index ->
            val jsonObject = groupsArray[index]
            jsonObject.isSelected = false
            groupsAdapter.notifyDataSetChanged()

            val intent = Intent(context, GroupChatActivity::class.java)
            intent.putExtra("ID", jsonObject.id)
            startActivity(intent)
        }

        val groupsLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        groupsRecyclerView.layoutManager = groupsLayoutManager
        groupsRecyclerView.adapter = groupsAdapter

        share.setOnClickListener {
            val url = Constants.appURL
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
            startActivity(Intent.createChooser(shareIntent, "Share link using"))
        }

        selectTab(0)
        getMessages()
        getGroups()
    }

    fun selectTab(index: Int) {
        if (index == 0) {
            messagesSelectedView.visibility = View.VISIBLE
            groupsSelectedView.visibility = View.GONE
            messages.setTextColor(requireContext().getColor(R.color.blue))
            groups.setTextColor(requireContext().getColor(R.color.white))
            messagesRefreshLayout.visibility = View.VISIBLE
            groupsRefreshLayout.visibility = View.GONE
        } else {
            messagesSelectedView.visibility = View.GONE
            groupsSelectedView.visibility = View.VISIBLE
            messages.setTextColor(requireContext().getColor(R.color.white))
            groups.setTextColor(requireContext().getColor(R.color.blue))
            messagesRefreshLayout.visibility = View.GONE
            groupsRefreshLayout.visibility = View.VISIBLE
        }
    }

    fun showDeleteAlert(id: String) {
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setTitle(getString(R.string.alert_delete))
        alertDialog.setPositiveButton(getString(R.string.delete)) {
            _, _ ->
            progressBar.visibility = View.VISIBLE
            delete(id)
        }
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val alert = alertDialog.create()
        alert.show()
    }

    fun setInbox(userID: String, message: String, date: String, badge: Boolean) {
        if (messagesArray.any {
            jsonObject ->
            jsonObject.id == userID
        }) {
            val index = messagesArray.indexOfFirst {
                jsonObject ->
                jsonObject.id == userID
            }
            val jsonObject = messagesArray.removeAt(index)
            jsonObject.message = message
            jsonObject.date = getTime(date)
            jsonObject.isSelected = badge
            messagesArray.add(0, jsonObject)
            messagesAdapter.notifyDataSetChanged()

            if (badge) {
                showBadge()
            }
        } else {
            reloadMessages()
        }
    }

    fun setGroup(groupID: String, message: String, date: String, badge: Boolean) {
        if (groupsArray.any {
            jsonObject ->
            jsonObject.id == groupID
        }) {
            val index = groupsArray.indexOfFirst {
                jsonObject ->
                jsonObject.id == groupID
            }
            val jsonObject = groupsArray.removeAt(index)
            jsonObject.message = message
            jsonObject.date = getTime(date)
            jsonObject.isSelected = badge
            groupsArray.add(0, jsonObject)
            groupsAdapter.notifyDataSetChanged()

            if (badge) {
                showBadge()
            }
        } else {
            refreshGroups()
        }
    }

    fun refreshMessages() {
        messageDao = Room.databaseBuilder(requireContext(), MessageDatabase::class.java, "Messages").allowMainThreadQueries().build().dataDao()

        messagesArray.clear()
        messagesAdapter.notifyDataSetChanged()

        Handler().postDelayed({
            getMessages()
        }, 1000)
    }

    fun refreshGroups() {
        groupDao = Room.databaseBuilder(requireContext(), MessageDatabase::class.java, "Groups").allowMainThreadQueries().build().dataDao()

        groupsArray.clear()
        groupsAdapter.notifyDataSetChanged()

        Handler().postDelayed({
            getGroups()
        }, 1000)
    }

    fun reloadMessages() {
        messageDao = Room.databaseBuilder(requireContext(), MessageDatabase::class.java, "Messages").allowMainThreadQueries().build().dataDao()

        messagesArray.clear()
        messagesAdapter.notifyDataSetChanged()
        progressBar.visibility = View.VISIBLE
        getMessages()
    }

    fun reloadGroups() {
        groupDao = Room.databaseBuilder(requireContext(), MessageDatabase::class.java, "Groups").allowMainThreadQueries().build().dataDao()

        groupsArray.clear()
        groupsAdapter.notifyDataSetChanged()
        progressBar.visibility = View.VISIBLE
        getGroups()
    }

    fun getMessages() {
        SupabaseData.fetchConversations(requireContext(), onSuccess = { conversations ->
            messagesSuccess(conversations)
        }, onError = { message ->
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
            messagesRefreshLayout.isRefreshing = false
            progressBar.visibility = View.GONE
        })
    }

    // Conversation summaries from SupabaseData.fetchConversations, each shaped:
    // { id, is_group, name, last_message, last_message_at, my_status,
    //   other_user_id, other_display_name, other_avatar_url }. Group conversations are
    // out of scope for this pass (see Groups.* — untouched) so only 1:1 conversations
    // are rendered here.
    fun messagesSuccess(conversations: org.json.JSONArray) {
        messagesArray.clear()

        for (index in 0 until conversations.length()) {
            val jsonObject = conversations.getJSONObject(index)
            val isGroup = jsonObject.optBoolean("is_group", false)

            if (isGroup) {
                continue
            }
            val id = jsonObject.optString("other_user_id", jsonObject.getString("id"))
            val name = jsonObject.optString("other_display_name", "")
            val image = jsonObject.optString("other_avatar_url", "")
            val text = jsonObject.optString("last_message", "")
            val messageAt = jsonObject.optString("last_message_at", "")

            var time = ""
            if (messageAt.isNotEmpty()) {
                try {
                    time = getTime(toLegacyDateFormat(messageAt))
                } catch (e: Exception) {
                    time = ""
                }
            }
            val badge = checkMessageBadge(id, jsonObject.getString("id"))

            messagesArray.add(MessageClass(image, id, name, jsonObject.getString("id"), text, time, "", false, badge, false))
        }
        if (groupsArray.isEmpty() && messagesArray.isEmpty()) {
            noMessagesLayout.visibility = View.VISIBLE
        } else {
            noMessagesLayout.visibility = View.GONE
        }
        messagesRefreshLayout.isRefreshing = false
        messagesAdapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
    }

    fun getGroups() {
        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_groups_inboxs.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    groupsSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                groupsConnectionError()
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
        Singleton.getInstance(requireContext()).addToRequestQueue(request)
    }

    fun groupsConnectionError() {
        println("Error2")
        getGroups()
    }

    fun groupsSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            groupsArray.clear()

            val message = json.getJSONArray("message")

            for (index in 0..(message.length() - 1)) {
                val jsonObject = message[index] as JSONObject
                val image = jsonObject.getString("group_image")
                val comment_Id = jsonObject.getString("comment_Id")
                val name = jsonObject.getString("title")
                val text = jsonObject.getString("text")
                val message_at = jsonObject.getString("insert_at")
                val messageID = jsonObject.getString("message_Id")
                val is_mute = jsonObject.getBoolean("is_mute")

                var time = ""

                if (!message_at.isEmpty()) {
                    time = getTime(message_at)
                }
                val badge = checkGroupBadge(comment_Id, messageID)
                groupsArray.add(GroupClass(image, comment_Id, name, messageID, text, time, badge, is_mute))
            }
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
        }
        if (groupsArray.isEmpty() && messagesArray.isEmpty()) {
            noMessagesLayout.visibility = View.VISIBLE
        } else {
            noMessagesLayout.visibility = View.GONE
        }
        groupsRefreshLayout.isRefreshing = false
        groupsAdapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
    }

    // Supabase timestamps come back as ISO 8601 ("2026-07-20T00:38:08.466+00:00");
    // getTime() below expects the legacy PHP backend's "yyyy-MM-dd HH:mm:ss" shape.
    fun toLegacyDateFormat(iso: String): String {
        val datePart = iso.substringBefore("T")
        val timePart = iso.substringAfter("T").substringBefore(".").substringBefore("+")
        return "$datePart $timePart"
    }

    fun getTime(date: String): String {
        val dateParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

        val date = dateParser.parse(date)!!
        val today = Calendar.getInstance().time

        val difference = getStartOfDay(today).time - getStartOfDay(date).time
        val days = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS).toInt()

        if (days == 0) {
            val dateFormatter = SimpleDateFormat("hh:mm aa", Locale.getDefault())
            val dateString = dateFormatter.format(date)
            return dateString
        } else if (days == 1) {
            return "Yesterday"
        } else {
            val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateString = dateFormatter.format(date)
            return dateString
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

    fun checkMessageBadge(userID: String, messageID: String): Boolean {
        if (messageID.isEmpty()) {
            return false
        }
        val array = messageDao.getAll()

        if (array.any {
            jsonObject ->
            jsonObject.userID == userID && jsonObject.lastMessageID == messageID
        }) {
            return false
        }
        showBadge()
        return true
    }

    fun checkGroupBadge(groupID: String, messageID: String): Boolean {
        if (messageID.isEmpty()) {
            return false
        }
        val array = groupDao.getAll()

        if (array.any {
            jsonObject ->
            jsonObject.userID == groupID && jsonObject.lastMessageID == messageID
        }) {
            return false
        }
        showBadge()
        return true
    }

    fun delete(id: String) {
        val preferences = requireContext().getSharedPreferences("Preferences", AppCompatActivity.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "delete_inbox.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    deleteSuccess(response, id)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                deleteError(id)
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
        Singleton.getInstance(requireContext()).addToRequestQueue(request)
    }

    fun deleteError(id: String) {
        println("Error2")
        delete(id)
    }

    fun deleteSuccess(response: String, id: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val index = messagesArray.indexOfFirst {
                jsonObject ->
                jsonObject.id == id
            }
            messagesArray.removeAt(index)
            messagesAdapter.notifyItemRemoved(index)
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
        }
        progressBar.visibility = View.GONE
    }
}
