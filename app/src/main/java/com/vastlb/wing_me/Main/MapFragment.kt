
package com.vastlb.wing_me.Main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.vastlb.wing_me.Adapters.CategoriesAdapter
import com.vastlb.wing_me.Adapters.LocationsAdapter
import com.vastlb.wing_me.Classes.BackgroundService
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.DataClasses.CategoryClass
import com.vastlb.wing_me.DataClasses.LocationClass
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Supabase.SupabaseData
import com.vastlb.wing_me.User.SingleLocationActivity
import org.json.JSONArray
import org.json.JSONObject

class MapFragment: Fragment(), OnMapReadyCallback {

    val PERMISSION_REQUEST_CODE = 1

    lateinit var map: GoogleMap

    lateinit var categoriesAdapter: CategoriesAdapter
    lateinit var locationsAdapter: LocationsAdapter

    lateinit var searchEditText: EditText
    lateinit var locationsRecyclerView: RecyclerView
    lateinit var showList: CardView
    lateinit var noPermissionLayout: RelativeLayout
    lateinit var progressBar: ProgressBar

    val categoriesArray = ArrayList<CategoryClass>()
    val allLocationsArray = ArrayList<LocationClass>()
    var locationsArray = ArrayList<LocationClass>()

    var lastCategory = 0
    var latitude = 0.0
    var longitude = 0.0
    var once = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        setViews(view)
        return view
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            }
        }
    }

    companion object {
        fun newInstance(): MapFragment = MapFragment()
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map.setMapStyle(MapStyleOptions(getResources().getString(R.string.style_json)))

        map.setOnMarkerClickListener {
            marker ->
            val id = marker.snippet

            val intent = Intent(context, SingleLocationActivity::class.java)
            intent.putExtra("ID", id)
            startActivity(intent)
            return@setOnMarkerClickListener true
        }
    }

    fun setViews(view: View) {
        val mapFragment = childFragmentManager.findFragmentById(R.id.id_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val categoriesRecyclerView: RecyclerView = view.findViewById(R.id.id_categories_recycler_view)
        searchEditText = view.findViewById(R.id.id_search_edit_text)
        locationsRecyclerView = view.findViewById(R.id.id_locations_recycler_view)
        val listLayout: RelativeLayout = view.findViewById(R.id.id_list_layout)
        showList = view.findViewById(R.id.id_show_list)
        val hideList: RelativeLayout = view.findViewById(R.id.id_hide_list)
        noPermissionLayout = view.findViewById(R.id.id_no_permission_layout)
        val allowPermission: TextView = view.findViewById(R.id.id_allow_permission)
        progressBar = view.findViewById(R.id.id_progress_bar)

        val locationManager = requireContext().getSystemService(LOCATION_SERVICE) as LocationManager?

        searchEditText.addTextChangedListener(object: TextWatcher {

            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onTextChanged()
            }
        })

        showList.setOnClickListener {
            showList.visibility = View.GONE
            listLayout.visibility = View.VISIBLE
        }

        hideList.setOnClickListener {
            showList.visibility = View.VISIBLE
            listLayout.visibility = View.GONE
        }

        allowPermission.setOnClickListener {
            if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                getLocation()
            } else {
                val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(callGPSSettingIntent)
            }
        }

        categoriesAdapter = CategoriesAdapter(categoriesArray, requireContext()) {
            index ->
            categoriesArray[lastCategory].isSelected = false
            categoriesArray[index].isSelected = true
            lastCategory = index
            categoriesAdapter.notifyDataSetChanged()

            val jsonObject = categoriesArray[index]
            setLocations(jsonObject.array)
        }

        val categoriesLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        categoriesRecyclerView.layoutManager = categoriesLayoutManager
        categoriesRecyclerView.adapter = categoriesAdapter

        locationsAdapter = LocationsAdapter(locationsArray) {
            index ->
            val jsonObject = locationsArray[index]

            val intent = Intent(context, SingleLocationActivity::class.java)
            intent.putExtra("ID", jsonObject.id)
            startActivity(intent)
        }

        val locationsLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        locationsRecyclerView.layoutManager = locationsLayoutManager
        locationsRecyclerView.adapter = locationsAdapter

        if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                getLocation()
                return
            }
        }
    }

    fun getLocation() {
        val context = context ?: return

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        if (Build.VERSION.SDK_INT > 28) {
            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    BackgroundService.startService(requireContext(), "Foreground Service is running...")
                } else {
                    val alertDialog = AlertDialog.Builder(context)
                    alertDialog.setMessage("This app uses the background location to notify you once you enter a registered location even when the app is closed")
                    alertDialog.setPositiveButton("Allow Permission") {
                        _, _ ->
                        requestPermissions(arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSION_REQUEST_CODE)
                    }
                    alertDialog.setNegativeButton(getString(R.string.cancel), null)
                    val alert = alertDialog.create()
                    alert.show()
                }
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
                return
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        val locationCallback = object: LocationCallback() {

            @SuppressLint("MissingPermission")
            override fun onLocationResult(locationResult: LocationResult) {
                val location = getBestAccuracy(locationResult.locations) ?: return

                latitude = location.latitude
                longitude = location.longitude

                val accuracy = 60

                if (location.accuracy <= accuracy) {
                    checkLocation(location.accuracy)
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
                if (once) {
                    return
                }
                val camera = CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 13f)
                map.animateCamera(camera)
                map.isMyLocationEnabled = true
                once = true
            }
        }

        task.addOnSuccessListener {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
        noPermissionLayout.visibility = View.GONE
        showList.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE

        Constants.inLocation = false
        request()
    }

    fun getBestAccuracy(locations: List<Location>): Location? {
        if (locations.isEmpty()) {
            return null
        }
        val firstLocation = locations[0]

        var minAccuracy = firstLocation.accuracy
        var bestLoccation = firstLocation

        for (index in 0..(locations.size - 1)) {
            val jsonObject = locations[index]

            if (jsonObject.accuracy < minAccuracy) {
                minAccuracy = jsonObject.accuracy
                bestLoccation = jsonObject
            }
        }
        return bestLoccation
    }

    fun onTextChanged() {
        if (categoriesArray.isEmpty()) {
            return
        }
        val array = categoriesArray[lastCategory].array
        locationsArray.clear()

        for (index in 0..(array.size - 1)) {
            val jsonObject = array[index]

            if (searchEditText.text.toString().isEmpty()) {
                locationsArray.add(jsonObject)
            } else {
                if (jsonObject.name.lowercase().startsWith(searchEditText.text.toString().lowercase())) {
                    locationsArray.add(jsonObject)
                }
            }
        }
        locationsAdapter.notifyDataSetChanged()
    }

    // The `locations` table (shared Supabase schema) has no category grouping, unlike
    // the old PHP backend's get_location_category.php — every venue is loaded into a
    // single "All" category, which is the only category the existing UI actually needs
    // to keep the map/list/geofencing logic below working unmodified.
    fun request() {
        SupabaseData.fetchVenues(requireContext(), onSuccess = { venues ->
            requestSuccess(venues)
        }, onError = { message ->
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
            categoriesAdapter.notifyDataSetChanged()
            progressBar.visibility = View.GONE
        })
    }

    fun requestSuccess(venues: JSONArray) {
        val array = getLocationsArray(venues)
        var allCount = 0

        for (index in 0 until array.size) {
            allCount += array[index].count
        }
        setLocations(allLocationsArray)
        categoriesArray.add(0, CategoryClass("", "All", allCount, allLocationsArray, true))
        categoriesAdapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
    }

    fun getLocationsArray(array: JSONArray): ArrayList<LocationClass> {
        val locationsArray = ArrayList<LocationClass>()

        for (index in 0 until array.length()) {
            val jsonObject = array.getJSONObject(index)
            val image = jsonObject.optString("banner_image", "")
            val id = jsonObject.getString("id")
            val name = jsonObject.optString("name", "")
            val description = jsonObject.optString("description", "")
            val latitude = jsonObject.optDouble("lat", 0.0)
            val longitude = jsonObject.optDouble("lng", 0.0)
            val radius = jsonObject.optInt("geofence_radius_meters", 100).toDouble()

            val locationClass = LocationClass(image, id, name, description, latitude, longitude, radius, 0)
            allLocationsArray.add(locationClass)
            locationsArray.add(locationClass)
        }
        return locationsArray
    }

    fun setLocations(array: ArrayList<LocationClass>) {
        map.clear()
        locationsArray.clear()

        for (index in 0..(array.size - 1)) {
            val jsonObject = array[index]

            if (searchEditText.text.toString().isEmpty()) {
                locationsArray.add(jsonObject)
            } else {
                if (jsonObject.name.lowercase().startsWith(searchEditText.text.toString().lowercase())) {
                    locationsArray.add(jsonObject)
                }
            }
            val marker = MarkerOptions()
            marker.position(LatLng(jsonObject.latitude, jsonObject.longitude))
            marker.title(jsonObject.name)
            marker.snippet(jsonObject.id)
            marker.icon(getBitmap(requireContext(), R.drawable.icon_pin))
            map.addMarker(marker)
        }
        locationsAdapter.notifyDataSetChanged()

        if (!locationsArray.isEmpty()) {
            locationsRecyclerView.scrollToPosition(0)
        }
    }

    private fun getBitmap(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, 160, 160)
            val bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    fun checkLocation(accuracy: Float) {
        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")!!
        val locationID = preferences.getString("LocationID", "")!!

        if (token.isEmpty()) {
            return
        }
        if (locationID.contains("Event")) {
            return
        }
        if (allLocationsArray.isEmpty()) {
            return
        }
        val locations = ArrayList<LocationClass>()

        if (Constants.inLocation) {
            for (index in 0..(allLocationsArray.size - 1)) {
                val jsonObject = allLocationsArray[index]

                val userCoordinates = Location("locationA")
                userCoordinates.latitude = latitude
                userCoordinates.longitude = longitude

                val locationCoordinates = Location("locationB")
                locationCoordinates.latitude = jsonObject.latitude
                locationCoordinates.longitude = jsonObject.longitude

                val distance = userCoordinates.distanceTo(locationCoordinates)

                if (accuracy < 20) {
                    if (distance <= jsonObject.radius + 20) {
                        locations.add(jsonObject)
                    }
                } else {
                    if (distance <= jsonObject.radius + accuracy) {
                        locations.add(jsonObject)
                    }
                }
            }
            if (locations.isEmpty()) {
                Constants.inLocation = false
                Constants.hideLocation()

                val editor = preferences.edit()
                editor.remove("LocationID")
                editor.remove("LocationName")
                editor.remove("LastLocationAlert")
                editor.remove("LastLocationNotification")
                editor.apply()
            } else {
                val locationID = preferences.getString("LastLocationAlert", "")!!

                if (!locations.any {
                    jsonObject ->
                    jsonObject.id == locationID
                }) {
                    Constants.hideLocation()
                    showLocationsAlert(locations)

                    val editor = preferences.edit()
                    editor.remove("LocationID")
                    editor.remove("LocationName")
                    editor.apply()
                }
            }
        } else {
            for (index in 0..(allLocationsArray.size - 1)) {
                val jsonObject = allLocationsArray[index]

                val userCoordinates = Location("locationA")
                userCoordinates.latitude = latitude
                userCoordinates.longitude = longitude

                val locationCoordinates = Location("locationB")
                locationCoordinates.latitude = jsonObject.latitude
                locationCoordinates.longitude = jsonObject.longitude

                val distance = userCoordinates.distanceTo(locationCoordinates)

                if (distance <= jsonObject.radius + 10) {
                    locations.add(jsonObject)
                }
            }
            if (locations.isEmpty()) {
                val editor = preferences.edit()
                editor.remove("LocationID")
                editor.remove("LocationName")
                editor.remove("LastLocationAlert")
                editor.remove("LastLocationNotification")
                editor.apply()
            } else {
                Constants.inLocation = true

                val locationID = preferences.getString("LocationID", "")!!
                val locationName = preferences.getString("LocationName", "")!!

                if (locations.any {
                    jsonObject ->
                    jsonObject.id == locationID
                }) {
                    val locationClass = LocationClass("", locationID, locationName, "", 0.0, 0.0, 0.0, 0)
                    Constants.wingMe(locationClass)
                } else {
                    Constants.hideLocation()
                    showLocationsAlert(locations)

                    val editor = preferences.edit()
                    editor.remove("LocationID")
                    editor.remove("LocationName")
                    editor.apply()
                }
            }
        }
    }

    fun showLocationsAlert(array: ArrayList<LocationClass>) {
        val context = context ?: return

        if (array.size > 1) {
            val jsonObject = array.first()
            val preferences = context.getSharedPreferences("Preferences", Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString("LastLocationAlert", jsonObject.id)
            editor.putString("LastLocationNotification", jsonObject.id)
            editor.apply()

            val stringArray = getStringArray(array)

            val alertDialog = AlertDialog.Builder(requireContext())
            alertDialog.setTitle("You have entered a Wing Me location")
            alertDialog.setNegativeButton(getString(R.string.maybe_later), null)

            alertDialog.setItems(stringArray) { dialog, which ->
                val jsonObject = array[which]
                Constants.wingMe(jsonObject)
                Constants.selectMyLocation()
            }
            alertDialog.show()
        } else {
            val jsonObject = array.first()
            val preferences = context.getSharedPreferences("Preferences", Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString("LastLocationAlert", jsonObject.id)
            editor.putString("LastLocationNotification", jsonObject.id)
            editor.apply()

            val alertDialog = AlertDialog.Builder(requireContext())
            alertDialog.setTitle("May I wing you into ${jsonObject.name}?")
            alertDialog.setPositiveButton(getString(R.string.wing_me)) {
                _, _ ->
                Constants.wingMe(jsonObject)
                Constants.selectMyLocation()
            }
            alertDialog.setNegativeButton(getString(R.string.maybe_later), null)
            val alert = alertDialog.create()
            alert.show()
        }
    }

    fun getStringArray(array: ArrayList<LocationClass>): Array<String> {
        val stringArray = ArrayList<String>()

        for (index in 0..(array.size - 1)) {
            val jsonObject = array[index]
            stringArray.add("Wing me into ${jsonObject.name}")
        }
        return stringArray.toTypedArray()
    }
}
