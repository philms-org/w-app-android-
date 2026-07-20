
package com.vastlb.wing_me.Launch

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.DataClasses.PickerClass
import com.vastlb.wing_me.Main.PickerFragment
import com.vastlb.wing_me.R
import kotlinx.android.synthetic.main.activity_forgot_password.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class ForgotPasswordActivity: AppCompatActivity() {

    val fragmentManager = supportFragmentManager
    val pickerFragment = PickerFragment.newInstance()

    val auth = FirebaseAuth.getInstance()

    var isOpen = false

    var step = 1
    var code = "1"
    var verificationID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        setViews()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isOpen) {
            isOpen = false
            fragmentManager.beginTransaction().hide(pickerFragment).commit()
        } else {
            finish()
        }
    }

    fun setViews() {
        id_code_layout.visibility = View.GONE
        id_password_layout.visibility = View.GONE
        id_confirm_layout.visibility = View.GONE

        fragmentManager.beginTransaction().add(R.id.id_container, pickerFragment).hide(pickerFragment).commit()

        pickerFragment.close = {
            isOpen = false
            fragmentManager.beginTransaction().hide(pickerFragment).commit()
        }

        id_country_code.setOnClickListener {
            openPicker()
        }

        id_back.setOnClickListener {
            finish()
        }

        id_next.setOnClickListener {
            if (step == 1) {
                if (Constants.getPhone(id_phone_edit_text).isEmpty()) {
                    val toast = Toast.makeText(this, getString(R.string.alert_empty), Toast.LENGTH_LONG)
                    toast.show()
                } else {
                    id_next.visibility = View.GONE
                    id_next_progress_bar.visibility = View.VISIBLE

                    id_country_code.isEnabled = false
                    id_phone_edit_text.isEnabled = false

                    val auth = FirebaseAuth.getInstance()
                    val phone = id_country_code.text.toString() + Constants.getPhone(id_phone_edit_text)
                    val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(phone).setTimeout(60L, TimeUnit.SECONDS).setActivity(this).setCallbacks(callbacks).build()
                    PhoneAuthProvider.verifyPhoneNumber(options)

                    Log.e("Response", phone)
                }
            } else if (step == 2) {
                if (id_code_edit_text.text.toString().isEmpty()) {
                    val toast = Toast.makeText(this, getString(R.string.alert_empty), Toast.LENGTH_LONG)
                    toast.show()
                } else {
                    id_next.visibility = View.GONE
                    id_next_progress_bar.visibility = View.VISIBLE

                    id_code_edit_text.isEnabled = false

                    val code = id_code_edit_text.text.toString()
                    val credential = PhoneAuthProvider.getCredential(verificationID, code)
                    signInWithPhoneAuthCredential(credential)
                }
            } else {
                if (id_password_edit_text.text.toString().isEmpty() || id_confirm_edit_text.text.toString().isEmpty()) {
                    val toast = Toast.makeText(this, getString(R.string.alert_empty), Toast.LENGTH_LONG)
                    toast.show()
                } else if (id_password_edit_text.text.toString() != id_confirm_edit_text.text.toString()) {
                    val toast = Toast.makeText(this, getString(R.string.alert_both), Toast.LENGTH_LONG)
                    toast.show()
                } else {
                    id_next.visibility = View.GONE
                    id_next_progress_bar.visibility = View.VISIBLE
                    reset()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun openPicker() {
        pickerFragment.select = {
            index ->
            val jsonObject = pickerFragment.array[index]
            code = jsonObject.string1
            id_country_code.setText("+${code}")
        }
        pickerFragment.array.clear()

        val countries = Constants.getCountries()

        for (index in 0..(countries.size - 1)) {
            val jsonObject = countries[index]
            val pickerClass = PickerClass(jsonObject.string2, jsonObject.string1)
            pickerFragment.array.add(pickerClass)
        }
        pickerFragment.adapter.notifyDataSetChanged()

        isOpen = true
        fragmentManager.beginTransaction().show(pickerFragment).commit()
    }

    val callbacks = object: PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            next("")
        }

        override fun onVerificationFailed(e: FirebaseException) {
            id_country_code.isEnabled = true
            id_phone_edit_text.isEnabled = true

            id_next.visibility = View.VISIBLE
            id_next_progress_bar.visibility = View.GONE

            val toast = Toast.makeText(this@ForgotPasswordActivity, e.toString(), Toast.LENGTH_LONG)
            toast.show()

            Log.e("Response", e.toString())
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            next(verificationId)
        }
    }

    fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener(this) {
            task ->
            id_next.visibility = View.VISIBLE
            id_next_progress_bar.visibility = View.GONE

            if (task.isSuccessful) {
                setPasswordUI()
            } else {
                id_code_edit_text.isEnabled = true

                val toast = Toast.makeText(this, getString(R.string.alert_verification), Toast.LENGTH_LONG)
                toast.show()
            }
        }
    }

    fun next(verificationId: String) {
        id_next.visibility = View.VISIBLE
        id_next_progress_bar.visibility = View.GONE

        if (verificationId.isEmpty()) {
            setPasswordUI()
        } else {
            verificationID = verificationId
            setCodeUI()
        }
    }

    fun setCodeUI() {
        step = 2
        id_code_layout.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    fun setPasswordUI() {
        step = 3
        id_password_layout.visibility = View.VISIBLE
        id_confirm_layout.visibility = View.VISIBLE

        id_next.setText("Reset Password")
    }

    fun reset() {
        val url = Constants.url + "reset_password.php"
        val uid = Constants.getUID(this)

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    loginSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                loginError()
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["language"] = getString(R.string.language)
                params["phone"] = code + Constants.getPhone(id_phone_edit_text)
                params["password"] = id_password_edit_text.text.toString()
                params["uid"] = uid
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun loginError() {
        println("Error2")
        reset()
    }

    fun loginSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle(getString(R.string.alert_password_reset))
            alertDialog.setPositiveButton(getString(R.string.ok)) {
                _, _ ->
                finish()
            }
            val alert = alertDialog.create()
            alert.show()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_next.visibility = View.VISIBLE
        id_next_progress_bar.visibility = View.GONE
    }
}