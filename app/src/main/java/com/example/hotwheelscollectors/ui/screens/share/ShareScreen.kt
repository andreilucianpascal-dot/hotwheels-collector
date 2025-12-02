package com.example.hotwheelscollectors.ui.screens.share

import androidx.compose.material3.ExperimentalMaterial3Api
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.viewmodels.ShareViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ShareScreen(
    navController: NavController,
    carId: String,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showQRDialog by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(carId) {
        viewModel.loadCar(carId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ShareViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ShareViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is ShareViewModel.UiState.Success -> {
                val car = state.car
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        // Car preview card
                        CarPreviewCard(car = car)
                    }

                    item {
                        ShareOption(
                            icon = Icons.Default.Share,
                            title = "Share Details",
                            subtitle = "Share car information as text",
                            onClick = {
                                scope.launch {
                                    shareCarDetails(context, car)
                                }
                            }
                        )
                    }

                    if (car.frontPhotoPath.isNotEmpty() || car.backPhotoPath.isNotEmpty()) {
                        item {
                            ShareOption(
                                icon = Icons.Default.Image,
                                title = "Share Photo",
                                subtitle = "Share car photo",
                                onClick = {
                                    scope.launch {
                                        shareCarPhoto(context, car)
                                    }
                                }
                            )
                        }
                    }

                    item {
                        ShareOption(
                            icon = Icons.Default.QrCode,
                            title = "Share QR Code",
                            subtitle = "Generate QR code for this car",
                            onClick = {
                                scope.launch {
                                    qrBitmap = generateQRCode(car)
                                    showQRDialog = true
                                }
                            }
                        )
                    }

                    item {
                        ShareOption(
                            icon = Icons.Default.Link,
                            title = "Copy Link",
                            subtitle = "Copy shareable link to clipboard",
                            onClick = {
                                copyCarLink(context, car)
                            }
                        )
                    }

                    item {
                        ShareOption(
                            icon = Icons.Default.Download,
                            title = "Export Car Data",
                            subtitle = "Export as JSON file",
                            onClick = {
                                scope.launch {
                                    exportCarData(context, car)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // QR Code Dialog
    if (showQRDialog && qrBitmap != null) {
        AlertDialog(
            onDismissRequest = { showQRDialog = false },
            title = { Text("QR Code") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Scan to view car details",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            shareQRCode(context, qrBitmap!!)
                        }
                    }
                ) {
                    Text("Share QR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQRDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun CarPreviewCard(car: CarEntity) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${car.brand} ${car.model}",
                style = MaterialTheme.typography.titleLarge
            )
            
            if (car.series.isNotEmpty()) {
                Text(
                    text = car.series,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = "${car.year}${if (car.color.isNotEmpty()) " - ${car.color}" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (car.number.isNotEmpty()) {
                Text(
                    text = "Number: ${car.number}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShareOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private suspend fun shareCarDetails(context: Context, car: CarEntity) {
    val shareText = buildString {
        appendLine("🏎️ Hot Wheels Car Details")
        appendLine("=".repeat(30))
        appendLine("Car: ${car.brand} ${car.model}")
        if (car.series.isNotEmpty()) appendLine("Series: ${car.series}")
        appendLine("Year: ${car.year}")
        if (car.color.isNotEmpty()) appendLine("Color: ${car.color}")
        if (car.number.isNotEmpty()) appendLine("Number: ${car.number}")
        if (car.barcode.isNotEmpty()) appendLine("Barcode: ${car.barcode}")
        if (car.notes.isNotEmpty()) {
            appendLine("Notes: ${car.notes}")
        }
        appendLine()
        appendLine("Shared from Hot Wheels Collectors App 🏁")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Hot Wheels: ${car.brand} ${car.model}")
    }
    context.startActivity(Intent.createChooser(intent, "Share Car Details"))
}

private suspend fun shareCarPhoto(context: Context, car: CarEntity) {
    withContext(Dispatchers.IO) {
        try {
            val photoPath = car.frontPhotoPath.takeIf { it.isNotEmpty() } 
                ?: car.backPhotoPath.takeIf { it.isNotEmpty() }
                ?: return@withContext

            val photoFile = File(photoPath)
            if (!photoFile.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Photo not found", Toast.LENGTH_SHORT).show()
                }
                return@withContext
            }

            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )

            val shareText = "${car.brand} ${car.model} (${car.year})\nShared from Hot Wheels Collectors App"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, photoUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(intent, "Share Photo"))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to share photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private suspend fun generateQRCode(car: CarEntity): Bitmap = withContext(Dispatchers.IO) {
    val qrContent = buildString {
        append("hotwheels://car?")
        append("brand=${Uri.encode(car.brand)}")
        append("&model=${Uri.encode(car.model)}")
        append("&year=${car.year}")
        if (car.series.isNotEmpty()) append("&series=${Uri.encode(car.series)}")
        if (car.number.isNotEmpty()) append("&number=${Uri.encode(car.number)}")
        if (car.barcode.isNotEmpty()) append("&barcode=${Uri.encode(car.barcode)}")
    }

    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    bitmap
}

private suspend fun shareQRCode(context: Context, qrBitmap: Bitmap) {
    withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "qr_code_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Scan this QR code to view car details")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(intent, "Share QR Code"))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to share QR code", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun copyCarLink(context: Context, car: CarEntity) {
    val link = "https://hotwheelscollectors.app/car/${car.id}?brand=${Uri.encode(car.brand)}&model=${Uri.encode(car.model)}"
    
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("Car Link", link)
    clipboardManager.setPrimaryClip(clipData)
    
    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
}

private suspend fun exportCarData(context: Context, car: CarEntity) {
    withContext(Dispatchers.IO) {
        try {
            val jsonData = buildString {
                appendLine("{")
                appendLine("  \"id\": \"${car.id}\",")
                appendLine("  \"brand\": \"${car.brand}\",")
                appendLine("  \"model\": \"${car.model}\",")
                appendLine("  \"series\": \"${car.series}\",")
                appendLine("  \"year\": ${car.year},")
                appendLine("  \"color\": \"${car.color}\",")
                appendLine("  \"number\": \"${car.number}\",")
                appendLine("  \"barcode\": \"${car.barcode}\",")
                appendLine("  \"notes\": \"${car.notes}\",")
                appendLine("  \"isPremium\": ${car.isPremium},")
                appendLine("  \"timestamp\": ${car.timestamp}")
                appendLine("}")
            }

            val fileName = "${car.brand}_${car.model}_${System.currentTimeMillis()}.json"
                .replace(" ", "_")
                .replace("/", "_")
            
            val file = File(context.getExternalFilesDir("Exports"), fileName)
            file.parentFile?.mkdirs()
            file.writeText(jsonData)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Car data export from Hot Wheels Collectors")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(intent, "Export Car Data"))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}