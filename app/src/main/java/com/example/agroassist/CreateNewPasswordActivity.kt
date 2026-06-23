package com.example.agroassist

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CreateNewPasswordActivity : AppCompatActivity() {

    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_password)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val newPasswordInput = findViewById<EditText>(R.id.newPasswordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)
        val toggleNewPassword = findViewById<ImageView>(R.id.toggleNewPassword)
        val toggleConfirmPassword = findViewById<ImageView>(R.id.toggleConfirmPassword)
        val resetPasswordButton = findViewById<Button>(R.id.resetPasswordButton)

        val reqLengthDot = findViewById<ImageView>(R.id.reqLengthDot)
        val reqUpperDot = findViewById<ImageView>(R.id.reqUpperDot)
        val reqLowerDot = findViewById<ImageView>(R.id.reqLowerDot)
        val reqNumberDot = findViewById<ImageView>(R.id.reqNumberDot)

        val newPasswordContainer = findViewById<LinearLayout>(R.id.newPasswordContainer)
        val confirmPasswordContainer = findViewById<LinearLayout>(R.id.confirmPasswordContainer)

        backButton.setOnClickListener { finish() }

        toggleNewPassword.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            newPasswordInput.inputType = if (isNewPasswordVisible) 
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD 
            else 
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleNewPassword.alpha = if (isNewPasswordVisible) 0.5f else 1.0f
            newPasswordInput.setSelection(newPasswordInput.text.length)
        }

        toggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            confirmPasswordInput.inputType = if (isConfirmPasswordVisible) 
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD 
            else 
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleConfirmPassword.alpha = if (isConfirmPasswordVisible) 0.5f else 1.0f
            confirmPasswordInput.setSelection(confirmPasswordInput.text.length)
        }

        resetPasswordButton.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = newPasswordInput.text.toString()
                val confirmPass = confirmPasswordInput.text.toString()

                val hasLength = pass.length >= 8
                val hasUpper = pass.any { it.isUpperCase() }
                val hasLower = pass.any { it.isLowerCase() }
                val hasNumber = pass.any { it.isDigit() }

                val activeTint = ColorStateList.valueOf(resources.getColor(R.color.primary_green, theme))
                val inactiveTint = ColorStateList.valueOf(Color.parseColor("#D6D9E0"))

                reqLengthDot.imageTintList = if (hasLength) activeTint else inactiveTint
                reqUpperDot.imageTintList = if (hasUpper) activeTint else inactiveTint
                reqLowerDot.imageTintList = if (hasLower) activeTint else inactiveTint
                reqNumberDot.imageTintList = if (hasNumber) activeTint else inactiveTint

                val isAllReqsMet = hasLength && hasUpper && hasLower && hasNumber
                val doPasswordsMatch = pass == confirmPass && pass.isNotEmpty()

                if (isAllReqsMet && doPasswordsMatch) {
                    resetPasswordButton.setBackgroundColor(resources.getColor(R.color.primary_green, theme))
                    resetPasswordButton.isEnabled = true
                } else {
                    resetPasswordButton.setBackgroundColor(Color.parseColor("#D6D9E0"))
                    resetPasswordButton.isEnabled = false
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        newPasswordInput.addTextChangedListener(textWatcher)
        confirmPasswordInput.addTextChangedListener(textWatcher)

        newPasswordInput.setOnFocusChangeListener { _, hasFocus ->
            newPasswordContainer.setBackgroundResource(if (hasFocus) R.drawable.edit_text_bg_active else R.drawable.edit_text_bg)
        }
        confirmPasswordInput.setOnFocusChangeListener { _, hasFocus ->
            confirmPasswordContainer.setBackgroundResource(if (hasFocus) R.drawable.edit_text_bg_active else R.drawable.edit_text_bg)
        }

        resetPasswordButton.setOnClickListener {
            Toast.makeText(this, "Password Reset Successfully!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PasswordSuccessActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
