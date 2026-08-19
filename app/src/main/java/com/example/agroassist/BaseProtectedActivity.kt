package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * BaseProtectedActivity - Abstract base class for all protected AgroAssist Activities.
 * Enforces strict authentication checking on startup and resume.
 * Unauthenticated users are immediately redirected to LoginActivity.
 */
abstract class BaseProtectedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAuthenticationState()
    }

    override fun onResume() {
        super.onResume()
        checkAuthenticationState()
    }

    protected fun checkAuthenticationState(): Boolean {
        SessionManager.init(this)
        if (!SessionManager.isLoggedIn(this)) {
            val state = SessionManager.getAuthState(this)
            if (state == SessionManager.AuthState.SESSION_EXPIRED) {
                Toast.makeText(this, "Session expired. Please sign in again.", Toast.LENGTH_SHORT).show()
            }
            redirectToLogin()
            return false
        }
        return true
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
