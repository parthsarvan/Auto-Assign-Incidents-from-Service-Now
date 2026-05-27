package com.inciteam.app.session

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.inciteam.app.data.AuthenticatedUser
import com.inciteam.app.data.StoredSession
import com.inciteam.app.data.WorkspaceSummary
import com.inciteam.app.network.InciTeamApiClient
import org.json.JSONObject

class SessionStore(
    context: Context,
    private val apiClient: InciTeamApiClient = InciTeamApiClient()
) {
    private val repository = SessionRepository(context.applicationContext)
    private val profileImageStorage = ProfileImageStorage(context.applicationContext)

    var session by mutableStateOf(repository.loadSession())
        private set

    var isSigningIn by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var profileImageData by mutableStateOf(session?.user?.id?.let(profileImageStorage::loadImage))
        private set

    val isSignedIn: Boolean
        get() = session != null

    suspend fun signIn(username: String, password: String): Boolean {
        val normalizedUsername = username.trim()
        if (normalizedUsername.isEmpty() || password.isEmpty()) {
            errorMessage = "Enter your username and password."
            return false
        }

        isSigningIn = true
        errorMessage = null
        return try {
            val response = apiClient.signIn(normalizedUsername, password)
            val storedSession = StoredSession(
                token = response.token,
                user = AuthenticatedUser.from(response)
            )
            repository.saveSession(storedSession)
            session = storedSession
            profileImageData = profileImageStorage.loadImage(storedSession.user.id)
            true
        } catch (error: Exception) {
            errorMessage = error.message ?: "Sign in failed."
            false
        } finally {
            isSigningIn = false
        }
    }

    fun saveProfileImage(data: ByteArray) {
        val userId = session?.user?.id ?: return
        profileImageData = profileImageStorage.saveImage(userId, data)
    }

    fun signOut() {
        repository.clearSession()
        session = null
        profileImageData = null
        errorMessage = null
    }

    fun clearError() {
        errorMessage = null
    }
}

private class SessionRepository(context: Context) {
    private val preferences = context.getSharedPreferences("inciteam_session", Context.MODE_PRIVATE)

    fun loadSession(): StoredSession? {
        val token = preferences.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        if (Jwt.isExpired(token)) {
            clearSession()
            return null
        }

        return StoredSession(
            token = token,
            user = AuthenticatedUser(
                id = preferences.getLong(KEY_USER_ID, 0L),
                username = preferences.getString(KEY_USERNAME, null).orEmpty(),
                firstName = preferences.getString(KEY_FIRST_NAME, null),
                lastName = preferences.getString(KEY_LAST_NAME, null),
                workEmail = preferences.getString(KEY_WORK_EMAIL, null),
                role = preferences.getString(KEY_ROLE, null) ?: "User",
                workspace = WorkspaceSummary(
                    organizationId = preferences.getNullableLong(KEY_ORG_ID),
                    organizationName = preferences.getString(KEY_ORG_NAME, null),
                    teamId = preferences.getNullableLong(KEY_TEAM_ID),
                    teamName = preferences.getString(KEY_TEAM_NAME, null),
                    teamRole = preferences.getString(KEY_TEAM_ROLE, null),
                    teamTimezone = preferences.getString(KEY_TEAM_TIMEZONE, null)
                )
            )
        )
    }

    fun saveSession(session: StoredSession) {
        val user = session.user
        val workspace = user.workspace
        preferences.edit()
            .putString(KEY_TOKEN, session.token)
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_FIRST_NAME, user.firstName)
            .putString(KEY_LAST_NAME, user.lastName)
            .putString(KEY_WORK_EMAIL, user.workEmail)
            .putString(KEY_ROLE, user.role)
            .putNullableLong(KEY_ORG_ID, workspace?.organizationId)
            .putString(KEY_ORG_NAME, workspace?.organizationName)
            .putNullableLong(KEY_TEAM_ID, workspace?.teamId)
            .putString(KEY_TEAM_NAME, workspace?.teamName)
            .putString(KEY_TEAM_ROLE, workspace?.teamRole)
            .putString(KEY_TEAM_TIMEZONE, workspace?.teamTimezone)
            .apply()
    }

    fun clearSession() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_LAST_NAME = "last_name"
        const val KEY_WORK_EMAIL = "work_email"
        const val KEY_ROLE = "role"
        const val KEY_ORG_ID = "organization_id"
        const val KEY_ORG_NAME = "organization_name"
        const val KEY_TEAM_ID = "team_id"
        const val KEY_TEAM_NAME = "team_name"
        const val KEY_TEAM_ROLE = "team_role"
        const val KEY_TEAM_TIMEZONE = "team_timezone"
    }
}

private object Jwt {
    fun isExpired(token: String): Boolean {
        val expirationSeconds = expirationSeconds(token) ?: return false
        val nowSeconds = System.currentTimeMillis() / 1000
        return expirationSeconds <= nowSeconds + 30
    }

    private fun expirationSeconds(token: String): Long? {
        val payload = token.split(".").getOrNull(1) ?: return null
        return try {
            val decoded = Base64.decode(
                payload,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            JSONObject(String(decoded, Charsets.UTF_8)).optLong("exp").takeIf { it > 0L }
        } catch (_: Exception) {
            null
        }
    }
}

private fun android.content.SharedPreferences.getNullableLong(key: String): Long? {
    return if (contains(key)) getLong(key, 0L) else null
}

private fun android.content.SharedPreferences.Editor.putNullableLong(
    key: String,
    value: Long?
): android.content.SharedPreferences.Editor {
    return if (value == null) remove(key) else putLong(key, value)
}
