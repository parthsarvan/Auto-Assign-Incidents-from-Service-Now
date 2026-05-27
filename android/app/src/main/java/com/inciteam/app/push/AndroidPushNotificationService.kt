package com.inciteam.app.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.inciteam.app.BuildConfig
import com.inciteam.app.MainActivity
import com.inciteam.app.R
import com.inciteam.app.network.InciTeamApiClient
import com.inciteam.app.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object AndroidPushNotificationService {
    private const val CHANNEL_ID = "incident_assignments"
    private const val NOTIFICATION_ID_BASE = 42_000

    fun isConfigured(): Boolean =
        BuildConfig.INCITEAM_FIREBASE_PROJECT_ID.isNotBlank()
            && BuildConfig.INCITEAM_FIREBASE_APP_ID.isNotBlank()
            && BuildConfig.INCITEAM_FIREBASE_API_KEY.isNotBlank()
            && BuildConfig.INCITEAM_FIREBASE_SENDER_ID.isNotBlank()

    fun configureFirebase(context: Context): Boolean {
        if (!isConfigured()) {
            return false
        }
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            return true
        }
        val options = FirebaseOptions.Builder()
            .setProjectId(BuildConfig.INCITEAM_FIREBASE_PROJECT_ID)
            .setApplicationId(BuildConfig.INCITEAM_FIREBASE_APP_ID)
            .setApiKey(BuildConfig.INCITEAM_FIREBASE_API_KEY)
            .setGcmSenderId(BuildConfig.INCITEAM_FIREBASE_SENDER_ID)
            .build()
        FirebaseApp.initializeApp(context.applicationContext, options)
        return true
    }

    suspend fun registerCurrentDevice(context: Context, authToken: String, apiClient: InciTeamApiClient = InciTeamApiClient()) {
        if (!configureFirebase(context)) {
            return
        }
        val deviceToken = FirebaseMessaging.getInstance().token.await()
        apiClient.registerMobileDeviceToken(authToken, deviceToken, environment())
    }

    suspend fun unregisterCurrentDevice(context: Context, authToken: String, apiClient: InciTeamApiClient = InciTeamApiClient()) {
        if (!configureFirebase(context)) {
            return
        }
        val deviceToken = FirebaseMessaging.getInstance().token.await()
        apiClient.unregisterMobileDeviceToken(authToken, deviceToken, environment())
    }

    fun showIncidentAssigned(context: Context, data: Map<String, String>) {
        createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val incidentNumber = data["incidentNumber"].orEmpty().ifBlank { "Incident" }
        val incidentTitle = data["title"].orEmpty().ifBlank { "Incident assigned to you" }
        val priority = data["priority"].orEmpty().ifBlank { "Not provided" }
        val configurationItem = data["ci"].orEmpty().ifBlank { data["configurationItem"].orEmpty() }.ifBlank { "Not provided" }
        val body = "$incidentNumber assigned to you"
        val detail = """
            Incident: $incidentNumber
            Title: $incidentTitle
            Priority: $priority
            CI: $configurationItem
        """.trimIndent()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("incidentNumber", incidentNumber)
            putExtra("title", incidentTitle)
            putExtra("priority", priority)
            putExtra("ci", configurationItem)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            incidentNumber.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incident Assigned")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID_BASE + incidentNumber.hashCode().mod(10_000),
            notification
        )
    }

    fun environment(): String = if (BuildConfig.DEBUG) "development" else "production"

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Incident assignments",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications when InciTeam assigns an incident to you."
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}

class InciTeamFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AndroidPushNotificationService.configureFirebase(applicationContext)
    }

    override fun onNewToken(token: String) {
        val session = SessionStore(applicationContext).session ?: return
        serviceScope.launch {
            runCatching {
                InciTeamApiClient().registerMobileDeviceToken(
                    token = session.token,
                    deviceToken = token,
                    environment = AndroidPushNotificationService.environment()
                )
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data.toMutableMap()
        message.notification?.title?.takeIf { it.isNotBlank() }?.let { data.putIfAbsent("notificationTitle", it) }
        AndroidPushNotificationService.showIncidentAssigned(applicationContext, data)
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: IllegalStateException("Firebase task failed."))
        }
    }
}
