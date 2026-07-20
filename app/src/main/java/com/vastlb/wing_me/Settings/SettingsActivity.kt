
package com.vastlb.wing_me.Settings

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setViews()
    }

    fun setViews() {
        id_back.setOnClickListener {
            finish()
        }

        id_about.setOnClickListener {
            openAbout(getString(R.string.about_us), "about.php")
        }

        id_terms.setOnClickListener {
            openAbout(getString(R.string.terms_conditions), "terms.php")
        }

        id_privacy.setOnClickListener {
            openAbout(getString(R.string.privacy_policy), "privacy.php")
        }

        id_vast.setOnClickListener {
            val intent = Intent(this, VastActivity::class.java)
            startActivity(intent)
        }

        id_block_list.setOnClickListener {
            val intent = Intent(this, BlockListActivity::class.java)
            startActivity(intent)
        }

        id_share.setOnClickListener {
            val url = Constants.appURL
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
            startActivity(Intent.createChooser(shareIntent, "Share link using"))
        }

        id_contact.setOnClickListener {
            val intent = Intent(this, ContactActivity::class.java)
            startActivity(intent)
        }

        id_change_password.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }
    }

    fun openAbout(title: String, path: String) {
        val intent = Intent(this, AboutActivity::class.java)
        intent.putExtra("Title", title)
        intent.putExtra("Path", path)
        startActivity(intent)
    }
}