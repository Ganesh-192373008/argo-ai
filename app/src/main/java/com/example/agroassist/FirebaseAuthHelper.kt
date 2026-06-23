package com.example.agroassist

import android.content.Context
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

object FirebaseAuthHelper {

    private var isFirebaseConfigured = false

    private fun getAuthInstance(context: Context): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            isFirebaseConfigured = true
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            isFirebaseConfigured = false
            null
        }
    }

    fun signIn(
        context: Context,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val auth = getAuthInstance(context)
        if (auth != null && isFirebaseConfigured) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        val errorMsg = task.exception?.localizedMessage ?: "Authentication failed"
                        onFailure(errorMsg)
                    }
                }
        } else {
            // Local fallback simulation when Firebase is unconfigured
            Toast.makeText(context, "Running in Offline/Local Mode (No Firebase)", Toast.LENGTH_SHORT).show()
            // Any email and 6+ character password works as standard failover
            if (email.contains("@") && password.length >= 6) {
                // Save this email in the database profile
                val dbHelper = AgroDatabaseHelper(context)
                dbHelper.saveProfile(email.substringBefore("@"), "25", "Tomato, Wheat")
                onSuccess()
            } else {
                onFailure("Invalid email format or password must be at least 6 characters.")
            }
        }
    }

    fun signUp(
        context: Context,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val auth = getAuthInstance(context)
        if (auth != null && isFirebaseConfigured) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        val errorMsg = task.exception?.localizedMessage ?: "Registration failed"
                        onFailure(errorMsg)
                    }
                }
        } else {
            Toast.makeText(context, "Running in Offline/Local Mode (No Firebase)", Toast.LENGTH_SHORT).show()
            if (email.contains("@") && password.length >= 6) {
                val dbHelper = AgroDatabaseHelper(context)
                dbHelper.saveProfile(email.substringBefore("@"), "25", "Tomato, Wheat")
                onSuccess()
            } else {
                onFailure("Invalid email or password.")
            }
        }
    }

    fun sendPasswordResetEmail(
        context: Context,
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val auth = getAuthInstance(context)
        if (auth != null && isFirebaseConfigured) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        val errorMsg = task.exception?.localizedMessage ?: "Failed to send reset link"
                        onFailure(errorMsg)
                    }
                }
        } else {
            Toast.makeText(context, "Running in Offline/Local Mode (No Firebase)", Toast.LENGTH_SHORT).show()
            if (email.contains("@")) {
                onSuccess()
            } else {
                onFailure("Please enter a valid email address.")
            }
        }
    }
}
