
package com.vastlb.wing_me.SetupProfile

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.vastlb.wing_me.Main.MainActivity
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Supabase.SupabaseAuth
import com.vastlb.wing_me.Supabase.SupabaseData
import kotlinx.android.synthetic.main.activity_profile_setup_fifth.*
import org.json.JSONObject

class FifthProfileSetupActivity: AppCompatActivity() {

    var height = ""
    var relationship = ""
    var datingID = ""
    var socialisingID = ""
    var networkingID = ""
    var nationality = ""
    var city = ""
    var drink = ""
    var activity = ""
    var profession = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup_fifth)
        setViews()
    }

    fun setViews() {
        height = intent.getStringExtra("Height")!!
        relationship = intent.getStringExtra("Relationship")!!
        datingID = intent.getStringExtra("DatingID")!!
        socialisingID = intent.getStringExtra("SocialisingID")!!
        networkingID = intent.getStringExtra("NetworkingID")!!
        nationality = intent.getStringExtra("Nationality")!!
        city = intent.getStringExtra("City")!!
        drink = intent.getStringExtra("Drink")!!
        activity = intent.getStringExtra("Activity")!!
        profession = intent.getStringExtra("Profession")!!

        id_city_edit_text.setText(city)
        id_drink_edit_text.setText(drink)
        id_friday_activity_edit_text.setText(activity)
        id_profession_edit_text.setText(profession)

        id_back.setOnClickListener {
            finish()
        }

        id_save.setOnClickListener {
            id_save.visibility = View.GONE
            id_save_progress_bar.visibility = View.VISIBLE
            send()
        }
    }

    fun send() {
        val userId = SupabaseAuth.getUserId(this)

        if (userId == null) {
            sendError("Not signed in")
            return
        }
        val fields = JSONObject()
        fields.put("id", userId)
        if (height.isNotEmpty()) fields.put("height", height.toDoubleOrNull())
        if (relationship.isNotEmpty()) fields.put("relationship", relationship)
        if (datingID.isNotEmpty()) fields.put("dating_id", datingID.toIntOrNull())
        if (socialisingID.isNotEmpty()) fields.put("socialising_id", socialisingID.toIntOrNull())
        if (networkingID.isNotEmpty()) fields.put("networking_id", networkingID.toIntOrNull())
        if (nationality.isNotEmpty()) fields.put("nationality", nationality)
        val cityText = id_city_edit_text.text.toString()
        if (cityText.isNotEmpty()) fields.put("city", cityText)
        // `id_drink_edit_text` / `id_friday_activity_edit_text` / `id_profession_edit_text`
        // are intentionally not sent — no matching columns on the live `profiles` table
        // yet (fave_drink / friday_night / profession). See final report.

        SupabaseData.upsertProfile(this, fields, onSuccess = {
            sendSuccess()
        }, onError = { message ->
            sendError(message)
        })
    }

    fun sendError(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.show()
        id_save.visibility = View.VISIBLE
        id_save_progress_bar.visibility = View.GONE
    }

    fun sendSuccess() {
        val preferences = getSharedPreferences("Preferences", MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("Setup", "true")
        editor.apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        id_save.visibility = View.VISIBLE
        id_save_progress_bar.visibility = View.GONE
    }
}
