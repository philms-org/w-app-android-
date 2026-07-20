
package com.vastlb.wing_me.Launch

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_first_start.*

class FirstStartActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_start)
        setViews()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java", ReplaceWith("finishAffinity()"))
    override fun onBackPressed() {
        finishAffinity()
    }

    fun setViews() {
        id_next.setOnClickListener {
            val intent = Intent(this, SecondStartActivity::class.java)
            startActivity(intent)
        }
    }
}