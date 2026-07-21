
package com.vastlb.wing_me.Main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import com.vastlb.wing_me.Classes.BackgroundService
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Profile.EditProfileActivity
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Settings.NotificationsActivity
import com.vastlb.wing_me.Settings.SettingsActivity
import com.vastlb.wing_me.SetupProfile.SecondProfileSetupActivity
import com.vastlb.wing_me.SetupProfile.ThirdProfileSetupActivity
import com.vastlb.wing_me.Supabase.SupabaseAuth
import com.vastlb.wing_me.Supabase.SupabaseData
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment: Fragment() {

    lateinit var close: () -> Unit

    lateinit var logout: TextView
    lateinit var logoutProgressBar: ProgressBar

    lateinit var imageView: ImageView
    lateinit var nameTextView: TextView
    lateinit var setupProfile: TextView
    lateinit var editProfileLayout: RelativeLayout
    lateinit var settingsLayout: RelativeLayout
    lateinit var phoneTextView: TextView
    lateinit var emailTextView: TextView
    lateinit var cityTextView: TextView
    lateinit var nationalityTextView: TextView
    lateinit var ageTextView: TextView
    lateinit var genderTextView: TextView
    lateinit var heightTextView: TextView
    lateinit var relationshipTextView: TextView
    lateinit var lookingForTextView: TextView
    lateinit var drinkTextView: TextView
    lateinit var fridayActivityTextView: TextView
    lateinit var professionTextView: TextView

    lateinit var scrollView: ScrollView
    lateinit var progressBar: ProgressBar

    var imageURL = ""
    var name = ""
    var phone = ""
    var email = ""
    var birthDate = ""
    var agePrivacy = ""
    var gender = ""

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        setViews(view)
        return view
    }

    companion object {
        fun newInstance(): ProfileFragment = ProfileFragment()
    }

    fun setViews(view: View) {
        Constants.reloadProfile = {
            reload()
        }

        val notification: ImageView = view.findViewById(R.id.id_notifications)

        val editProfile: TextView = view.findViewById(R.id.id_edit_profile)
        val settings: TextView = view.findViewById(R.id.id_settings)
        logout = view.findViewById(R.id.id_logout)
        logoutProgressBar = view.findViewById(R.id.id_logout_progress_bar)

        imageView = view.findViewById(R.id.id_image_view)
        nameTextView = view.findViewById(R.id.id_name_text_view)
        val setupProfileLayout: RelativeLayout = view.findViewById(R.id.id_setup_profile_layout)
        setupProfile = view.findViewById(R.id.id_setup_profile)
        editProfileLayout = view.findViewById(R.id.id_edit_profile_layout)
        settingsLayout = view.findViewById(R.id.id_settings_layout)
        phoneTextView = view.findViewById(R.id.id_phone_text_view)
        emailTextView = view.findViewById(R.id.id_email_text_view)
        cityTextView = view.findViewById(R.id.id_city_text_view)
        nationalityTextView = view.findViewById(R.id.id_nationality_text_view)
        ageTextView = view.findViewById(R.id.id_age_text_view)
        genderTextView = view.findViewById(R.id.id_gender_text_view)
        heightTextView = view.findViewById(R.id.id_height_text_view)
        relationshipTextView = view.findViewById(R.id.id_relationship_text_view)
        lookingForTextView = view.findViewById(R.id.id_looking_for_text_view)
        drinkTextView = view.findViewById(R.id.id_drink_text_view)
        fridayActivityTextView = view.findViewById(R.id.id_friday_night_text_view)
        professionTextView = view.findViewById(R.id.id_profession_text_view)

        scrollView = view.findViewById(R.id.id_scroll_view)
        progressBar = view.findViewById(R.id.id_progress_bar)

        scrollView.visibility = View.GONE

        val preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val setup = preferences.getString("Setup", "")

        if (setup == "true") {
            setupProfileLayout.visibility = View.GONE
        }

        notification.setOnClickListener {
            val intent = Intent(context, NotificationsActivity::class.java)
            startActivity(intent)
        }

        setupProfile.setOnClickListener {
            openSetupProfile()
        }

        editProfile.setOnClickListener {
            openEditProfile()
        }

        settings.setOnClickListener {
            val intent = Intent(context, SettingsActivity::class.java)
            startActivity(intent)
        }

        logout.setOnClickListener {
            val alertDialog = AlertDialog.Builder(context)
            alertDialog.setTitle(getString(R.string.alert_logout))
            alertDialog.setPositiveButton(getString(R.string.logout)) {
                _, _ ->
                logout.visibility = View.GONE
                logoutProgressBar.visibility = View.VISIBLE
                logout()
            }
            alertDialog.setNegativeButton(getString(R.string.cancel), null)
            val alert = alertDialog.create()
            alert.show()
        }

        request()
    }

    fun openEditProfile() {
        val intent = Intent(context, EditProfileActivity::class.java)
        intent.putExtra("Image", imageURL)
        intent.putExtra("Name", name)
        intent.putExtra("Phone", phone)
        intent.putExtra("Email", email)
        intent.putExtra("Birth", birthDate)
        intent.putExtra("AgePrivacy", agePrivacy)
        intent.putExtra("Gender", gender)

        intent.putExtra("Height", height)
        intent.putExtra("Relationship", relationship)
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

    fun reload() {
        scrollView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        request()
    }

    fun request() {
        val userId = SupabaseAuth.getUserId(requireContext())

        if (userId == null) {
            Constants.deleteUserData(requireContext())
            close()
            return
        }
        SupabaseData.fetchProfile(requireContext(), userId, onSuccess = { profile ->
            if (profile != null) {
                requestSuccess(profile)
            } else {
                val toast = Toast.makeText(context, "Profile not found", Toast.LENGTH_LONG)
                toast.show()
            }
            progressBar.visibility = View.GONE
        }, onError = { message ->
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()
            progressBar.visibility = View.GONE
        })
    }

    // NOTE: the live `profiles` table does not yet have `fave_drink`, `friday_night`,
    // `profession`, or `email` columns (verified 2026-07-20 against the real project —
    // see SupabaseData.kt), so `drink`/`activity`/`profession`/`email` always render as
    // "---" here until those columns exist. `gender` and `date_of_birth` DO exist and
    // are used for the gender/age rows.
    @SuppressLint("SetTextI18n")
    fun requestSuccess(message: JSONObject) {
        imageURL = message.optString("avatar_url", "")
        if (imageURL.isNotEmpty()) {
            Picasso.get().load(imageURL).into(imageView)
        }

        name = message.optString("display_name", "")
        phone = message.optString("phone", "")
        email = ""

        datingID = optIntAsString(message, "dating_id")
        socialisingID = optIntAsString(message, "socialising_id")
        networkingID = optIntAsString(message, "networking_id")

        city = message.optString("city", "")
        drink = ""
        activity = ""
        profession = ""

        nameTextView.setText(name)
        phoneTextView.setText(phone)
        emailTextView.setText(if (email.isEmpty()) "---" else email)

        cityTextView.setText(if (city.isEmpty()) "---" else city)
        drinkTextView.setText("---")
        fridayActivityTextView.setText("---")
        professionTextView.setText("---")

        nationality = message.optString("nationality", "")

        if (nationality.isEmpty()) {
            nationalityTextView.setText("---")
        } else {
            val flags = Constants.getFlags()
            val jsonObject = flags.firstOrNull { jsonObject ->
                jsonObject.id == nationality
            }
            if (jsonObject != null) {
                val locale = Locale("en_US", jsonObject.id)
                val name = locale.getDisplayCountry()
                nationalityTextView.setText("${name} ${jsonObject.emoji}")
            } else {
                nationalityTextView.setText("---")
            }
        }

        birthDate = message.optString("date_of_birth", "")

        if (birthDate.isEmpty()) {
            ageTextView.setText("---")
        } else {
            ageTextView.setText(getAge(birthDate))
        }
        agePrivacy = ""

        gender = message.optString("gender", "")
        genderTextView.setText(getGender(gender))

        height = optDoubleAsString(message, "height")

        if (height.isEmpty()) {
            heightTextView.setText("---")
        } else {
            heightTextView.setText("${height}m")
        }

        relationship = message.optString("relationship", "")

        if (relationship.isEmpty()) {
            relationshipTextView.setText("---")
        } else {
            val array = Constants.getRelationships(0)
            val jsonObject = array.firstOrNull { jsonObject ->
                jsonObject.id == relationship
            }
            relationshipTextView.setText(jsonObject?.title ?: "---")
        }

        Constants.datingID = datingID
        Constants.socialisingID = socialisingID
        Constants.networkingID = networkingID
        val lookingFor = Constants.getLookingFor(Constants.datingID, Constants.socialisingID, Constants.networkingID)

        if (lookingFor.isEmpty()) {
            lookingForTextView.setText("---")
        } else {
            lookingForTextView.setText(lookingFor)
        }

        if (gender == "F") {
            editProfileLayout.setBackgroundResource(R.drawable.view_drawable_profile_button_pink)
            settingsLayout.setBackgroundResource(R.drawable.view_drawable_profile_button_pink)
        } else {
            editProfileLayout.setBackgroundResource(R.drawable.view_drawable_profile_button_blue)
            settingsLayout.setBackgroundResource(R.drawable.view_drawable_profile_button_blue)
        }
        scrollView.visibility = View.VISIBLE

        if (city.isEmpty() || nationality.isEmpty() || height.isEmpty()) {
            setup()
        }
    }

    fun optIntAsString(json: JSONObject, key: String): String {
        if (json.isNull(key) || !json.has(key)) return ""
        return json.optInt(key).toString()
    }

    fun optDoubleAsString(json: JSONObject, key: String): String {
        if (json.isNull(key) || !json.has(key)) return ""
        return json.optDouble(key).toString()
    }

    fun getGender(gender: String): String {
        if (gender == "M") {
            return "Male"
        } else if (gender == "F") {
            return "Female"
        } else if (gender == "O") {
            return "Other"
        } else {
            return ""
        }
    }

    fun getAge(dateString: String): String {
        val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val date = dateParser.parse(dateString)!!

        val birth = Calendar.getInstance()
        val today = Calendar.getInstance()

        birth.time = date

        var age = today[Calendar.YEAR] - birth[Calendar.YEAR]

        if (today[Calendar.DAY_OF_YEAR] < birth[Calendar.DAY_OF_YEAR]) {
            age--
        }
        val ageInt = age

        return ageInt.toString()
    }

    fun logout() {
        SupabaseAuth.signOut(requireContext()) {
            logoutSuccess()
        }
    }

    fun logoutSuccess() {
        Constants.hideLocation()
        Constants.deleteUserData(requireContext())

        logout.visibility = View.VISIBLE
        logoutProgressBar.visibility = View.GONE

        BackgroundService.stopService(requireContext())
        close()
    }

    fun setup() {
        Handler().postDelayed({
            showSetupAlert()
        }, 30000)
    }

    fun showSetupAlert() {
        val context = context ?: return

        val preferences = context.getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val setup = preferences.getString("Setup", "")!!

        if (!setup.isEmpty()) {
            return
        }
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle(getString(R.string.alert_setup))
        alertDialog.setNegativeButton(getString(R.string.cancel), null)
        val dialogItems = arrayOf("Setup Now", "Later", "Never")

        alertDialog.setItems(dialogItems) {
            dialog, which ->

            when (which) {
                0 -> {
                    openSetupProfile()
                }
                2 -> {
                    val editor = preferences.edit()
                    editor.putString("Setup", "false")
                    editor.apply()
                }
            }
        }
        alertDialog.show()
    }

    fun openSetupProfile() {
        val intent = Intent(context, SecondProfileSetupActivity::class.java)
        intent.putExtra("Height", height)
        intent.putExtra("Relationship", relationship)
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
}
