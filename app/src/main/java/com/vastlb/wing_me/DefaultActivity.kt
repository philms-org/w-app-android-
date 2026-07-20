
package com.vastlb.wing_me

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_1_default.*

class DefaultActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_1_default)
        setViews()
    }

    fun setViews() {
        id_back.setOnClickListener {
            finish()
        }
    }
}
