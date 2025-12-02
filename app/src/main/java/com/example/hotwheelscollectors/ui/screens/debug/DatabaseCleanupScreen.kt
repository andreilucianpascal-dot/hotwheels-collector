package com.example.hotwheelscollectors.ui.screens.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.hotwheelscollectors.viewmodels.DatabaseCleanupViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hotwheelscollectors.ui.components.TopAppBarWithBack
import androidx.compose.ui.unit.dp
import com.example.hotwheelscollectors.utils.DatabaseStats
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseCleanupScreen(
    onNavigateBack: () -> Unit,
    viewModel: DatabaseCleanupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    // Check authentication status
    val authStatus = remember { mutableStateOf("Checking...") }
    LaunchedEffect(Unit) {
        authStatus.value = viewModel.checkAuthenticationStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        TopAppBarWithBack(
            title = "Database Cleanup",
            onNavigateBack = onNavigateBack
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Authentication Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = authStatus.value,
                    color = if (authStatus.value.startsWith("✅")) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Database Statistics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (uiState.databaseStats != null) {
                    val stats = uiState.databaseStats!!
                    Text("Total Cars: ${stats.totalCars}")
                    Text("Total Photos: ${stats.totalPhotos}")
                    Text("Total Keywords: ${stats.totalKeywords}")
                    Text("Mainline Cars: ${stats.mainlineCars}")
                    Text("Others Cars: ${stats.othersCars}")
                    Text("Premium Cars: ${stats.premiumCars}")
                    Text("Generic Brand Cars: ${stats.genericBrandCars}")
                } else {
                    Text("Loading statistics...")
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Cleanup Actions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Remove Generic Brand Cars
                Button(
                    onClick = { viewModel.removeGenericBrandCars() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove Generic Brand Cars")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Remove Duplicate Cars
                Button(
                    onClick = { viewModel.removeDuplicateCars() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove Duplicate Cars")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Clear User Data
                Button(
                    onClick = { viewModel.clearUserData() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear User Data")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Complete Database Cleanup
                Button(
                    onClick = { viewModel.clearAllData() },
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All Data (DANGER)")
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (uiState.message.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isError) 
                        MaterialTheme.colorScheme.errorContainer 
                    else 
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = uiState.message,
                    modifier = Modifier.padding(16.dp),
                    color = if (uiState.isError) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
