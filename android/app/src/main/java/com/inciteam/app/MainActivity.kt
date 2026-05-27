package com.inciteam.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.inciteam.app.push.AndroidPushNotificationService
import com.inciteam.app.push.IncidentNotificationDetail
import com.inciteam.app.session.SessionStore
import com.inciteam.app.ui.InciTeamAndroidApp
import com.inciteam.app.ui.theme.InciTeamTheme

class MainActivity : ComponentActivity() {
    private val sessionStore by lazy {
        SessionStore(applicationContext)
    }
    private var openedIncidentNotification by mutableStateOf<IncidentNotificationDetail?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AndroidPushNotificationService.configureFirebase(applicationContext)
        openedIncidentNotification = IncidentNotificationDetail.fromIntent(intent)
        requestNotificationPermissionIfNeeded()
        setContent {
            InciTeamTheme(darkTheme = false) {
                InciTeamAndroidApp(
                    sessionStore = sessionStore,
                    openedIncidentNotification = openedIncidentNotification,
                    onCloseIncidentNotification = { openedIncidentNotification = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        IncidentNotificationDetail.fromIntent(intent)?.let {
            openedIncidentNotification = it
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }
}
