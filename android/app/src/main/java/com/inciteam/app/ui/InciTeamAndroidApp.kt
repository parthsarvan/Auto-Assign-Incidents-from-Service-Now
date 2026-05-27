package com.inciteam.app.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inciteam.app.data.AuthenticatedUser
import com.inciteam.app.push.AndroidPushNotificationService
import com.inciteam.app.push.IncidentNotificationDetail
import com.inciteam.app.session.SessionStore
import com.inciteam.app.ui.theme.InciTeamBackgroundBottom
import com.inciteam.app.ui.theme.InciTeamBackgroundTop
import com.inciteam.app.ui.theme.InciTeamBorder
import com.inciteam.app.ui.theme.InciTeamCard
import com.inciteam.app.ui.theme.InciTeamInk
import com.inciteam.app.ui.theme.InciTeamMuted
import com.inciteam.app.ui.theme.InciTeamPrimary
import com.inciteam.app.ui.theme.InciTeamPrimaryBright
import com.inciteam.app.ui.theme.InciTeamPrimaryDeep
import com.inciteam.app.ui.theme.InciTeamRow
import kotlinx.coroutines.launch

private const val SignUpUrl = "https://www.inciteam.com/signup"
private const val PrivacyUrl = "https://www.inciteam.com/privacy"

@Composable
fun InciTeamAndroidApp(
    sessionStore: SessionStore,
    openedIncidentNotification: IncidentNotificationDetail? = null,
    onCloseIncidentNotification: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var isSigningInScreenVisible by remember { mutableStateOf(false) }
    var selectedFeature by remember { mutableStateOf<InciTeamFeature?>(null) }
    val session = sessionStore.session

    LaunchedEffect(session?.user?.id) {
        if (session != null) {
            isSigningInScreenVisible = false
            selectedFeature = null
            runCatching {
                AndroidPushNotificationService.registerCurrentDevice(context, session.token)
            }
        }
    }

    when {
        session == null && isSigningInScreenVisible -> SignInScreen(
            sessionStore = sessionStore,
            onCancel = {
                sessionStore.clearError()
                isSigningInScreenVisible = false
            }
        )

        session == null -> LandingScreen(
            onSignIn = { isSigningInScreenVisible = true },
            onSignUp = { uriHandler.openUri(SignUpUrl) },
            onPrivacy = { uriHandler.openUri(PrivacyUrl) }
        )

        selectedFeature != null -> FeatureScreen(
            feature = selectedFeature!!,
            token = session.token,
            user = session.user,
            sessionStore = sessionStore,
            onBack = { selectedFeature = null }
        )

        else -> WelcomeScreen(
            sessionStore = sessionStore,
            user = session.user,
            onSignOut = {
                scope.launch {
                    runCatching {
                        AndroidPushNotificationService.unregisterCurrentDevice(context, session.token)
                    }
                    sessionStore.signOut()
                }
            },
            onFeatureSelected = { selectedFeature = it }
        )
    }

    openedIncidentNotification?.let { detail ->
        IncidentNotificationDetailDialog(
            detail = detail,
            onClose = onCloseIncidentNotification
        )
    }
}

@Composable
private fun IncidentNotificationDetailDialog(
    detail: IncidentNotificationDetail,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = InciTeamCard),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.88f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(InciTeamPrimaryBright, InciTeamPrimary, InciTeamPrimaryDeep)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Incident Assigned",
                            color = InciTeamPrimaryDeep,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = detail.incidentNumber,
                            color = InciTeamPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Outlined.Done,
                            contentDescription = null,
                            tint = InciTeamPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }

                IncidentNotificationRow(
                    title = "Incident",
                    value = detail.incidentNumber,
                    icon = Icons.Outlined.ListAlt
                )
                IncidentNotificationRow(
                    title = "Title",
                    value = detail.title,
                    icon = Icons.Outlined.FactCheck
                )
                IncidentNotificationRow(
                    title = "Priority",
                    value = detail.priority,
                    icon = Icons.Outlined.Flag
                )
                IncidentNotificationRow(
                    title = "Configuration Item",
                    value = detail.configurationItem,
                    icon = Icons.Outlined.Dns
                )
            }
        }
    }
}

