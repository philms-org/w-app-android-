
package com.vastlb.wing_me.Main

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.squareup.picasso.Picasso
import com.vastlb.wing_me.Adapters.CommentsAdapter
import com.vastlb.wing_me.Adapters.NewLocationBannerAdapter
import com.vastlb.wing_me.Adapters.UsersAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.DataClasses.CommentClass
import com.vastlb.wing_me.DataClasses.LocationBannerClass
import com.vastlb.wing_me.DataClasses.LocationClass
import com.vastlb.wing_me.DataClasses.UserClass
import com.vastlb.wing_me.Groups.RepliesActivity
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Supabase.SupabaseData
import com.vastlb.wing_me.User.ChatActivity
import com.vastlb.wing_me.User.SocialSettingsActivity
import com.vastlb.wing_me.User.UserProfileActivity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class NewMyLocationFragment: Fragment() {

    lateinit var noLocationLayout: RelativeLayout
    lateinit var progressBar: ProgressBar

    lateinit var bannerTextLayout: RelativeLayout
    lateinit var bannerTextView: TextView

    lateinit var imageView: ImageView
    lateinit var nameTextView: TextView
    lateinit var cityTextView: TextView
    lateinit var detailsTextView: TextView

    lateinit var commentEditText: EditText
    lateinit var send: ImageView
    lateinit var sendProgressBar: ProgressBar

    lateinit var bannerAdapter: NewLocationBannerAdapter
    lateinit var commentsAdapter: CommentsAdapter
    lateinit var usersAdapter: UsersAdapter

    val bannersArray = ArrayList<LocationBannerClass>()
    val commentsArray = ArrayList<CommentClass>()
    val usersArray = ArrayList<UserClass>()

    var timer: CountDownTimer? = null

    var inLocation = false
    var locationID = ""

    var bannerIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_my_location_new, container, false)
        setViews(view)
        return view
    }

    companion object {
        fun newInstance(): NewMyLocationFragment = NewMyLocationFragment()
    }

    fun setViews(view: View) {
        noLocationLayout = view.findViewById(R.id.id_no_location_layout)
        val menu: ImageView = view.findViewById(R.id.id_menu)

        val share: RelativeLayout = view.findViewById(R.id.id_share)
        progressBar = view.findViewById(R.id.id_progress_bar)

        bannerTextLayout = view.findViewById(R.id.id_banner_text_layout)
        bannerTextView = view.findViewById(R.id.id_banner_text_view)

        imageView = view.findViewById(R.id.id_image_view)
        nameTextView = view.findViewById(R.id.id_title_text_view)
        cityTextView = view.findViewById(R.id.id_city_text_view)
        detailsTextView = view.findViewById(R.id.id_details_text_view)

        val bannerRecyclerView: RecyclerView = view.findViewById(R.id.id_banner_recycler_view)
        val commentsRecyclerView: RecyclerView = view.findViewById(R.id.id_comments_recycler_view)
        val usersRecyclerView: RecyclerView = view.findViewById(R.id.id_users_recycler_view)

        commentEditText = view.findViewById(R.id.id_comment_edit_text)
        send = view.findViewById(R.id.id_send)
        sendProgressBar = view.findViewById(R.id.id_send_progress_bar)

        bannerTextLayout.visibility = View.GONE

        Constants.reloadLocation = {
            reload()
        }

        Constants.wingMe = {
            jsonObject ->
            wingMe(jsonObject)
        }

        bannerAdapter = NewLocationBannerAdapter(requireContext(), bannersArray) {
            index ->
            val jsonObject = bannersArray[index]

            if (jsonObject.url.isEmpty()) {

            } else {
                openURL(jsonObject.url)
            }
        }

        val snapHelper = PagerSnapHelper()
        val bannerLayoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        bannerRecyclerView.layoutManager = bannerLayoutManager
        bannerRecyclerView.adapter = bannerAdapter
        snapHelper.attachToRecyclerView(bannerRecyclerView)

        bannerRecyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                bannerIndex = bannerLayoutManager.findFirstVisibleItemPosition()
                setUI(bannerIndex)
            }
        })

        commentsAdapter = CommentsAdapter(requireContext(), commentsArray, select = {
            index ->
            val jsonObject = commentsArray[index]
            Constants.commentClass = jsonObject

            val intent = Intent(context, RepliesActivity::class.java)
            intent.putExtra("LocationID", locationID)
            startActivity(intent)
        }, longClick = {
            index ->
            val jsonObject = commentsArray[index]

            if (jsonObject.isMyComment) {
                showDeleteSheet(jsonObject.id)
            }
        }, openProfile = {
            index ->
            val jsonObject = commentsArray[index]

            val intent = Intent(context, UserProfileActivity::class.java)
            intent.putExtra("ID", jsonObject.id)
            startActivity(intent)
        }, like = {
            index, like ->
            val jsonObject = commentsArray[index]
            jsonObject.isLiked = like

            if (like) {
                jsonObject.likes += 1
                addLike(jsonObject.id)
            } else {
                jsonObject.likes -= 1
                deleteLike(jsonObject.id)
            }
            commentsAdapter.notifyDataSetChanged()
        })

        val commentsLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        commentsRecyclerView.layoutManager = commentsLayoutManager
        commentsRecyclerView.adapter = commentsAdapter

        usersAdapter = UsersAdapter(requireContext(), usersArray) {
            index ->
            val jsonObject = usersArray[index]

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
        usersRecyclerView.layoutManager = linearLayoutManager
        usersRecyclerView.adapter = usersAdapter

        Constants.hideLocation = {
            hideLocation()
        }

        menu.setOnClickListener {
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

        send.setOnClickListener {
            if (commentEditText.text.toString().isEmpty()) {
                return@setOnClickListener
            }
            send.visibility = View.GONE
            sendProgressBar.visibility = View.VISIBLE
            addComment()
        }

        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val locationID = preferences.getString("LocationID", "")!!
        val LocationName = preferences.getString("LocationName", "")!!

        if (locationID.contains("Event")) {
            val jsonObject = LocationClass("", locationID, LocationName, "", 0.0, 0.0, 0.0, 0)
            wingMe(jsonObject)
        }
    }

    fun wingMe(jsonObject: LocationClass) {
        inLocation = true
        locationID = jsonObject.id

        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("LocationID", jsonObject.id)
        editor.putString("LocationName", jsonObject.name)
        editor.apply()

        noLocationLayout.visibility = View.GONE
        setTimer()
        reload()

        // location_checkins insert (core Supabase check-in). Events (locationID
        // prefixed "Event_") don't map to a real `locations` row, so they're skipped —
        // the event flow itself is left on the legacy backend (see getEvent()/out of
        // scope note below).
        if (!jsonObject.id.contains("Event") && context != null) {
            SupabaseData.checkIn(requireContext(), jsonObject.id, onSuccess = {}, onError = { })
        }
    }

    fun setUI(index: Int) {
        val jsonObject = bannersArray[index]

        if (jsonObject.blurred) {
            bannerTextLayout.visibility = View.GONE
        } else {
            if (jsonObject.title.isEmpty()) {
                bannerTextLayout.visibility = View.GONE
            } else {
                bannerTextLayout.visibility = View.VISIBLE
                bannerTextView.setText(jsonObject.title)
            }
        }
    }

    fun openURL(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        try {
            startActivity(intent)
        } catch (e: Exception) {

        }
    }

    fun showDeleteSheet(id: String) {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle(getString(R.string.select_option))
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val dialogItems = arrayOf("Delete Comment")

        alertDialog.setItems(dialogItems) {
            dialog, which ->

            when (which) {
                0 -> {
                    progressBar.visibility = View.VISIBLE
                    deleteComment(id)
                }
            }
        }
        alertDialog.show()
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
        progressBar.visibility = View.VISIBLE

        if (locationID.contains("Event")) {
            getEvent()
        } else {
            getLocation()
            getUsers()
        }
    }

    // NOTE: banners and comments are explicitly out of scope for this Supabase pass
    // (see task instructions — comments/likes/replies untouched) — this only loads the
    // core venue details (name/city/description/image) from the `locations` table.
    // bannersArray/commentsArray are simply left empty rather than wired to the dead
    // legacy endpoints.
    fun getLocation() {
        val context = context ?: return

        SupabaseData.fetchVenue(context, locationID, onSuccess = { venue ->
            if (venue != null) {
                getLocationSuccess(venue)
            }
            progressBar.visibility = View.GONE
        }, onError = { message ->
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
            progressBar.visibility = View.GONE
        })
    }

    fun getLocationSuccess(venue: JSONObject) {
        val image = venue.optString("banner_image", "")
        val name = venue.optString("name", "")
        val city = venue.optString("city", "")
        val description = venue.optString("description", "")

        bannersArray.clear()
        bannerAdapter.notifyDataSetChanged()
        bannerTextLayout.visibility = View.GONE

        if (image.isNotEmpty()) {
            Picasso.get().load(image).into(imageView)
        }

        nameTextView.setText(name)
        cityTextView.setText(city)
        detailsTextView.setText(description)

        commentsArray.clear()
        commentsAdapter.notifyDataSetChanged()
    }

    // location_checkins joined with profiles (`?select=*,profiles(*)&location_id=eq.<id>
    // &checked_out_at=is.null`) — who's currently checked in, per the shared schema.
    fun getUsers() {
        val context = context ?: return

        SupabaseData.fetchPresence(context, locationID, onSuccess = { presence ->
            usersSuccess(presence)
        }, onError = { message ->
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
            progressBar.visibility = View.GONE
        })
    }

    fun usersSuccess(presence: JSONArray) {
        usersArray.clear()

        for (index in 0 until presence.length()) {
            val row = presence.getJSONObject(index)
            val profile = row.optJSONObject("profiles") ?: continue

            val id = row.optString("user_id", "")
            val image = profile.optString("avatar_url", "")
            val name = profile.optString("display_name", "")
            val gender = profile.optString("gender", "")
            val city = profile.optString("city", "")
            val nationality = profile.optString("nationality", "")

            var details = city

            if (nationality.isNotEmpty()) {
                val flags = Constants.getFlags()
                val flag = flags.firstOrNull { flagClass -> flagClass.id == nationality }
                if (flag != null) {
                    details = if (city.isEmpty()) flag.emoji else "$city ${flag.emoji}"
                }
            }

            usersArray.add(UserClass(image, id, name, details, gender, false))
        }
        usersAdapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
    }

    fun getEvent() {
        val context = context ?: return

        val preferences = context.getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_event.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    getEventSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                getEventError()
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
                params["event_Id"] = locationID.replace("Event_", "")
                return params
            }
        }
        Singleton.getInstance(requireContext()).addToRequestQueue(request)
    }

    fun getEventError() {
        println("Error2")
        getEvent()
    }

    fun getEventSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val message = json.getJSONObject("message")
            val event_image = message.getString("event_image")
            val comment = message.getJSONArray("comments")
            val title = message.getString("title")
            val description = message.getString("description")

            bannersArray.clear()
            bannersArray.add(LocationBannerClass(event_image, "", "", false))
            bannerAdapter.notifyDataSetChanged()

            imageView.setImageDrawable(requireContext().getDrawable(R.drawable.icon_logo_profile))

            nameTextView.setText(title)
            cityTextView.setText("Welcome to our Event!")
            detailsTextView.setText(description)

            commentsArray.clear()

            for (index in 0..(comment.length() - 1)) {
                val jsonObject = comment[index] as JSONObject
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

                commentsArray.add(CommentClass(image, Id, user_Id, name, badges_image, badges_title, age, gender, nationality, city, comment, number_of_like, is_my_comment, is_liked))
            }
            commentsAdapter.notifyDataSetChanged()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
        }
        progressBar.visibility = View.GONE
    }

    fun wingOff() {
        val context = context ?: return

        // location_checkins check-out (core Supabase loop). Events don't have a real
        // `locations` row to check out of, so they're skipped here — see wingMe().
        if (locationID.contains("Event")) {
            return
        }
        SupabaseData.checkOut(context, locationID, onSuccess = {}, onError = { })
    }

    fun addLike(id: String) {
        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "add_like.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    addLikeSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
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
        Singleton.getInstance(requireContext()).addToRequestQueue(request)
    }

    fun addLikeError() {

    }

    fun addLikeSuccess(response: String) {

    }

    fun deleteLike(id: String) {
        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "delete_comment_likes.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    deleteLikeSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
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
        Singleton.getInstance(requireContext()).addToRequestQueue(request)
    }

    fun deleteLikeError() {

    }

    fun deleteLikeSuccess(response: String) {

    }

    fun addComment() {
        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "add_comment.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    addCommentSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
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

                if (locationID.contains("Event")) {
                    params["event_Id"] = locationID.replace("Event_", "")
                } else {
                    params["location_Id"] = locationID
                }
                params["comment"] = commentEditText.text.toString()
                return params
            }
        }
        Singleton.getInstance(requireContext()).addToRequestQueue(request)
    }

    fun addCommentError() {
        println("Error2")
        addComment()
    }

    fun addCommentSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            commentEditText.setText("")

            reload()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
        }
        send.visibility = View.VISIBLE
        sendProgressBar.visibility = View.GONE
    }

    fun deleteComment(id: String) {
        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "delete_comment.php"

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener {
                response ->

                try {
                    deleteCommentSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
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
        Singleton.getInstance(requireContext()).addToRequestQueue(request)
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
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
        }
        progressBar.visibility = View.GONE
    }
}
