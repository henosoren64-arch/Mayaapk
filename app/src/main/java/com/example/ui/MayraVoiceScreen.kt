package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AssistantStatus
import com.example.model.ConnectionState
import com.example.ui.components.GlowingVoiceOrb
import com.example.ui.components.MemoryManagerDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.UserProfileDialog
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.DarkVioletSurface
import com.example.ui.theme.DeepGlassSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.MidnightObsidian
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.RoseGlow
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MayraVoiceScreen(
    viewModel: MayraViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Permissions check
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasMicPermission = results[Manifest.permission.RECORD_AUDIO] == true
        if (hasMicPermission) {
            viewModel.startSession(context)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mayra_voice_screen"),
        containerColor = MidnightObsidian
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MidnightObsidian,
                            DarkVioletSurface.copy(alpha = 0.85f),
                            MidnightObsidian
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar
                TopStatusAndControls(
                    uiState = uiState,
                    activeProfileName = activeProfile?.profileName ?: "Default",
                    userNickname = activeProfile?.userNickname ?: "Darling",
                    memoryCount = memories.size,
                    onOpenProfiles = { showProfileDialog = true },
                    onOpenMemories = { showMemoryDialog = true },
                    onOpenSettings = { showSettingsDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Orb & Waveform Center Piece
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Central Interactive Voice Orb
                        GlowingVoiceOrb(
                            assistantStatus = uiState.assistantStatus,
                            userAudioLevel = uiState.userAudioLevel,
                            mayraAudioLevel = uiState.mayraAudioLevel,
                            onClick = {
                                if (uiState.assistantStatus == AssistantStatus.Speaking) {
                                    viewModel.handleInterruption("Tapped glowing orb")
                                } else if (uiState.connectionState is ConnectionState.Disconnected) {
                                    if (hasMicPermission) {
                                        viewModel.startSession(context)
                                    } else {
                                        permissionLauncher.launch(permissionsToRequest)
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Waveform Spectrum Visualizer
                        WaveformVisualizer(
                            assistantStatus = uiState.assistantStatus,
                            userAudioLevel = uiState.userAudioLevel,
                            mayraAudioLevel = uiState.mayraAudioLevel,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                // Sassy Status & Dynamic Personality Caption
                SassyCaptionCard(
                    status = uiState.assistantStatus,
                    caption = uiState.sassyCaption,
                    interruptionCount = uiState.interruptionCount,
                    isMuted = uiState.isMicMuted,
                    onClickOrbHint = {
                        if (uiState.assistantStatus == AssistantStatus.Speaking) {
                            viewModel.handleInterruption("Tapped caption to interrupt")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Optional subtle live transcript snippet
                AnimatedVisibility(
                    visible = uiState.lastRecognizedText.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = DeepGlassSurface.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "“${uiState.lastRecognizedText}”",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Minimalist Voice Action Dock
                BottomVoiceControls(
                    uiState = uiState,
                    hasMicPermission = hasMicPermission,
                    onToggleMute = { viewModel.toggleMute() },
                    onInterrupt = { viewModel.handleInterruption("Bottom dock interrupt button") },
                    onToggleSession = {
                        if (uiState.connectionState is ConnectionState.Connected ||
                            uiState.connectionState is ConnectionState.Connecting
                        ) {
                            viewModel.stopSession(context)
                        } else {
                            if (hasMicPermission) {
                                viewModel.startSession(context)
                            } else {
                                permissionLauncher.launch(permissionsToRequest)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Background listening mode indicator
                if (uiState.isBackgroundRunning) {
                    Text(
                        text = "● Background active • Mayra listens while screen is locked",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Dialogs
    if (showProfileDialog) {
        UserProfileDialog(
            profiles = profiles,
            onSelectProfile = { profileId -> viewModel.selectActiveProfile(profileId) },
            onSaveProfile = { profile -> viewModel.saveProfile(profile) },
            onDeleteProfile = { profile -> viewModel.deleteProfile(profile) },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showMemoryDialog) {
        MemoryManagerDialog(
            memories = memories,
            onAddMemory = { topic, fact, cat -> viewModel.addMemory(topic, fact, cat) },
            onDeleteMemory = { memory -> viewModel.deleteMemory(memory) },
            onClearAll = { viewModel.clearAllMemories() },
            onDismiss = { showMemoryDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentApiKey = uiState.apiKey,
            currentModel = uiState.activeModel,
            currentVoice = uiState.activeVoice,
            isBackgroundEnabled = uiState.isBackgroundRunning,
            onSaveApiKey = { key -> viewModel.updateApiKey(key) },
            onSelectModel = { model -> viewModel.updateModel(model) },
            onSelectVoice = { voice -> viewModel.updateVoice(voice) },
            onToggleBackground = { viewModel.toggleBackgroundMode(context) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun TopStatusAndControls(
    uiState: com.example.model.MayraUiState,
    activeProfileName: String,
    userNickname: String,
    memoryCount: Int,
    onOpenProfiles: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity & Connection Status Pill
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MAYRA",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                val (statusColor, statusLabel) = when (uiState.connectionState) {
                    is ConnectionState.Connected -> StatusActive to "LIVE"
                    is ConnectionState.Connecting -> StatusWarning to "CONNECTING"
                    is ConnectionState.Error -> StatusError to "ERROR"
                    ConnectionState.Disconnected -> TextMuted to "OFFLINE"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = "Talking to $userNickname",
                color = ElectricCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Action Buttons: Profile, Memory, Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Profile selector chip
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenProfiles)
                    .testTag("top_profile_btn"),
                color = DeepGlassSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = NeonMagenta,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeProfileName,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Memory Bank button
            IconButton(
                onClick = onOpenMemories,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DeepGlassSurface)
                    .testTag("top_memories_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Memories",
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DeepGlassSurface)
                    .testTag("top_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SassyCaptionCard(
    status: AssistantStatus,
    caption: String,
    interruptionCount: Int,
    isMuted: Boolean,
    onClickOrbHint: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sassy_caption_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepGlassSurface.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusText = when (status) {
                    AssistantStatus.Speaking -> "MAYRA IS SPEAKING"
                    AssistantStatus.Listening -> if (isMuted) "MIC MUTED" else "LISTENING TO YOU"
                    AssistantStatus.Thinking -> "THINKING WITTY RETORT"
                    AssistantStatus.Interrupted -> "INTERRUPTED!"
                    AssistantStatus.Idle -> "READY FOR BANTER"
                }
                val badgeColor = when (status) {
                    AssistantStatus.Speaking -> NeonMagenta
                    AssistantStatus.Listening -> if (isMuted) StatusWarning else ElectricCyan
                    AssistantStatus.Thinking -> ElectricPurple
                    AssistantStatus.Interrupted -> StatusError
                    AssistantStatus.Idle -> RoseGlow
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (interruptionCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚡ $interruptionCount cut-offs",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "“$caption”",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            if (status == AssistantStatus.Speaking) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap orb or speak to interrupt anytime",
                    color = ElectricCyan.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.clickable(onClick = onClickOrbHint)
                )
            }
        }
    }
}

@Composable
private fun BottomVoiceControls(
    uiState: com.example.model.MayraUiState,
    hasMicPermission: Boolean,
    onToggleMute: () -> Unit,
    onInterrupt: () -> Unit,
    onToggleSession: () -> Unit
) {
    val isConnected = uiState.connectionState is ConnectionState.Connected
    val isConnecting = uiState.connectionState is ConnectionState.Connecting

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute / Unmute Button
        FilledIconButton(
            onClick = onToggleMute,
            enabled = isConnected,
            modifier = Modifier
                .size(54.dp)
                .testTag("mic_mute_toggle_btn"),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (uiState.isMicMuted) StatusWarning else DeepGlassSurface,
                contentColor = if (uiState.isMicMuted) MidnightObsidian else ElectricCyan
            )
        ) {
            Icon(
                imageVector = if (uiState.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (uiState.isMicMuted) "Unmute" else "Mute",
                modifier = Modifier.size(24.dp)
            )
        }

        // Main Live Connection Button
        FilledIconButton(
            onClick = onToggleSession,
            modifier = Modifier
                .size(68.dp)
                .testTag("live_session_toggle_btn"),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isConnected) StatusError else NeonMagenta,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CallEnd else Icons.Default.PowerSettingsNew,
                contentDescription = if (isConnected) "End Call" else "Start Live",
                modifier = Modifier.size(30.dp)
            )
        }

        // Instant Interrupt Button
        FilledIconButton(
            onClick = onInterrupt,
            enabled = isConnected,
            modifier = Modifier
                .size(54.dp)
                .testTag("interrupt_btn"),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (uiState.assistantStatus == AssistantStatus.Speaking) NeonMagenta.copy(alpha = 0.8f) else DeepGlassSurface,
                contentColor = if (uiState.assistantStatus == AssistantStatus.Speaking) Color.White else TextSecondary
            )
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = "Interrupt",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
