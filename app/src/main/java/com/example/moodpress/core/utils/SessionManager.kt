package com.example.moodpress.core.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.SharedPreferences

@Suppress("DEPRECATION")
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private companion object {
        const val PREFS_NAME = "mood_press_prefs"
        const val KEY_USER_ID = "USER_ID"
        const val KEY_USER_NAME = "USER_NAME"
        const val KEY_USER_PICTURE = "USER_PICTURE"
        const val KEY_NOTIF_ENABLED = "NOTIF_ENABLED"
        const val KEY_NOTIF_HOUR = "NOTIF_HOUR"
        const val KEY_NOTIF_MINUTE = "NOTIF_MINUTE"
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveUserId(id: String) {
        sharedPrefs.edit {
            putString(KEY_USER_ID, id)
        }
    }

    fun fetchUserId(): String? {
        return sharedPrefs.getString(KEY_USER_ID, null)
    }

    fun saveUserName(name: String) {
        sharedPrefs.edit {
            putString(KEY_USER_NAME, name)
        }
    }

    fun fetchUserName(): String? {
        return sharedPrefs.getString(KEY_USER_NAME, null)
    }

    fun saveUserPicture(pictureUrl: String?) {
        sharedPrefs.edit {
            putString(KEY_USER_PICTURE, pictureUrl)
        }
    }

    fun fetchUserPicture(): String? {
        return sharedPrefs.getString(KEY_USER_PICTURE, null)
    }

    fun clearSession() {
        sharedPrefs.edit {
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_PICTURE)
        }
    }

    fun saveNotificationSettings(isEnabled: Boolean, hour: Int, minute: Int) {
        sharedPrefs.edit {
            putBoolean(KEY_NOTIF_ENABLED, isEnabled)
                .putInt(KEY_NOTIF_HOUR, hour)
                .putInt(KEY_NOTIF_MINUTE, minute)
        }
    }

    fun isNotificationEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_NOTIF_ENABLED, false)
    }

    fun getNotificationTime(): Pair<Int, Int> {
        val hour = sharedPrefs.getInt(KEY_NOTIF_HOUR, 20)
        val minute = sharedPrefs.getInt(KEY_NOTIF_MINUTE, 0)
        return Pair(hour, minute)
    }
}