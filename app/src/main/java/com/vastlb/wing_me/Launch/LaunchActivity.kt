
package com.vastlb.wing_me.Launch

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.vastlb.wing_me.Main.MainActivity
import com.vastlb.wing_me.R

class LaunchActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
        start()
    }

    fun start() {
        Handler().postDelayed({
            val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
            val start = preferences.getBoolean("Start", false)

            if (start) {
                val token = preferences.getString("Token", "")!!

                if (token.isEmpty()) {
                    openWelcome()
                } else {
                    openMain()
                }
            } else {
                openStart()
            }
        }, 1000)
    }

    fun openStart() {
        val intent = Intent(this, FirstStartActivity::class.java)
        startActivity(intent)
    }

    fun openWelcome() {
        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
    }

    fun openMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
