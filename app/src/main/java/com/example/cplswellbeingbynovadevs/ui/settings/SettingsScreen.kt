package com.example.cplswellbeingbynovadevs.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.cplswellbeingbynovadevs.data.remote.SupabaseClient

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()
    val isPauseSharing by viewModel.isPauseSharing.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val updateManifest by viewModel.updateManifest.collectAsState()

    val showChangeEmailDialog by viewModel.showChangeEmailDialog.collectAsState()
    val showChangePasswordDialog by viewModel.showChangePasswordDialog.collectAsState()
    val isUpdatingAccount by viewModel.isUpdatingAccount.collectAsState()
    val accountMessage by viewModel.accountMessage.collectAsState()

    val currentEmail = currentUserProfile?.email?.ifEmpty { SupabaseClient.currentUserEmail } ?: SupabaseClient.currentUserEmail ?: "user@example.com"
    val currentRole = currentUserProfile?.role ?: SupabaseClient.currentUserRole

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Text(
                    text = "Settings & Privacy",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Account Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Email: $currentEmail", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Role: $currentRole",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.openChangeEmailDialog() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Email")
                            }

                            OutlinedButton(
                                onClick = { viewModel.openChangePasswordDialog() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Pass")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Privacy Transparency",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isPauseSharing) "Sharing is PAUSED" else "Sharing is ACTIVE",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPauseSharing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                            Switch(
                                checked = !isPauseSharing,
                                onCheckedChange = { viewModel.togglePauseSharing() },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Shared with connected partner:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        PrivacyCheckRow(text = "App names & usage durations", isShared = true)
                        PrivacyCheckRow(text = "Number of sessions & daily rankings", isShared = true)
                        PrivacyCheckRow(text = "Battery percentage & charging status", isShared = true)
                        PrivacyCheckRow(text = "Online / active synchronization status", isShared = true)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "NOT shared (Private):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        PrivacyCheckRow(text = "Messages, chats & notification contents", isShared = false)
                        PrivacyCheckRow(text = "Passwords, keystrokes & screenshots", isShared = false)
                        PrivacyCheckRow(text = "Camera, microphone & contact files", isShared = false)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "CPL Wellbeing App", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isCheckingUpdate
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Check Update")
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        viewModel.signOut()
                        onSignOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        if (showChangeEmailDialog) {
            var newEmailInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.closeChangeEmailDialog() },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.submitNewEmail(newEmailInput) {
                                Toast.makeText(context, "Email updated successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isUpdatingAccount,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isUpdatingAccount) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Update Email")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeChangeEmailDialog() }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Change Account Email", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newEmailInput,
                            onValueChange = { newEmailInput = it },
                            label = { Text("New Email Address") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                        if (accountMessage != null) {
                            Text(text = accountMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                shape = RoundedCornerShape(26.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (showChangePasswordDialog) {
            var newPasswordInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.closeChangePasswordDialog() },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.submitNewPassword(newPasswordInput) {
                                Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isUpdatingAccount,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isUpdatingAccount) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Update Password")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeChangePasswordDialog() }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Change Password", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newPasswordInput,
                            onValueChange = { newPasswordInput = it },
                            label = { Text("New Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                        if (accountMessage != null) {
                            Text(text = accountMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                shape = RoundedCornerShape(26.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (showUpdateDialog && updateManifest != null) {
            AlertDialog(
                onDismissRequest = { if (!updateManifest!!.mandatory) viewModel.dismissUpdateDialog() },
                confirmButton = {
                    Button(
                        onClick = {
                            val apkUrl = updateManifest?.apkUrl ?: updateManifest?.releaseUrl ?: ""
                            if (apkUrl.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, apkUrl.toUri())
                                context.startActivity(intent)
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Update Now")
                    }
                },
                dismissButton = {
                    if (!updateManifest!!.mandatory) {
                        TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                            Text("Later")
                        }
                    }
                },
                title = { Text(updateManifest?.title ?: "App Update", fontWeight = FontWeight.Bold) },
                text = { Text(updateManifest?.message ?: "A new version of CPL Wellbeing is available.") },
                shape = RoundedCornerShape(26.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun PrivacyCheckRow(text: String, isShared: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isShared) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isShared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
