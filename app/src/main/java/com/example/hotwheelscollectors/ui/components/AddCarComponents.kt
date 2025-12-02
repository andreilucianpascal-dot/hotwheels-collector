package com.example.hotwheelscollectors.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InitialOptionsScreen(
    onScanBarcode: () -> Unit,
    onSearchDatabase: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Add New Car",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onScanBarcode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Barcode")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSearchDatabase,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search Database")
        }
    }
}

@Composable
fun SearchDatabaseScreen(
    onCarFound: (com.example.hotwheelscollectors.model.HotWheelsCar) -> Unit,
    onCarNotFound: () -> Unit
) {
    var isSearching by remember { mutableStateOf(true) }
    var searchResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Simulate database search
        kotlinx.coroutines.delay(2000)
        searchResult = "No car found in database"
        isSearching = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSearching) {
            CircularProgressIndicator()
            Text("Searching database...")
        } else {
            Text(
                text = searchResult ?: "Search complete",
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = { /* Navigate to take photos */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Take Photo (Front + Back of Card)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Navigate to upload photos */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upload (Front + Back of Card)")
            }
        }
    }
}

@Composable
fun PhotoOptionsScreen(
    onTakePhoto: () -> Unit,
    onUploadPhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Add Photos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Take Photo (Front + Back of Card)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onUploadPhoto,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload (Front + Back of Card)")
        }
    }
}

@Composable
fun PhotoCaptureScreen(
    title: String,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Take Photo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPickFromGallery,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pick from Gallery")
        }
    }
}