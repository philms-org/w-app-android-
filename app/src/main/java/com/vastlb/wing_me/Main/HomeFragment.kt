
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
import com.vastlb.wing_me.Adapters.EventsAdapter
import com.vastlb.wing_me.Adapters.HomeBannerAdapter
import com.vastlb.wing_me.Adapters.HomeLocationsAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.DataClasses.BannerClass
import com.vastlb.wing_me.DataClasses.EventClass
import com.vastlb.wing_me.DataClasses.HomeLocationClass
import com.vastlb.wing_me.DataClasses.LocationClass
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Supabase.SupabaseData
import com.vastlb.wing_me.User.SingleLocationActivity
import org.json.JSONArray

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

    // The old get_home.php endpoint curated 4 different sections (banner/events/
    // new/most-visited/last-visited) server-side. banner now has a real Supabase
    // table (banners, migration 015); events/most-visited/last-visited still have
    // no equivalent, so every venue from `locations` is shown in the "new added"
    // section as before and those three sections stay hidden.
    fun request() {
        SupabaseData.fetchVenues(requireContext(), onSuccess = { venues ->
            SupabaseData.fetchBanners(requireContext(), onSuccess = { banners ->
                requestSuccess(venues, banners)
            }, onError = {
                // Banner load failure shouldn't block the rest of the screen.
                requestSuccess(venues, JSONArray())
            })
        }, onError = { message ->
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
            scrollView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        })
    }

    fun requestSuccess(venues: JSONArray, banners: JSONArray) {
        bannerArray.clear()
        for (index in 0 until banners.length()) {
            val jsonObject = banners.getJSONObject(index)
            val imageURL = jsonObject.optString("image_url", "")
            val locationID = jsonObject.optString("location_id", "")
            val link = jsonObject.optString("link", "")
            bannerArray.add(BannerClass(imageURL, locationID, link))
        }
        bannerAdapter.notifyDataSetChanged()
        bannerLayout.visibility = if (bannerArray.isEmpty()) View.GONE else View.VISIBLE

        eventsLayout.visibility = View.GONE

        newAddedArray.clear()

        for (index in 0 until venues.length()) {
            val jsonObject = venues.getJSONObject(index)
            newAddedArray.add(getLocation(jsonObject))
        }
        newAddedAdapter.notifyDataSetChanged()

        if (newAddedArray.isEmpty()) {
            newAddedLayout.visibility = View.GONE
        } else {
            newAddedLayout.visibility = View.VISIBLE
        }
        mostVisitedLayout.visibility = View.GONE
        lastVisitedLayout.visibility = View.GONE

        scrollView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    fun getLocation(jsonObject: org.json.JSONObject): HomeLocationClass {
        val image = jsonObject.optString("banner_image", "")
        val id = jsonObject.getString("id")
        val name = jsonObject.optString("name", "")
        val description = jsonObject.optString("description", "")

        return HomeLocationClass(image, id, name, description)
    }
}
