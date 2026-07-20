
package com.vastlb.wing_me.Launch

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_second_start.*

class SecondStartActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second_start)
        setViews()
    }

    fun setViews() {
        id_start.setOnClickListener {
            val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putBoolean("Start", true)
            editor.apply()

            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
        }
    }
}