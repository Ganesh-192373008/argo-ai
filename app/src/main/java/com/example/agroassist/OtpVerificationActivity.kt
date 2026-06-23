package com.example.agroassist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var otp1: EditText
    private lateinit var otp2: EditText
    private lateinit var otp3: EditText
    private lateinit var otp4: EditText
    private lateinit var otp5: EditText
    private lateinit var otp6: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        val resendText = findViewById<TextView>(R.id.resendText)
        resendText.setOnClickListener {
            Toast.makeText(this, "OTP Resent!", Toast.LENGTH_SHORT).show()
        }

        otp1 = findViewById(R.id.otp1)
        otp2 = findViewById(R.id.otp2)
        otp3 = findViewById(R.id.otp3)
        otp4 = findViewById(R.id.otp4)
        otp5 = findViewById(R.id.otp5)
        otp6 = findViewById(R.id.otp6)

        setupOtpInputs()

        val receivedOtp = intent.getStringExtra("OTP_CODE")
        if (!receivedOtp.isNullOrEmpty()) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (receivedOtp.length >= 6) {
                    otp1.setText(receivedOtp[0].toString())
                    otp2.setText(receivedOtp[1].toString())
                    otp3.setText(receivedOtp[2].toString())
                    otp4.setText(receivedOtp[3].toString())
                    otp5.setText(receivedOtp[4].toString())
                    otp6.setText(receivedOtp[5].toString())
                    otp6.requestFocus()
                    verifyOtp()
                }
            }, 1500)
        }
    }

    private fun setupOtpInputs() {
        val editTexts = arrayOf(otp1, otp2, otp3, otp4, otp5, otp6)

        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        if (i < editTexts.size - 1) {
                            editTexts[i + 1].requestFocus()
                        } else {
                            // Last OTP digit entered
                            verifyOtp()
                        }
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            editTexts[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editTexts[i].text.isEmpty() && i > 0) {
                        editTexts[i - 1].requestFocus()
                        editTexts[i - 1].setText("")
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    private fun verifyOtp() {
        val otp = "${otp1.text}${otp2.text}${otp3.text}${otp4.text}${otp5.text}${otp6.text}"
        Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
        
        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        
        val intent = if (profile["name"]?.isNotEmpty() == true) {
            android.content.Intent(this, DashboardActivity::class.java)
        } else {
            android.content.Intent(this, ProfileSetupActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
