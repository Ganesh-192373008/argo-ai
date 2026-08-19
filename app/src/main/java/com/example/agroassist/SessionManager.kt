package com.example.agroassist

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SessionManager - Single Source of Truth for AgroAssist Authentication & Secure Token Storage.
 * Uses Android Keystore-backed EncryptedSharedPreferences for storing JWT access tokens.
 */
object SessionManager {

    private const val TAG = "SessionManager"
    private const val PREF_NAME = "AgroAssistSecureSession"

    private const val KEY_AUTH_TOKEN = "auth_jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_AUTH_STATE = "auth_state"

    enum class AuthState {
        UNAUTHENTICATED,
        AUTHENTICATING,
        AUTHENTICATED,
        SESSION_EXPIRED
    }

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = try {
                val masterKey = MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context.applicationContext,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.e(TAG, "EncryptedSharedPreferences failed, falling back to private preferences: ${e.message}")
                context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null) {
            init(context)
        }
        return prefs!!
    }

    /**
     * Save authenticated user session with JWT token
     */
    fun saveSession(context: Context, token: String, userId: Long, email: String, name: String) {
        val sp = getPrefs(context)
        sp.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_AUTH_STATE, AuthState.AUTHENTICATED.name)
            apply()
        }
        
        // Synchronize with SQLite db helper
        val dbHelper = AgroDatabaseHelper(context)
        dbHelper.saveProfile(name, "30", "Wheat, Rice")
        
        // Sync legacy SharedPreferences keys securely
        context.getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE).edit().apply {
            putString("email_address", email)
            putString("user_name", name)
            apply()
        }
    }

    /**
     * Get active JWT Auth Token
     */
    fun getToken(context: Context): String? {
        val token = getPrefs(context).getString(KEY_AUTH_TOKEN, null)
        return if (!token.isNullOrEmpty()) token else null
    }

    /**
     * Check whether user has a valid authenticated session token
     */
    fun isLoggedIn(context: Context): Boolean {
        val token = getToken(context)
        val stateStr = getPrefs(context).getString(KEY_AUTH_STATE, AuthState.UNAUTHENTICATED.name)
        return !token.isNullOrEmpty() && stateStr == AuthState.AUTHENTICATED.name
    }

    fun getAuthState(context: Context): AuthState {
        val stateStr = getPrefs(context).getString(KEY_AUTH_STATE, AuthState.UNAUTHENTICATED.name)
        return try {
            AuthState.valueOf(stateStr ?: AuthState.UNAUTHENTICATED.name)
        } catch (e: Exception) {
            AuthState.UNAUTHENTICATED
        }
    }

    fun getUserEmail(context: Context): String {
        return getPrefs(context).getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "") ?: ""
    }

    fun getUserId(context: Context): Long {
        return getPrefs(context).getLong(KEY_USER_ID, -1L)
    }

    fun markSessionExpired(context: Context) {
        val sp = getPrefs(context)
        sp.edit().apply {
            putString(KEY_AUTH_STATE, AuthState.SESSION_EXPIRED.name)
            remove(KEY_AUTH_TOKEN)
            apply()
        }
    }

    /**
     * Clear session token, auth state, and clear legacy credentials on logout
     */
    fun clearSession(context: Context) {
        val sp = getPrefs(context)
        sp.edit().clear().apply()

        // Clear local database profile
        val dbHelper = AgroDatabaseHelper(context)
        dbHelper.saveProfile("", "", "")

        // Clear settings SharedPreferences
        context.getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE).edit().apply {
            remove("email_address")
            remove("user_name")
            remove("registered_email")
            remove("registered_password")
            apply()
        }
    }
}
