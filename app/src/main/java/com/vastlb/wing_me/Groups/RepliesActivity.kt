
package com.vastlb.wing_me.Groups

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.squareup.picasso.Picasso
import com.vastlb.wing_me.Adapters.RepliesAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.DataClasses.ReplyClass
import com.vastlb.wing_me.R
import com.vastlb.wing_me.User.UserProfileActivity
import kotlinx.android.synthetic.main.activity_replies.*
import org.json.JSONException
import org.json.JSONObject

class RepliesActivity: AppCompatActivity() {

    lateinit var adapter: RepliesAdapter

    val array = ArrayList<ReplyClass>()

    var locationID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_replies)
        setViews()
    }

    fun setViews() {
        locationID = intent.getStringExtra("LocationID")!!

        val jsonObject = Constants.commentClass

        Picasso.get().load(Constants.url + jsonObject.imageURL).into(id_image_view)
        Picasso.get().load(Constants.url + jsonObject.badgeImageURL).into(id_badge_image_view)

        id_name_text_view.setText(jsonObject.name)
        id_badge_text_view.setText(jsonObject.badgeTitle)
        id_comment_text_view.setText(jsonObject.comment)
        id_likes_text_view.setText(jsonObject.likes.toString())

        var details = ""

        id_details_text_view.visibility = View.VISIBLE

        if (jsonObject.age.isEmpty()) {
            if (jsonObject.city.isEmpty()) {
                if (jsonObject.country.isEmpty()) {
                    id_details_text_view.visibility = View.GONE
                } else {
                    val flags = Constants.getFlags()
                    val flag = flags.first {
                        flag ->
                        flag.id == jsonObject.country
                    }
                    details = flag.emoji
                }
            } else {
                if (jsonObject.country.isEmpty()) {
                    details = jsonObject.city
                } else {
                    val flags = Constants.getFlags()
                    val flag = flags.first {
                        flag ->
                        flag.id == jsonObject.country
                    }
                    details = "${jsonObject.city} ${flag.emoji}"
                }
            }
        } else {
            if (jsonObject.city.isEmpty()) {
                if (jsonObject.country.isEmpty()) {
                    details = "Age: ${jsonObject.age}"
                } else {
                    val flags = Constants.getFlags()
                    val flag = flags.first {
                        flag ->
                        flag.id == jsonObject.country
                    }
                    details = "Age: ${jsonObject.age} ${flag.emoji}"
                }
            } else {
                if (jsonObject.country.isEmpty()) {
                    details = "Age: ${jsonObject.age}, ${jsonObject.city}"
                } else {
                    val flags = Constants.getFlags()
                    val flag = flags.first {
                        flag ->
                        flag.id == jsonObject.country
                    }
                    details = "Age: ${jsonObject.age}, ${jsonObject.city} ${flag.emoji}"
                }
            }
        }
        id_details_text_view.setText(details)

        if (jsonObject.gender == "F") {
            id_image_layout.setBackgroundResource(R.drawable.view_drawable_user_image_pink)
        } else {
            id_image_layout.setBackgroundResource(R.drawable.view_drawable_user_image_blue)
        }

        if (jsonObject.isLiked) {
            id_like.setImageDrawable(getDrawable(R.drawable.icon_heart_full))
            id_like.setColorFilter(getColor(R.color.red))
        } else {
            id_like.setImageDrawable(getDrawable(R.drawable.icon_heart))
            id_like.setColorFilter(getColor(R.color.white))
        }

        adapter = RepliesAdapter(this, array, select = {
            index ->

        }, longClick = {
            index ->
            val jsonObject = array[index]

            if (jsonObject.isMyComment) {
                showDeleteSheet(jsonObject.id)
            }
        }, openProfile = {
            index ->
            val jsonObject = array[index]

            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("ID", jsonObject.id)
            startActivity(intent)
        }, like = {
            index, like ->
            val jsonObject = array[index]
            jsonObject.isLiked = like

            if (like) {
                jsonObject.likes += 1
                addLike(jsonObject.id)
            } else {
                jsonObject.likes -= 1
                deleteLike(jsonObject.id)
            }
            adapter.notifyDataSetChanged()
        }, add = {
            index, add ->
            val jsonObject = array[index]

            id_progress_bar.visibility = View.VISIBLE

            if (add) {
                addUserGroup(jsonObject.userID)
            } else {
                removeUserGroup(jsonObject.userID)
            }
        })

        adapter.isMyComment = Constants.commentClass.isMyComment

        val linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        id_recycler_view.layoutManager = linearLayoutManager
        id_recycler_view.adapter = adapter

        id_send.setOnClickListener {
            if (id_comment_edit_text.text.toString().isEmpty()) {
                return@setOnClickListener
            }
            id_send.visibility = View.GONE
            id_send_progress_bar.visibility = View.VISIBLE
            addComment()
        }

        request()
    }

    fun showDeleteSheet(id: String) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.select_option))
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val dialogItems = arrayOf("Delete Comment")

        alertDialog.setItems(dialogItems) {
            dialog, which ->

            when (which) {
                0 -> {
                    id_progress_bar.visibility = View.VISIBLE
                    deleteComment(id)
                }
            }
        }
        alertDialog.show()
    }

    fun reload() {
        id_progress_bar.visibility = View.VISIBLE
        request()
    }

    fun request() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_replies.php"

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

                if (locationID.contains("Event")) {
                    params["event_Id"] = locationID.replace("Event_", "")
                } else {
                    params["location_Id"] = locationID
                }
                params["comment_Id"] = Constants.commentClass.id
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

            array.clear()

            for (index in 0..(message.length() - 1)) {
                val jsonObject = message[index] as JSONObject
                val image = jsonObject.getString("image")
                val badges_image = jsonObject.getString("badges_image")
                val Id = jsonObject.getString("Id")
                val user_Id = jsonObject.getString("user_Id")
                val name = jsonObject.getString("name")
                val badges_title = jsonObject.getString("badges_title")
                val age = jsonObject.getString("age")
                val gender = jsonObject.getString("gender")
                val nationality = jsonObject.getString("nationality")
                val city = jsonObject.getString("city")
                val comment = jsonObject.getString("comment")
                val number_of_like = jsonObject.getInt("number_of_like")
                val is_my_comment = jsonObject.getBoolean("is_my_comment")
                val is_liked = jsonObject.getBoolean("is_liked")
                val in_group = jsonObject.getBoolean("in_group")

                array.add(ReplyClass(image, Id, user_Id, name, badges_image, badges_title, age, gender, nationality, city, comment, number_of_like, is_my_comment, is_liked, in_group))
            }
            adapter.notifyDataSetChanged()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_progress_bar.visibility = View.GONE
    }

    fun addLike(id: String) {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "add_like.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    addLikeSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                addLikeError()
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

    fun addLikeError() {

    }

    fun addLikeSuccess(response: String) {

    }

    fun deleteLike(id: String) {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "delete_comment_likes.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                    response ->

                try {
                    deleteLikeSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                deleteLikeError()
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

    fun deleteLikeError() {

    }

    fun deleteLikeSuccess(response: String) {

    }

    fun addComment() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "add_reply.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    addCommentSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                addCommentError()
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
                params["comment_Id"] = Constants.commentClass.id
                params["comment"] = id_comment_edit_text.text.toString()
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun addCommentError() {
        println("Error2")
        addComment()
    }

    fun addCommentSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            id_comment_edit_text.setText("")

            reload()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_send.visibility = View.VISIBLE
        id_send_progress_bar.visibility = View.GONE
    }

    fun deleteComment(id: String) {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "delete_comment.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                    response ->

                try {
                    deleteCommentSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                deleteCommentError(id)
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

    fun deleteCommentError(id: String) {
        println("Error2")
        deleteComment(id)
    }

    fun deleteCommentSuccess(response: String) {
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

    fun addUserGroup(id: String) {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "add_user_group.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                    response ->

                try {
                    addUserGroupSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                addUserGroupError()
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
                params["comment_Id"] = Constants.commentClass.id
                params["user_Id"] = id
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun addUserGroupError() {

    }

    fun addUserGroupSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            reload()
            Constants.reloadGroups()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_progress_bar.visibility = View.GONE
    }

    fun removeUserGroup(id: String) {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "remove_user_group.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    removeUserGroupSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                removeUserGroupError()
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
                params["comment_Id"] = Constants.commentClass.id
                params["user_Id"] = id
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun removeUserGroupError() {

    }

    fun removeUserGroupSuccess(response: String) {
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
