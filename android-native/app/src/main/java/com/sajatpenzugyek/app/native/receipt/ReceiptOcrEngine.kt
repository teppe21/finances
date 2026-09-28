package com.sajatpenzugyek.app.native.receipt

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sajatpenzugyek.app.domain.model.ReceiptScan
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ReceiptOcrEngine(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeFromBitmap(bitmap: Bitmap): ReceiptScan {
        val image = InputImage.fromBitmap(bitmap, 0)
        return recognize(image, null)
    }

    suspend fun recognizeFromUri(uri: Uri): ReceiptScan {
        val image = InputImage.fromFilePath(context, uri)
        return recognize(image, uri.toString())
    }

    private suspend fun recognize(image: InputImage, imageUri: String?): ReceiptScan {
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val parsed = ReceiptParser.parse(visionText.text, imageUri)
                    continuation.resume(parsed)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }
}