@Composable
private fun IncidentNotificationRow(
    title: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(InciTeamRow)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(InciTeamPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InciTeamPrimary,
                modifier = Modifier.size(21.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                color = InciTeamMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = value.ifBlank { "-" },
                modifier = Modifier.padding(top = 4.dp),
                color = InciTeamInk,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun LandingScreen(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onPrivacy: () -> Unit
) {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(84.dp))
            LogoMark(size = 96.dp)
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "InciTeam",
                color = InciTeamInk,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Incident and Team Management platform for ServiceNow",
                modifier = Modifier.padding(top = 8.dp),
                color = InciTeamMuted,
                fontSize = 19.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(76.dp))
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text("Sign In", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSignUp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Sign Up", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onPrivacy, modifier = Modifier.padding(top = 6.dp)) {
                Text("Privacy Policy", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SignInScreen(
    sessionStore: SessionStore,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    BackHandler(onBack = onCancel)

    fun submit() {
        scope.launch {
            sessionStore.signIn(username = username, password = password)
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sign In",
                    modifier = Modifier.weight(1f),
                    color = InciTeamInk,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = InciTeamCard),
                border = BorderStroke(1.dp, InciTeamBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            sessionStore.clearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            sessionStore.clearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { submit() }
                        )
                    )

                    sessionStore.errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = Color(0xFFC74444),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { submit() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = username.trim().isNotEmpty() && password.isNotEmpty() && !sessionStore.isSigningIn,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (sessionStore.isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Sign In", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    sessionStore: SessionStore,
    user: AuthenticatedUser,
    onSignOut: () -> Unit,
    onFeatureSelected: (InciTeamFeature) -> Unit
) {
    AppBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                top = 16.dp,
                end = 18.dp,
                bottom = 34.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WelcomeHeader(
                    user = user,
                    profileImageData = sessionStore.profileImageData,
                    onProfileImageSelected = sessionStore::saveProfileImage,
                    onSignOut = onSignOut
                )
            }

            items(InciTeamFeatureSections) { section ->
                FeatureSectionCard(
                    section = section,
                    onFeatureSelected = onFeatureSelected
                )
            }
        }
    }
}

@Composable
private fun WelcomeHeader(
    user: AuthenticatedUser,
    profileImageData: ByteArray?,
    onProfileImageSelected: (ByteArray) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (data != null) {
            onProfileImageSelected(data)
        }
    }
    val workspace = user.workspace
    val teamRole = workspace?.teamRole?.takeIf { it.isNotBlank() }?.let(::friendlyBadgeText)
    val timezone = workspace?.teamTimezone?.takeIf { it.isNotBlank() }?.let(::compactTimezone)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(InciTeamPrimaryBright, InciTeamPrimary, InciTeamPrimaryDeep)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(30.dp))
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(
                    name = user.displayName,
                    imageData = profileImageData,
                    size = 74.dp,
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 18.dp)
                ) {
                    Text(
                        text = "Welcome, ${user.displayName}",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 34.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.workspaceLine,
                        modifier = Modifier.padding(top = 6.dp),
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onSignOut) {
                    Icon(
                        imageVector = Icons.Outlined.Logout,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoPill(title = user.role)
                if (teamRole != null) {
                    InfoPill(title = teamRole)
                }
                if (timezone != null) {
                    InfoPill(title = timezone)
                }
            }
        }
    }
}

@Composable
private fun FeatureSectionCard(
    section: InciTeamFeatureSection,
    onFeatureSelected: (InciTeamFeature) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = InciTeamCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.86f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionBadge(section.badge)
                Text(
                    text = section.title,
                    modifier = Modifier.padding(start = 10.dp),
                    color = InciTeamPrimaryDeep,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                section.features.forEach { feature ->
                    FeatureRow(feature = feature) {
                        onFeatureSelected(feature)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(feature: InciTeamFeature, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(InciTeamRow)
            .border(1.dp, InciTeamBorder.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(feature.tone.color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = featureIcon(feature.id),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = feature.title,
                color = InciTeamInk,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = feature.subtitle,
                color = InciTeamMuted,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = InciTeamMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun FeaturePlaceholderScreen(
    feature: InciTeamFeature?,
    onBack: () -> Unit
) {
    if (feature == null) {
        return
    }

    BackHandler(onBack = onBack)

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(20.dp)
        ) {
            TextButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("Back", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = InciTeamCard),
                border = BorderStroke(1.dp, InciTeamBorder)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(feature.tone.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = featureIcon(feature.id),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = feature.title,
                        color = InciTeamInk,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = feature.subtitle,
                        color = InciTeamMuted,
                        fontSize = 16.sp,
                        lineHeight = 23.sp
                    )
                    Text(
                        text = "This Android screen is next in the build queue. The iOS workflow already exists, and this app shell is ready for each feature to be brought over one by one.",
                        color = InciTeamMuted,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        InciTeamBackgroundTop,
                        Color(0xFFEFF6FF),
                        InciTeamBackgroundBottom
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
private fun LogoMark(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3f))
            .background(
                Brush.linearGradient(
                    colors = listOf(InciTeamPrimaryBright, InciTeamPrimary, InciTeamPrimaryDeep)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "IT",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ProfileAvatar(
    name: String,
    imageData: ByteArray?,
    size: androidx.compose.ui.unit.Dp = 82.dp,
    onClick: () -> Unit
) {
    val initials = name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "IT" }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = remember(imageData) {
            imageData?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(size * 0.37f)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = InciTeamPrimaryDeep,
                modifier = Modifier.size(size * 0.21f)
            )
        }
    }
}

@Composable
private fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = InciTeamInk,
    unfocusedTextColor = InciTeamInk,
    disabledTextColor = InciTeamMuted,
    focusedLabelColor = InciTeamPrimary,
    unfocusedLabelColor = InciTeamMuted,
    cursorColor = InciTeamPrimary,
    focusedBorderColor = InciTeamPrimary,
    unfocusedBorderColor = InciTeamBorder,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

@Composable
private fun InfoPill(title: String) {
    Surface(
        color = Color.White.copy(alpha = 0.18f),
        contentColor = Color.White,
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun friendlyBadgeText(value: String): String {
    return value
        .replace("_", " ")
        .lowercase()
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}

private fun compactTimezone(value: String): String {
    return value.substringAfterLast('/').replace("_", " ")
}

private fun featureIcon(featureId: String): ImageVector {
    return when (featureId) {
        "roster" -> Icons.Outlined.Groups
        "schedule" -> Icons.Outlined.CalendarMonth
        "team-members" -> Icons.Outlined.Person
        "leaves" -> Icons.Outlined.PersonOff
        "breaks" -> Icons.Outlined.Coffee
        "configuration-items" -> Icons.Outlined.Dns
        "ci-user-mapping" -> Icons.Outlined.Hub
        "summary" -> Icons.Outlined.Speed
        "logs" -> Icons.Outlined.ListAlt
        "diagnostics" -> Icons.Outlined.FactCheck
        "account" -> Icons.Outlined.AccountCircle
        "user-access" -> Icons.Outlined.AdminPanelSettings
        else -> Icons.Outlined.QueryStats
    }
}

@Composable
private fun SectionBadge(text: String) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(InciTeamPrimary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = InciTeamPrimaryDeep,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
