
package com.vastlb.wing_me.Launch

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.vastlb.wing_me.Classes.Constants
import com.vastlb.wing_me.Classes.Singleton
import com.vastlb.wing_me.DataClasses.PickerClass
import com.vastlb.wing_me.Main.MainActivity
import com.vastlb.wing_me.Main.PickerFragment
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Supabase.SupabaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class LoginActivity: AppCompatActivity() {

    val fragmentManager = supportFragmentManager
    val pickerFragment = PickerFragment.newInstance()

    val callbackManager = CallbackManager.Factory.create()

    var isOpen = false

    var code = "1"

    // Supabase phone auth is OTP-based (no password). This screen keeps its existing
    // phone + single text-field UI: tap 1 sends an OTP to the phone number, then the
    // same text field (originally the password field) is reused to collect the code
    // for tap 2. `otpSent` tracks which step we're on. `otpPhone` remembers the exact
    // phone string an OTP was sent to, so verify always targets the right number even
    // if the user edits the phone field after requesting a code.
    var otpSent = false
    var otpPhone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setViews()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
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

        id_login.setOnClickListener {
            if (!otpSent) {
                if (Constants.getPhone(id_phone_edit_text).isEmpty()) {
                    val toast = Toast.makeText(this, getString(R.string.alert_empty), Toast.LENGTH_LONG)
                    toast.show()
                } else {
                    id_login.visibility = View.GONE
                    id_login_progress_bar.visibility = View.VISIBLE
                    sendOtp()
                }
            } else {
                if (id_password_edit_text.text.toString().isEmpty()) {
                    val toast = Toast.makeText(this, getString(R.string.alert_empty), Toast.LENGTH_LONG)
                    toast.show()
                } else {
                    id_login.visibility = View.GONE
                    id_login_progress_bar.visibility = View.VISIBLE
                    verifyOtp()
                }
            }
        }

        id_forgot_password.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        id_facebook.setOnClickListener {
            id_facebook.visibility = View.GONE
            id_facebook_progress_bar.visibility = View.VISIBLE
            facebookLogin()
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

    fun sendOtp() {
        otpPhone = "+" + code + Constants.getPhone(id_phone_edit_text)

        SupabaseAuth.sendPhoneOtp(this, otpPhone, onSuccess = {
            otpSent = true
            id_password_edit_text.setText("")
            id_password_edit_text.hint = "Enter the code we sent you"
            id_login.visibility = View.VISIBLE
            id_login_progress_bar.visibility = View.GONE

            val toast = Toast.makeText(this, "Code sent to $otpPhone", Toast.LENGTH_LONG)
            toast.show()
        }, onError = { message ->
            id_login.visibility = View.VISIBLE
            id_login_progress_bar.visibility = View.GONE

            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        })
    }

    fun verifyOtp() {
        val code = id_password_edit_text.text.toString()

        SupabaseAuth.verifyPhoneOtp(this, otpPhone, code, onSuccess = { _, _ ->
            id_phone_edit_text.setText("")
            id_password_edit_text.setText("")

            openMain()
        }, onError = { message ->
            id_login.visibility = View.VISIBLE
            id_login_progress_bar.visibility = View.GONE

            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        })
    }

    fun facebookLogin() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"))

        LoginManager.getInstance().registerCallback(callbackManager, object: FacebookCallback<LoginResult> {

            override fun onSuccess(result: LoginResult) {
                val request = GraphRequest.newMeRequest(result.accessToken) {
                    jsonObject, response ->

                    try {
                        val id = jsonObject!!.getString("id")
                        facebookLogin(id)
                    } catch (e: Exception) {
                        id_facebook.visibility = View.VISIBLE
                        id_facebook_progress_bar.visibility = View.GONE

                        val toast = Toast.makeText(this@LoginActivity, e.toString(), Toast.LENGTH_LONG)
                        toast.show()
                    }
                }
                val parameters = Bundle()
                parameters.putString("fields", "id")
                request.parameters = parameters
                request.executeAsync()
            }

            override fun onCancel() {
                id_facebook.visibility = View.VISIBLE
                id_facebook_progress_bar.visibility = View.GONE

                val toast = Toast.makeText(this@LoginActivity, "Cancelled", Toast.LENGTH_LONG)
                toast.show()
            }

            override fun onError(error: FacebookException) {
                id_facebook.visibility = View.VISIBLE
                id_facebook_progress_bar.visibility = View.GONE

                val toast = Toast.makeText(this@LoginActivity, error.toString(), Toast.LENGTH_LONG)
                toast.show()
            }
        })
    }

    fun facebookLogin(id: String) {
        val url = Constants.url + "login_facebook.php"
        val uid = Constants.getUID(this)

        val request = object: StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    facebookSuccess(response)
                } catch (e: JSONException) {
                    val toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    toast.show()
                }
            },
            Response.ErrorListener {
                facebookError(id)
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["language"] = getString(R.string.language)
                params["facebook_Id"] = id
                params["uid"] = uid
                return params
            }
        }
        Singleton.getInstance(this).addToRequestQueue(request)
    }

    fun facebookError(id: String) {
        println("Error2")
        facebookLogin(id)
    }

    fun facebookSuccess(response: String) {
        val json = JSONObject(response)
        val error = json.getString("error")

        if (error == "0") {
            val message = json.getJSONObject("message")
            val token = message.getString("token")

            val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString("Token", token)
            editor.apply()

            id_phone_edit_text.setText("")
            id_password_edit_text.setText("")

            openMain()
        } else {
            val message = json.getString("message")
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
        }
        id_facebook.visibility = View.VISIBLE
        id_facebook_progress_bar.visibility = View.GONE
    }

    fun openMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("CLose", true)
        startActivity(intent)
    }
}