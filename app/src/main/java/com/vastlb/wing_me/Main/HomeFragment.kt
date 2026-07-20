
package com.vastlb.wing_me.Main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.vastlb.wing_me.Adapters.EventsAdapter
import com.vastlb.wing_me.Adapters.HomeBannerAdapter
import com.vastlb.wing_me.Adapters.HomeLocationsAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.DataClasses.BannerClass
import com.vastlb.wing_me.DataClasses.EventClass
import com.vastlb.wing_me.DataClasses.HomeLocationClass
import com.vastlb.wing_me.DataClasses.LocationClass
import com.vastlb.wing_me.R
import com.vastlb.wing_me.User.SingleLocationActivity
import org.json.JSONException
import org.json.JSONObject

class HomeFragment: Fragment() {

    lateinit var bannerAdapter: HomeBannerAdapter
    lateinit var eventsAdapter: EventsAdapter
    lateinit var newAddedAdapter: HomeLocationsAdapter
    lateinit var mostVisitedAdapter: HomeLocationsAdapter
    lateinit var lastVisitedAdapter: HomeLocationsAdapter

    lateinit var scrollView: ScrollView
    lateinit var newAddedLayout: RelativeLayout
    lateinit var bannerLayout: RelativeLayout
    lateinit var eventsLayout: RelativeLayout
    lateinit var mostVisitedLayout: RelativeLayout
    lateinit var lastVisitedLayout: RelativeLayout
    lateinit var progressBar: ProgressBar

