// app/src/main/java/com/example/hotwheelscollectors/ui/screens/terms/TermsScreen.kt

package com.example.hotwheelscollectors.ui.screens.terms

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
fun TermsScreen(
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
                    text = stringResource(R.string.terms_of_service),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.terms_intro),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            items(termsSection) { section ->
                TermsSection(
                    title = section.title,
                    content = section.content
                )
            }

            item {
                Text(
                    text = stringResource(R.string.terms_contact),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                Text(
                    text = stringResource(R.string.terms_last_updated),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TermsSection(
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

private val termsSection = listOf(
    TermsSection(
        title = "Acceptance of Terms",
        content = """
            By accessing or using the Hot Wheels Collectors app, you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use the app.
        """.trimIndent()
    ),
    TermsSection(
        title = "User Accounts",
        content = """
            • You are responsible for maintaining the confidentiality of your account
            • You must provide accurate and complete information
            • You are responsible for all activities under your account
            • You must notify us of any unauthorized use
        """.trimIndent()
    ),
    TermsSection(
        title = "User Content",
        content = """
            • You retain ownership of your content
            • You grant us license to use your content
            • You are responsible for your content
            • Content must not violate any laws or rights
        """.trimIndent()
    ),
    TermsSection(
        title = "Prohibited Activities",
        content = """
            You agree not to:
            • Violate any laws
            • Impersonate others
            • Upload malicious code
            • Interfere with app operation
            • Collect user data without consent
        """.trimIndent()
    ),
    TermsSection(
        title = "Intellectual Property",
        content = """
            • The app and its content are protected by copyright
            • Trademarks are property of their respective owners
            • You may not use our IP without permission
        """.trimIndent()
    ),
    TermsSection(
        title = "App Updates",
        content = """
            • We may update the app periodically
            • Updates may be required for continued use
            • We are not obligated to provide updates
        """.trimIndent()
    ),
    TermsSection(
        title = "Termination",
        content = """
            We may terminate or suspend access to the app:
            • For violations of terms
            • At our sole discretion
            • Without prior notice
        """.trimIndent()
    ),
    TermsSection(
        title = "Disclaimer",
        content = """
            • The app is provided "as is"
            • We make no warranties
            • Use is at your own risk
        """.trimIndent()
    ),
    TermsSection(
        title = "Limitation of Liability",
        content = """
            We are not liable for:
            • Direct or indirect damages
            • Loss of data
            • Service interruptions
            • Third-party actions
        """.trimIndent()
    ),
    TermsSection(
        title = "Changes to Terms",
        content = """
            • We may modify these terms
            • Changes will be effective immediately
            • Continued use constitutes acceptance
        """.trimIndent()
    )
)

private data class TermsSection(
    val title: String,
    val content: String
)