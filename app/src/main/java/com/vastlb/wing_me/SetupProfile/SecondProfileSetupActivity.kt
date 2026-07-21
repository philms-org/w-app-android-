
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
import com.vastlb.wing_me.Adapters.RelationshipsAdapter
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Main.MainActivity
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Supabase.SupabaseAuth
import com.vastlb.wing_me.Supabase.SupabaseData
import kotlinx.android.synthetic.main.activity_profile_setup_second.*
import org.json.JSONObject

class SecondProfileSetupActivity: AppCompatActivity() {

    lateinit var adapter: RelationshipsAdapter

    var lastSelected = 0

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
        setContentView(R.layout.activity_profile_setup_second)
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

        if (!relationship.isEmpty()) {
            val index = Constants.getRelationships(0).indexOfFirst {
                jsonObject ->
                jsonObject.id == relationship
            }
            lastSelected = index
        }
        val array = Constants.getRelationships(lastSelected)

        id_back.setOnClickListener {
            showBackAlert()
        }

        id_previous.setOnClickListener {
            finish()
        }

        id_next.setOnClickListener {
            val jsonObject = array[lastSelected]

            val intent = Intent(this, ThirdProfileSetupActivity::class.java)
            intent.putExtra("Height", height)
            intent.putExtra("Relationship", jsonObject.id)
            intent.putExtra("DatingID", datingID)
            intent.putExtra("SocialisingID", socialisingID)
            intent.putExtra("NetworkingID", networkingID)
            intent.putExtra("Nationality", nationality)
            intent.putExtra("City", city)
            intent.putExtra("Drink", drink)
            intent.putExtra("Activity", activity)
            intent.putExtra("Profession", profession)
            startActivity(intent)
        }

        adapter = RelationshipsAdapter(array, this) {
            index ->
            array[lastSelected].isSelected = false
            array[index].isSelected = true
            adapter.notifyDataSetChanged()
            lastSelected = index
        }

        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        id_recycler_view.layoutManager = linearLayoutManager
        id_recycler_view.adapter = adapter
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
        val array = Constants.getRelationships(lastSelected)
        val jsonObject = array[lastSelected]
        val userId = SupabaseAuth.getUserId(this)

        if (userId == null) {
            sendError("Not signed in")
            return
        }
        val fields = JSONObject()
        fields.put("id", userId)
        if (height.isNotEmpty()) fields.put("height", height.toDoubleOrNull())
        fields.put("relationship", jsonObject.id)
        if (datingID.isNotEmpty()) fields.put("dating_id", datingID.toIntOrNull())
        if (socialisingID.isNotEmpty()) fields.put("socialising_id", socialisingID.toIntOrNull())
        if (networkingID.isNotEmpty()) fields.put("networking_id", networkingID.toIntOrNull())
        if (nationality.isNotEmpty()) fields.put("nationality", nationality)
        if (city.isNotEmpty()) fields.put("city", city)
        // `drink`/`activity`/`profession` intentionally omitted — no matching columns
        // on the live `profiles` table yet (see FirstProfileSetupActivity for detail).

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
