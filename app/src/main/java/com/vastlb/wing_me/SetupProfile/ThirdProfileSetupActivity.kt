
package com.vastlb.wing_me.SetupProfile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.vastlb.wing_me.Adapters.LookingForAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Main.MainActivity
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Supabase.SupabaseAuth
import com.vastlb.wing_me.Supabase.SupabaseData
import kotlinx.android.synthetic.main.activity_profile_setup_third.*
import org.json.JSONObject

class ThirdProfileSetupActivity: AppCompatActivity() {

    lateinit var adapter: LookingForAdapter

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
        setContentView(R.layout.activity_profile_setup_third)
        setViews()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java", ReplaceWith("showBackAlert()"))
    override fun onBackPressed() {
        showBackAlert()
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

        val array = Constants.getLookingForArray(getProgress(datingID), getProgress(socialisingID), getProgress(networkingID))

        id_back.setOnClickListener {
            showBackAlert()
        }

        id_previous.setOnClickListener {
            finish()
        }

        id_info.setOnClickListener {
            val intent = Intent(this, LexiconActivity::class.java)
            startActivity(intent)
        }

        id_next.setOnClickListener {
            val intent = Intent(this, FourthProfileSetupActivity::class.java)
            intent.putExtra("Height", height)
            intent.putExtra("Relationship", relationship)
            intent.putExtra("DatingID", array[0].progress.toString())
            intent.putExtra("SocialisingID", array[1].progress.toString())
            intent.putExtra("NetworkingID", array[2].progress.toString())
            intent.putExtra("Nationality", nationality)
            intent.putExtra("City", city)
            intent.putExtra("Drink", drink)
            intent.putExtra("Activity", activity)
            intent.putExtra("Profession", profession)
            startActivity(intent)
        }

        adapter = LookingForAdapter(array, this) {
            index, progress ->
            array[index].progress = progress
        }

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        id_recycler_view.layoutManager = linearLayoutManager
        id_recycler_view.adapter = adapter
    }

    fun getProgress(progress: String): Int {
        if (progress.isEmpty()) {
            return 0
        } else {
            return progress.toInt()
        }
    }

    fun showBackAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Do you want to save your changed info?")
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val dialogItems = arrayOf("Save Changes", "Discard Changes")

        alertDialog.setItems(dialogItems) { dialog, which ->

            when (which) {
                0 -> {
                    id_back.visibility = View.GONE
                    id_progress_bar.visibility = View.VISIBLE
                    send()
                }
                1 -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
            }
        }
        alertDialog.show()
    }

    fun send() {
        val array = Constants.getLookingForArray(getProgress(datingID), getProgress(socialisingID), getProgress(networkingID))
        val userId = SupabaseAuth.getUserId(this)

        if (userId == null) {
            sendError("Not signed in")
            return
        }
        val fields = JSONObject()
        fields.put("id", userId)
        if (height.isNotEmpty()) fields.put("height", height.toDoubleOrNull())
        if (relationship.isNotEmpty()) fields.put("relationship", relationship)
        fields.put("dating_id", array[0].progress)
        fields.put("socialising_id", array[1].progress)
        fields.put("networking_id", array[2].progress)
        if (nationality.isNotEmpty()) fields.put("nationality", nationality)
        if (city.isNotEmpty()) fields.put("city", city)

        SupabaseData.upsertProfile(this, fields, onSuccess = {
            sendSuccess()
        }, onError = { message ->
            sendError(message)
        })
    }

    fun sendError(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.show()
        id_back.visibility = View.VISIBLE
        id_progress_bar.visibility = View.GONE
    }

    fun sendSuccess() {
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        id_back.visibility = View.VISIBLE
        id_progress_bar.visibility = View.GONE
    }
}
