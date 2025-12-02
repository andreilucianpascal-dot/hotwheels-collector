package com.example.hotwheelscollectors.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hotwheelscollectors.R
import com.example.hotwheelscollectors.ui.components.SettingsItem
import com.example.hotwheelscollectors.viewmodels.AboutViewModel

@Composable
fun AboutScreen(
    navController: NavController,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                AppHeader(
                    version = uiState.appVersion,
                    buildNumber = uiState.buildNumber
                )
            }

            item {
                SettingsSection(title = "App Info") {
                    SettingsItem(
                        title = "Check Updates",
                        icon = Icons.Default.Update,
                        onClick = viewModel::checkForUpdates
                    )

                    SettingsItem(
                        title = "What's New",
                        icon = Icons.Default.NewReleases,
                        onClick = { navController.navigate("whats_new") }
                    )

                    SettingsItem(
                        title = "Changelog",
                        icon = Icons.Default.History,
                        onClick = { navController.navigate("changelog") }
                    )
                }
            }

            item {
                SettingsSection(title = "Legal") {
                    SettingsItem(
                        title = "Privacy Policy",
                        icon = Icons.Default.Security,
                        onClick = { navController.navigate("privacy") }
                    )

                    SettingsItem(
                        title = "Terms of Service",
                        icon = Icons.Default.Description,
                        onClick = { navController.navigate("terms") }
                    )

                    SettingsItem(
                        title = "Licenses",
                        icon = Icons.Default.Article,
                        onClick = { navController.navigate("licenses") }
                    )
                }
            }

            item {
                SettingsSection(title = "Support") {
                    SettingsItem(
                        title = "Help Center",
                        icon = Icons.Default.Help,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.helpCenterUrl))
                            context.startActivity(intent)
                        }
                    )

                    SettingsItem(
                        title = "Report Bug",
                        icon = Icons.Default.BugReport,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.bugReportUrl))
                            context.startActivity(intent)
                        }
                    )

                    SettingsItem(
                        title = "Feedback",
                        icon = Icons.Default.Feedback,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.feedbackUrl))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Connect") {
                    SettingsItem(
                        title = "Rate App",
                        icon = Icons.Default.Star,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.playStoreUrl))
                            context.startActivity(intent)
                        }
                    )

                    SettingsItem(
                        title = "Share App",
                        icon = Icons.Default.Share,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, uiState.shareMessage)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Made with ❤️ for die-cast car collectors",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AppHeader(
    version: String,
    buildNumber: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // No logo - just start with the title

        Text(
            text = "Die-cast Car Collectors",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Version $version ($buildNumber)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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