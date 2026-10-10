package com.example.cplswellbeingbynovadevs.ui.admin

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cplswellbeingbynovadevs.data.model.UserProfile

@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel
) {
    val context = LocalContext.current
    val usersList by viewModel.usersList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showCreateUserDialog by viewModel.showCreateUserDialog.collectAsState()
    val selectedUserForEdit by viewModel.selectedUserForEdit.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAllUsers()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Admin Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.openCreateUserDialog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create User Account", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Managed Users (${usersList.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (usersList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(usersList) { user ->
                        AdminUserCard(
                            user = user,
                            onEditClick = { viewModel.selectUserForEdit(user) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }

        if (showCreateUserDialog) {
            var newEmail by remember { mutableStateOf("") }
            var newPass by remember { mutableStateOf("") }
            var newName by remember { mutableStateOf("") }
            var newRole by remember { mutableStateOf(UserProfile.ROLE_USER) }

            AlertDialog(
                onDismissRequest = { viewModel.closeCreateUserDialog() },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createNewUser(newEmail, newPass, newName, newRole) {
                                Toast.makeText(context, "User account created!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeCreateUserDialog() }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Create New User", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = { Text("Email") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("Initial Password") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { newRole = UserProfile.ROLE_USER },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (newRole == UserProfile.ROLE_USER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) { Text("User") }
                            OutlinedButton(
                                onClick = { newRole = UserProfile.ROLE_ADMIN },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (newRole == UserProfile.ROLE_ADMIN) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) { Text("Admin") }
                        }
                        if (statusMessage != null) {
                            Text(text = statusMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                shape = RoundedCornerShape(26.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (selectedUserForEdit != null) {
            val user = selectedUserForEdit!!
            AlertDialog(
                onDismissRequest = { viewModel.selectUserForEdit(null) },
                confirmButton = {
                    TextButton(onClick = { viewModel.selectUserForEdit(null) }) {
                        Text("Close")
                    }
                },
                title = { Text("Manage ${user.displayName}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Email: ${user.email}", style = MaterialTheme.typography.bodyMedium)
                        Text("Role: ${user.role}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Status: ${user.accountStatus}", style = MaterialTheme.typography.bodyMedium)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val targetRole = if (user.role == UserProfile.ROLE_ADMIN) UserProfile.ROLE_USER else UserProfile.ROLE_ADMIN
                                    viewModel.updateUserRole(user, targetRole)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (user.role == UserProfile.ROLE_ADMIN) "Make User" else "Make Admin")
                            }

                            Button(
                                onClick = { viewModel.toggleUserAccountStatus(user) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.accountStatus == UserProfile.STATUS_ACTIVE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (user.accountStatus == UserProfile.STATUS_ACTIVE) "Disable" else "Enable")
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(26.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun AdminUserCard(
    user: UserProfile,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = user.displayName.ifEmpty { user.email.substringBefore("@") },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${user.email} • ${user.role}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Status: ${user.accountStatus}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (user.accountStatus == UserProfile.STATUS_ACTIVE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit User",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
