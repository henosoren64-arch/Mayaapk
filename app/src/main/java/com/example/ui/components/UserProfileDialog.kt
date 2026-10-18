package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserProfileEntity
import com.example.ui.theme.DarkVioletSurface
import com.example.ui.theme.DeepGlassSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.MidnightObsidian
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun UserProfileDialog(
    profiles: List<UserProfileEntity>,
    onSelectProfile: (Long) -> Unit,
    onSaveProfile: (UserProfileEntity) -> Unit,
    onDeleteProfile: (UserProfileEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var editingProfile by remember { mutableStateOf<UserProfileEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("user_profile_dialog"),
            color = DarkVioletSurface,
            shadowElevation = 12.dp
        ) {
            if (editingProfile != null || isCreatingNew) {
                ProfileEditorContent(
                    initialProfile = editingProfile,
                    onSave = { updatedProfile ->
                        onSaveProfile(updatedProfile)
                        editingProfile = null
                        isCreatingNew = false
                    },
                    onCancel = {
                        editingProfile = null
                        isCreatingNew = false
                    }
                )
            } else {
                ProfileListContent(
                    profiles = profiles,
                    onSelectProfile = onSelectProfile,
                    onEditProfile = { profile -> editingProfile = profile },
                    onCreateNew = { isCreatingNew = true },
                    onDeleteProfile = onDeleteProfile,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ProfileListContent(
    profiles: List<UserProfileEntity>,
    onSelectProfile: (Long) -> Unit,
    onEditProfile: (UserProfileEntity) -> Unit,
    onCreateNew: () -> Unit,
    onDeleteProfile: (UserProfileEntity) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonMagenta.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profiles",
                        tint = NeonMagenta,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Mayra Profiles",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Influence her personality & callbacks",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_profiles_btn")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCreateNew,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_profile_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Profile", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                ProfileItemCard(
                    profile = profile,
                    onSelect = { onSelectProfile(profile.id) },
                    onEdit = { onEditProfile(profile) },
                    onDelete = { onDeleteProfile(profile) }
                )
            }
        }
    }
}

@Composable
private fun ProfileItemCard(
    profile: UserProfileEntity,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("profile_card_${profile.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (profile.isActive) DeepGlassSurface else MidnightObsidian
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (profile.isActive) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(NeonMagenta)
        ) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.profileName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (profile.isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StatusActive.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = StatusActive,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (!profile.isActive) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = StatusError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Calls you: \"${profile.userNickname}\" • Style: ${profile.sassLevel}",
                color = ElectricCyan,
                fontSize = 12.sp
            )

            if (profile.topicsOfInterest.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Interests: ${profile.topicsOfInterest}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            if (profile.customWittyRemarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Custom remarks: \"${profile.customWittyRemarks}\"",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ProfileEditorContent(
    initialProfile: UserProfileEntity?,
    onSave: (UserProfileEntity) -> Unit,
    onCancel: () -> Unit
) {
    var profileName by remember { mutableStateOf(initialProfile?.profileName ?: "") }
    var userNickname by remember { mutableStateOf(initialProfile?.userNickname ?: "Darling") }
    var sassLevel by remember { mutableStateOf(initialProfile?.sassLevel ?: "Playful & Sassy") }
    var topicsOfInterest by remember { mutableStateOf(initialProfile?.topicsOfInterest ?: "") }
    var commonPhrases by remember { mutableStateOf(initialProfile?.commonPhrases ?: "") }
    var customWittyRemarks by remember { mutableStateOf(initialProfile?.customWittyRemarks ?: "") }

    val sassOptions = listOf(
        "Playful & Sassy",
        "Sharp & Teasing",
        "Sweet & Confident",
        "Sarcastic Genius"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (initialProfile == null) "New Profile" else "Edit Profile",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name (e.g., Casual Bestie)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonMagenta,
                        unfocusedBorderColor = DeepGlassSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = userNickname,
                    onValueChange = { userNickname = it },
                    label = { Text("What Mayra should call you") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonMagenta,
                        unfocusedBorderColor = DeepGlassSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }

            item {
                Text(
                    text = "Personality & Sass Level",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sassOptions.take(2).forEach { option ->
                        FilterChip(
                            selected = sassLevel == option,
                            onClick = { sassLevel = option },
                            label = { Text(option, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonMagenta,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sassOptions.drop(2).forEach { option ->
                        FilterChip(
                            selected = sassLevel == option,
                            onClick = { sassLevel = option },
                            label = { Text(option, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonMagenta,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = topicsOfInterest,
                    onValueChange = { topicsOfInterest = it },
                    label = { Text("Topics of Interest (comma-separated)") },
                    placeholder = { Text("AI, Fitness, Espresso, Sci-fi") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = DeepGlassSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = commonPhrases,
                    onValueChange = { commonPhrases = it },
                    label = { Text("Common Phrases & Inside Slang") },
                    placeholder = { Text("Coffee first, Don't ask, Let's get it") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = DeepGlassSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = customWittyRemarks,
                    onValueChange = { customWittyRemarks = it },
                    label = { Text("Custom Witty Remarks / Teasing Lines") },
                    placeholder = { Text("You know you secretly adore me") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = DeepGlassSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    minLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val finalProfile = (initialProfile ?: UserProfileEntity(profileName = "")).copy(
                        profileName = if (profileName.isBlank()) "My Profile" else profileName.trim(),
                        userNickname = if (userNickname.isBlank()) "Darling" else userNickname.trim(),
                        sassLevel = sassLevel,
                        topicsOfInterest = topicsOfInterest.trim(),
                        commonPhrases = commonPhrases.trim(),
                        customWittyRemarks = customWittyRemarks.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(finalProfile)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Profile", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
