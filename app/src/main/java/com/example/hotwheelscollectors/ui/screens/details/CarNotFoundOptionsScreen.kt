package com.example.hotwheelscollectors.ui.screens.details

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hotwheelscollectors.R

@Composable
fun CarNotFoundOptionScreen(
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
                    text = stringResource(R.string.car_not_found),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.car_not_found_message),
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = { navController.navigate("add_mainline") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_mainline))
            }

            Button(
                onClick = { navController.navigate("add_premium") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Star, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_premium))
            }

            Button(
                onClick = { navController.navigate("add_others") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.More, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_other))
            }
        }
    }
}