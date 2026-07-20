
package com.vastlb.wing_me.Classes

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.vastlb.wing_me.DataClasses.LocationClass
import com.vastlb.wing_me.Launch.LaunchActivity
import com.vastlb.wing_me.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class BackgroundService: Service() {

    val LOCATION_CHANNEL_ID = "Foreground_Service"
    val NOTIFICATION_CHANNEL_ID = "Notification"

    val allLocationsArray = ArrayList<LocationClass>()

    var latitude = 0.0
    var longitude = 0.0

    companion object {

        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, BackgroundService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, BackgroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val intent = Intent(this, LaunchActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, LOCATION_CHANNEL_ID)
            .setContentTitle("Location Update")
            .setContentIntent(pendingIntent)
            .build()

        createLocationChannel()

        startForeground(1, builder)
        request()
        getLocation()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createLocationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(LOCATION_CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun getLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        if (Build.VERSION.SDK_INT > 28) {
            if (ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            } else {
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
        val client: SettingsClient = LocationServices.getSettingsClient(applicationContext)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        val locationCallback = object: LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {
                val location = getBestAccuracy(locationResult.locations) ?: return

                latitude = location.latitude
                longitude = location.longitude

                val accuracy = 60

                if (location.accuracy <= accuracy) {
                    checkLocation(location.accuracy)
                }
            }
        }

        task.addOnSuccessListener {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
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

    fun request() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")
        val url = Constants.url + "get_location_category.php"

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
                val location = jsonObject.getJSONArray("location")
                setLocationsArray(location)
            }
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
    }

    fun setLocationsArray(array: JSONArray) {
        for (index in 0..(array.length() - 1)) {
            val jsonObject = array[index] as JSONObject
            val image = jsonObject.getString("image")
            val Id = jsonObject.getString("Id")
            val name = jsonObject.getString("name")
            val description = jsonObject.getString("description")
            val google_latitude = jsonObject.getDouble("google_latitude")
            val google_longitude = jsonObject.getDouble("google_longitude")
            val radius = jsonObject.getDouble("radius")

            val locationClass = LocationClass(image, Id, name, description, google_latitude, google_longitude, radius, 0)
            allLocationsArray.add(locationClass)
        }
    }

    fun checkLocation(accuracy: Float) {
        if (allLocationsArray.isEmpty()) {
            return
        }
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
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
                val locationID = preferences.getString("LastLocationNotification", "")!!

                if (!locations.any {
                    jsonObject ->
                    jsonObject.id == locationID
                }) {
                    Constants.hideLocation()
                    sendNotification(locations)

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
                    sendNotification(locations)

                    val editor = preferences.edit()
                    editor.remove("LocationID")
                    editor.remove("LocationName")
                    editor.apply()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendNotification(array: ArrayList<LocationClass>) {
        val intent = Intent(this, LaunchActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
        val soundURI: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        for (index in 0..(array.size - 1)) {
            val jsonObject = array[index]

            val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
            val editor = preferences.edit()
            editor.remove("LastLocationAlert")
            editor.putString("LastLocationNotification", jsonObject.id)
            editor.apply()

            val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New Wing Me Location")
                .setContentText("Have you been to ${jsonObject.name}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundURI)
                .setContentIntent(pendingIntent)

            createNotificationChannel()

            with(NotificationManagerCompat.from(this)) {
                notify(jsonObject.id.toInt(), builder.build())
            }
        }
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification", NotificationManager.IMPORTANCE_DEFAULT)
            val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
