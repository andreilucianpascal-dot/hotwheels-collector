// app/src/main/java/com/example/hotwheelscollectors/ui/screens/settings/StorageSettingsScreen.kt
package com.example.hotwheelscollectors.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.foundation.layout.Arrangement
import com.example.hotwheelscollectors.R
import com.example.hotwheelscollectors.viewmodels.StorageSettingsViewModel
import com.example.hotwheelscollectors.ui.components.StorageProgressBar
import com.example.hotwheelscollectors.ui.components.SettingsItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.compose.ui.platform.LocalContext
import com.example.hotwheelscollectors.MainActivity

@Composable
fun StorageSettingsScreen(
    navController: NavController,
    viewModel: StorageSettingsViewModel = viewModel(),
    onGoogleSignIn: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(initialValue = StorageSettingsViewModel.UiState())
    val snackbarHostState = remember { SnackbarHostState() }

    var showLocationDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom TopAppBar using stable components
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.storage_settings),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    StorageUsageSection(
                        totalSpace = uiState.totalSpace,
                        usedSpace = uiState.usedSpace,
                        photoSpace = uiState.photoSpace,
                        databaseSpace = uiState.databaseSpace,
                        cacheSpace = uiState.cacheSpace
                    )
                }

                item {
                    SettingsSection(title = stringResource(R.string.storage_location)) {
                        SettingsItem(
                            title = stringResource(R.string.current_location),
                            subtitle = uiState.storageLocation,
                            icon = Icons.Default.Folder,
                            onClick = { showLocationDialog = true }
                        )

                        val context = LocalContext.current
                        val signedInAccount = remember { GoogleSignIn.getLastSignedInAccount(context) }
                        if (signedInAccount == null) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = onGoogleSignIn
                            ) {
                                Text("Connect Google Drive")
                            }
                        }

                        SettingsItem(
                            title = stringResource(R.string.auto_backup),
                            subtitle = stringResource(R.string.auto_backup_description),
                            icon = Icons.Default.Backup,
                            onClick = { showBackupDialog = true },
                            trailing = {
                                Switch(
                                    checked = uiState.autoBackupEnabled,
                                    onCheckedChange = { viewModel.toggleAutoBackup() }
                                )
                            }
                        )
                    }
                }

                item {
                    SettingsSection(title = stringResource(R.string.storage_management)) {
                        SettingsItem(
                            title = stringResource(R.string.compress_photos),
                            subtitle = stringResource(R.string.compress_photos_description),
                            icon = Icons.Default.Image,
                            onClick = { showCompressDialog = true }
                        )

                        SettingsItem(
                            title = stringResource(R.string.clear_cache),
                            subtitle = stringResource(
                                R.string.cache_size,
                                uiState.cacheSpace.formatSize()
                            ),
                            icon = Icons.Default.ClearAll,
                            onClick = { showClearCacheDialog = true }
                        )
                    }
                }
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showLocationDialog) {
        StorageLocationDialog(
            currentLocation = uiState.storageLocation,
            onLocationSelected = { location ->
                viewModel.updateStorageLocation(location)
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache)) },
            text = { Text(stringResource(R.string.clear_cache_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showCompressDialog) {
        AlertDialog(
            onDismissRequest = { showCompressDialog = false },
            title = { Text(stringResource(R.string.compress_photos)) },
            text = { Text(stringResource(R.string.compress_photos_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.compressPhotos()
                        showCompressDialog = false
                    }
                ) {
                    Text(stringResource(R.string.compress))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompressDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup Collection") },
            text = { Text("All collection data (including photos and details) is backed up automatically according to your chosen storage location. No manual backup is required. You can manually trigger a sync from the main menu.") },
            confirmButton = {
                TextButton(
                    onClick = { showBackupDialog = false }
                ) { Text("OK") }
            }
        )
    }
}

@Composable
private fun StorageUsageSection(
    totalSpace: Long,
    usedSpace: Long,
    photoSpace: Long,
    databaseSpace: Long,
    cacheSpace: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.storage_usage),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        StorageProgressBar(
            totalSpace = totalSpace,
            usedSpace = usedSpace,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        StorageBreakdown(
            photoSpace = photoSpace,
            databaseSpace = databaseSpace,
            cacheSpace = cacheSpace
        )
    }
}

@Composable
private fun StorageBreakdown(
    photoSpace: Long,
    databaseSpace: Long,
    cacheSpace: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StorageItem(
            label = stringResource(R.string.photos),
            size = photoSpace,
            color = MaterialTheme.colorScheme.primary
        )

        StorageItem(
            label = stringResource(R.string.database),
            size = databaseSpace,
            color = MaterialTheme.colorScheme.secondary
        )

        StorageItem(
            label = stringResource(R.string.cache),
            size = cacheSpace,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun StorageItem(
    label: String,
    size: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Text(text = label)
        }
        Text(
            text = size.formatSize(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StorageLocationDialog(
    currentLocation: String,
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val locations = listOf(
        "Internal Storage (On Device)" to "Internal",
        "Google Drive (Your Cloud)" to "Google Drive",
        "OneDrive (Your Cloud)" to "OneDrive",
        "Dropbox (Your Cloud)" to "Dropbox"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Storage Location") },
        text = {
            Column {
                locations.forEach { (name, id) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { if (id == "Google Drive") Text("Save to your own Google account drive") else if (id == "OneDrive") Text("Save to your own Microsoft OneDrive account") else if (id == "Dropbox") Text("Save to your own Dropbox account") else null },
                        leadingContent = {
                            RadioButton(
                                selected = id == currentLocation,
                                onClick = { onLocationSelected(id) }
                            )
                        },
                        modifier = Modifier.clickable { onLocationSelected(id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

private fun Long.formatSize(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = this.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return "%.1f %s".format(size, units[unitIndex])
}