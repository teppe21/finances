package com.sajatpenzugyek.app.feature.receipt

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import kotlin.math.abs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sajatpenzugyek.app.R
import com.sajatpenzugyek.app.core.theme.Blue500
import com.sajatpenzugyek.app.core.theme.Emerald500
import com.sajatpenzugyek.app.core.utils.CurrencyFormatter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReceiptViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.processUri(context, uri)
        }
    }

    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // CameraX Viewfinder
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (_: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.camera_permission_needed),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        }

        // Top Controls: Back button & Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.clickable { onNavigateBack() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.back), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = stringResource(R.string.receipt_title),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // Framing Guide Box
        Box(
            modifier = Modifier
                .size(width = 280.dp, height = 380.dp)
                .align(Alignment.Center)
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = stringResource(R.string.align_frame_guide),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Bottom Shutter & Triggers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Picker
            IconButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
            }

            // Capture Button
            Surface(
                modifier = Modifier
                    .size(76.dp)
                    .clickable {
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    val buffer = imageProxy.planes[0].buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    imageProxy.close()
                                    viewModel.processBitmap(context, bitmap)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    // Fallback to demo text if device emulator/no physical camera
                                    viewModel.processDemoText("LIDL MAGYARORSZAG KFT.\nFIZETENDO: 1 249 Ft\nKESZPENZ: 2000 Ft")
                                }
                            }
                        )
                    },
                shape = CircleShape,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(4.dp, Blue500)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Camera, contentDescription = null, tint = Blue500, modifier = Modifier.size(32.dp))
                }
            }

            // Demo Synthetic Receipt Trigger
            IconButton(
                onClick = {
                    viewModel.processDemoText(
                        "SPAR MAGYARORSZAG KFT.\n2026.03.28 10:15\nTEJ 399 Ft\nKENYER 850 Ft\nFIZETENDO: 1 249 Ft\nKESZPENZ: 2 000 Ft\nVISSZAJARO: 751 Ft\nAFA TARTALOM: 60 Ft"
                    )
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.demo_receipt), tint = Emerald500)
            }
        }

        // OCR Loading Spinner Overlay
        if (state.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Blue500)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.processing_ocr),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Receipt Review Bottom Sheet
        if (state.scannedReceipt != null) {
            val scan = state.scannedReceipt!!
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { viewModel.clearScan() },
                sheetState = sheetState
            ) {
                var merchant: String by remember { mutableStateOf(scan.merchant ?: "Store / Merchant") }
                val initialAmount = ((scan.totalMinor?.let { abs(it) } ?: 124900L) / 100L).toString()
                var totalAmountText: String by remember { mutableStateOf(initialAmount) }
                var selectedCurrency by remember { mutableStateOf(scan.currency.ifBlank { "HUF" }) }

                val merchantLabel = stringResource(R.string.merchant)
                val totalLabel = "${stringResource(R.string.total)} ($selectedCurrency)"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.review_receipt),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text(merchantLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = totalAmountText,
                        onValueChange = { totalAmountText = it.filter { c -> c.isDigit() } },
                        label = { Text(totalLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("HUF", "EUR", "USD", "GBP", "CHF").forEach { curr ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedCurrency == curr,
                                onClick = { selectedCurrency = curr },
                                label = { Text(curr) }
                            )
                        }
                    }

                    val cashText = stringResource(R.string.payment_cash)
                    val cardText = stringResource(R.string.payment_card)
                    val methodLabel = stringResource(R.string.payment_method)
                    val methodVal = if (scan.paymentMethod == com.sajatpenzugyek.app.domain.model.PaymentMethod.CASH) cashText else cardText

                    Text(
                        text = "$methodLabel: $methodVal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Emerald500,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = {
                            val minor = (totalAmountText.toLongOrNull() ?: 0L) * 100L
                            val accId = state.selectedAccountId ?: "acc_cash"
                            val catId = state.selectedCategoryId ?: "food"
                            viewModel.confirmSave(
                                merchant = merchant,
                                amountMinor = minor,
                                date = scan.date ?: LocalDate.now(),
                                accountId = accId,
                                categoryId = catId,
                                currency = selectedCurrency
                            ) {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.confirm_save))
                    }
                }
            }
        }
    }
}