    val bannerArray = ArrayList<BannerClass>()
    val eventsArray = ArrayList<EventClass>()
    val newAddedArray = ArrayList<HomeLocationClass>()
    val mostVisitedArray = ArrayList<HomeLocationClass>()
    val lastVisitedArray = ArrayList<HomeLocationClass>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        setViews(view)
        return view
    }

    companion object {
        fun newInstance(): HomeFragment = HomeFragment()
    }

    fun setViews(view: View) {
        scrollView = view.findViewById(R.id.id_scroll_view)
        bannerLayout = view.findViewById(R.id.id_banner_layout)
        val bannerRecyclerView: RecyclerView = view.findViewById(R.id.id_banner_recycler_view)
        eventsLayout = view.findViewById(R.id.id_events_layout)
        val eventsRecyclerView: RecyclerView = view.findViewById(R.id.id_events_recycler_view)
        newAddedLayout = view.findViewById(R.id.id_new_added_layout)
        val newAddedRecyclerView: RecyclerView = view.findViewById(R.id.id_new_added_recycler_view)
        mostVisitedLayout = view.findViewById(R.id.id_most_visited_layout)
        val mostVisitedRecyclerView: RecyclerView = view.findViewById(R.id.id_most_visited_recycler_view)
        lastVisitedLayout = view.findViewById(R.id.id_last_visited_layout)
        val lastVisitedRecyclerView: RecyclerView = view.findViewById(R.id.id_last_visited_recycler_view)
        progressBar = view.findViewById(R.id.id_progress_bar)

        scrollView.visibility = View.GONE

        bannerAdapter = HomeBannerAdapter(bannerArray) {
            index ->
            val jsonObject = bannerArray[index]

            if (!jsonObject.locationID.isEmpty() && jsonObject.locationID != "0") {
                val intent = Intent(context, SingleLocationActivity::class.java)
                intent.putExtra("ID", jsonObject.locationID)
                startActivity(intent)
            } else if (!jsonObject.url.isEmpty()) {
                openURL(jsonObject.url)
            }
        }

        val bannerLayoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        val snapHelper = PagerSnapHelper()
        bannerRecyclerView.layoutManager = bannerLayoutManager
        bannerRecyclerView.adapter = bannerAdapter
        snapHelper.attachToRecyclerView(bannerRecyclerView)

        eventsAdapter = EventsAdapter(eventsArray) {
            index ->
            val jsonObject = eventsArray[index]
            showWingMeSheet(jsonObject)
        }
        val eventsLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        eventsRecyclerView.layoutManager = eventsLayoutManager
        eventsRecyclerView.adapter = eventsAdapter

        newAddedAdapter = HomeLocationsAdapter(newAddedArray) {
            index ->
            val jsonObject = newAddedArray[index]
            val intent = Intent(context, SingleLocationActivity::class.java)
            intent.putExtra("ID", jsonObject.id)
            startActivity(intent)
        }
        val newAddedLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        newAddedRecyclerView.layoutManager = newAddedLayoutManager
        newAddedRecyclerView.adapter = newAddedAdapter

        mostVisitedAdapter = HomeLocationsAdapter(mostVisitedArray) {
            index ->
            val jsonObject = mostVisitedArray[index]
            val intent = Intent(context, SingleLocationActivity::class.java)
            intent.putExtra("ID", jsonObject.id)
            startActivity(intent)
        }
        val mostVisitedLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        mostVisitedRecyclerView.layoutManager = mostVisitedLayoutManager
        mostVisitedRecyclerView.adapter = mostVisitedAdapter

        lastVisitedAdapter = HomeLocationsAdapter(lastVisitedArray) {
            index ->
            val jsonObject = lastVisitedArray[index]
            val intent = Intent(context, SingleLocationActivity::class.java)
            intent.putExtra("ID", jsonObject.id)
            startActivity(intent)
        }
        val lastVisitedLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        lastVisitedRecyclerView.layoutManager = lastVisitedLayoutManager
        lastVisitedRecyclerView.adapter = lastVisitedAdapter

        request()
    }

    fun showWingMeSheet(jsonObject: EventClass) {
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("May I wing you into ${jsonObject.title}?")
        alertDialog.setPositiveButton(getString(R.string.wing_me)) {
            _, _ ->
            val jsonObject = LocationClass("", "Event_${jsonObject.id}", jsonObject.title, "", 0.0, 0.0, 0.0, 0)
            Constants.wingMe(jsonObject)
            Constants.selectMyLocation()
        }
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val alert = alertDialog.create()
        alert.show()
    }

    fun openURL(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        try {
            startActivity(intent)
        } catch (e: Exception) {

        }
    }

    fun request() {
        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_home.php"

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
            val message = json.getJSONObject("message")
            val banner = message.getJSONArray("banner")
            val events = message.getJSONArray("events")
            val new_locations = message.getJSONArray("new_locations")
            val most_visited = message.getJSONArray("most_visited")
            val last_visited = message.getJSONArray("last_visited")

            for (index in 0..(banner.length() - 1)) {
                val jsonObject = banner[index] as JSONObject
                val image = jsonObject.getString("image")
                val location_Id = jsonObject.getString("location_Id")
                val link = jsonObject.getString("link")

                bannerArray.add(BannerClass(image, location_Id, link))
            }
            bannerAdapter.notifyDataSetChanged()

            if (bannerArray.isEmpty()) {
                bannerLayout.visibility = View.GONE
            } else {
                bannerLayout.visibility = View.VISIBLE
            }

            for (index in 0..(events.length() - 1)) {
                val jsonObject = events[index] as JSONObject
                val image = jsonObject.getString("image")
                val Id = jsonObject.getString("Id")
                val title = jsonObject.getString("title")
                val description = jsonObject.getString("description")
                val start_date = jsonObject.getString("start_date")
                val end_date = jsonObject.getString("end_date")

                val startDate = Constants.getDate(requireContext(), start_date, "yyyy-MM-dd HH:mm:ss", "EEEE dd MMM")
                val endDate = Constants.getDate(requireContext(), end_date, "yyyy-MM-dd HH:mm:ss", "EEEE dd MMM")

                eventsArray.add(EventClass(image, Id, title, description, startDate, endDate, "Active"))
            }
            eventsAdapter.notifyDataSetChanged()

            if (eventsArray.isEmpty()) {
                eventsLayout.visibility = View.GONE
            } else {
                eventsLayout.visibility = View.VISIBLE
            }

            for (index in 0..(new_locations.length() - 1)) {
                val jsonObject = new_locations[index] as JSONObject
                val locationClass = getLocation(jsonObject)
                newAddedArray.add(locationClass)
            }
            newAddedAdapter.notifyDataSetChanged()

            if (newAddedArray.isEmpty()) {
                newAddedLayout.visibility = View.GONE
            } else {
                newAddedLayout.visibility = View.VISIBLE
            }

            for (index in 0..(most_visited.length() - 1)) {
                val jsonObject = most_visited[index] as JSONObject
                val locationClass = getLocation(jsonObject)
                mostVisitedArray.add(locationClass)
            }
            mostVisitedAdapter.notifyDataSetChanged()

            if (mostVisitedArray.isEmpty()) {
                mostVisitedLayout.visibility = View.GONE
            } else {
                mostVisitedLayout.visibility = View.VISIBLE
            }

            for (index in 0..(last_visited.length() - 1)) {
                val jsonObject = last_visited[index] as JSONObject
                val locationClass = getLocation(jsonObject)
                lastVisitedArray.add(locationClass)
            }
            lastVisitedAdapter.notifyDataSetChanged()

            if (lastVisitedArray.isEmpty()) {
                lastVisitedLayout.visibility = View.GONE
            } else {
                lastVisitedLayout.visibility = View.VISIBLE
            }
            scrollView.visibility = View.VISIBLE
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
        }
        progressBar.visibility = View.GONE
    }

    fun getLocation(jsonObject: JSONObject): HomeLocationClass {
        val image = jsonObject.getString("image")
        val Id = jsonObject.getString("Id")
        val name = jsonObject.getString("name")
        val description = jsonObject.getString("description")

        val locationClass = HomeLocationClass(image, Id, name, description)
        return locationClass
    }
}
