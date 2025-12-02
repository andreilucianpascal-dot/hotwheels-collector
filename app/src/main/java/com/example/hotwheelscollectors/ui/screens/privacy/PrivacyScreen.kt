package com.example.hotwheelscollectors.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hotwheelscollectors.R

@Composable
fun PrivacyScreen(
    navController: NavController
) {
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
                    text = stringResource(R.string.privacy_policy),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.privacy_intro),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            items(privacySections) { section ->
                PrivacySection(
                    title = section.title,
                    content = section.content
                )
            }

            item {
                Text(
                    text = stringResource(R.string.privacy_contact),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                Text(
                    text = stringResource(R.string.privacy_last_updated),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private val privacySections = listOf(
    PrivacySection(
        title = "Information We Collect",
        content = """
            We collect information that you provide directly to us, including:
            • Account information (email, name)
            • Collection data (car details, photos)
            • Device information
            • Usage data
        """.trimIndent()
    ),
    PrivacySection(
        title = "How We Use Your Information",
        content = """
            We use the information we collect to:
            • Provide and maintain the app
            • Improve our services
            • Communicate with you
            • Ensure security
        """.trimIndent()
    ),
    PrivacySection(
        title = "Data Storage",
        content = """
            Your collection data is stored securely using:
            • Local device storage
            • Cloud backup (optional)
            • Firebase services
        """.trimIndent()
    ),
    PrivacySection(
        title = "Data Sharing",
        content = """
            We do not sell your personal information. Data sharing is limited to:
            • Service providers
            • Legal requirements
            • With your consent
        """.trimIndent()
    ),
    PrivacySection(
        title = "Your Rights",
        content = """
            You have the right to:
            • Access your data
            • Correct your data
            • Delete your data
            • Export your data
        """.trimIndent()
    ),
    PrivacySection(
        title = "Security",
        content = """
            We implement appropriate security measures to protect your data:
            • Encryption
            • Secure authentication
            • Regular security audits
        """.trimIndent()
    ),
    PrivacySection(
        title = "Changes to Policy",
        content = """
            We may update this policy occasionally. We will notify you of any significant changes through the app or email.
        """.trimIndent()
    )
)

private data class PrivacySection(
    val title: String,
    val content: String
)