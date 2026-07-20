
package com.vastlb.wing_me.Main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.Launch.WelcomeActivity
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException

class MainActivity: AppCompatActivity() {

    lateinit var lastFragment: Fragment
    lateinit var lastImageView: ImageView

    val fragmentManager = supportFragmentManager
    val homeFragment = HomeFragment.newInstance()
    val mapFragment = MapFragment.newInstance()
    val myLocationFragment = NewMyLocationFragment.newInstance()
    val messagesFragment = MessagesFragment.newInstance()
    val profileFragment = ProfileFragment.newInstance()

    var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setFragments()
        setViews()
        setToken()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java", ReplaceWith("finishAffinity()"))
    override fun onBackPressed() {
        finishAffinity()
    }

    fun setFragments() {
        lastFragment = mapFragment
        lastImageView = id_map

        messagesFragment.showBadge = {
            if (index != 3) {
                id_badge_view.visibility = View.VISIBLE
            }
        }

        profileFragment.close = {
            close()
        }

        fragmentManager.beginTransaction().add(R.id.id_container, homeFragment).hide(homeFragment).commit()
        fragmentManager.beginTransaction().add(R.id.id_container, mapFragment).commit()
        fragmentManager.beginTransaction().add(R.id.id_container, myLocationFragment).hide(myLocationFragment).commit()
        fragmentManager.beginTransaction().add(R.id.id_container, messagesFragment).hide(messagesFragment).commit()
        fragmentManager.beginTransaction().add(R.id.id_container, profileFragment).hide(profileFragment).commit()

        updatesUI(mapFragment, id_map)
    }

    fun setViews() {
        id_badge_view.visibility = View.GONE

        Constants.selectMyLocation = {
            updatesUI(myLocationFragment, id_my_location)
        }

        id_home.setOnClickListener {
            index = 1
            updatesUI(homeFragment, id_home)
        }

        id_map.setOnClickListener {
            index = 2
            updatesUI(mapFragment, id_map)
        }

        id_my_location.setOnClickListener {
            index = 3
            updatesUI(myLocationFragment, id_my_location)
        }

        id_messages.setOnClickListener {
            index = 4
            id_badge_view.visibility = View.GONE
            updatesUI(messagesFragment, id_messages)
        }

        id_profile.setOnClickListener {
            index = 5
            updatesUI(profileFragment, id_profile)
        }
    }

    fun updatesUI(fragment: Fragment, imageView: ImageView) {
        fragmentManager.beginTransaction().hide(lastFragment).commit()
        fragmentManager.beginTransaction().show(fragment).commit()
        lastFragment = fragment

        lastImageView.setColorFilter(getColor(R.color.white))
        imageView.setColorFilter(getColor(R.color.blue))
        lastImageView = imageView
    }

    fun close() {
        val close = intent.getBooleanExtra("Close", false)

        if (close) {
            finish()
        } else {
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    fun setToken() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {

            }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            Constants.firebaseToken = task.result

            println(Constants.firebaseToken)

            val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
            val sent = preferences.getBoolean("Sent", false)

            if (!sent) {
                sendToken()
            }
        })
        FirebaseMessaging.getInstance().subscribeToTopic("Main")
    }

    fun sendToken() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val token = preferences.getString("Token", "")!!
        val url = Constants.url + "update_token.php"
        val uid = Constants.getUID(this)

        if (token.isEmpty()) {
            return
        }
        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    sendSuccess()
                } catch (e: JSONException) {
                    println("Error1")
                }
            },
            Response.ErrorListener {
                sendError()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = token
                return headers
            }
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["firebase"] = Constants.firebaseToken
                params["uid"] = uid
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun sendError() {
        println("Error2")
        sendToken()
    }

    fun sendSuccess() {
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putBoolean("Sent", true)
        editor.apply()
    }
}