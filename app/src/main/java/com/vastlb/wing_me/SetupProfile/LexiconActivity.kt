
package com.vastlb.wing_me.SetupProfile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_about.*

class LexiconActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lexicon)
        setViews()
    }

    fun setViews() {
        id_back.setOnClickListener {
            finish()
        }
    }
}
