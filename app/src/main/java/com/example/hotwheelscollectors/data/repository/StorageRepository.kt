package com.example.hotwheelscollectors.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.tasks.await

class StorageRepository(private val context: Context) {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    /**
     * Uploads a bitmap photo to Firebase Storage and returns the download URL.
     * 
     * ✅ FIX: Aruncă excepția dacă upload-ul eșuează (în loc să returneze string gol).
     * Astfel, codul apelant poate gestiona erorile corect și nu salvează date corupte.
     * 
     * @param bitmap Bitmap-ul foto să fie upload-at
     * @param path Path-ul în Storage (ex: "mainline/$carId/thumbnail")
     * @return Download URL-ul de la Firebase Storage
     * @throws StorageException Dacă upload-ul eșuează (ex: 403 Permission Denied)
     */
    suspend fun savePhoto(bitmap: Bitmap, path: String): String {
        // ✅ FIX: Păstrează referința la fișier pentru a-l putea șterge în finally
        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)
        
        return try {
            Log.d("StorageRepository", "=== STARTING PHOTO UPLOAD ===")
            Log.d("StorageRepository", "Path: $path")
            
            // Step 1: Save bitmap to temporary file
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            val fileSize = file.length()
            Log.d("StorageRepository", "Bitmap saved to temp file: ${file.absolutePath} (${fileSize} bytes)")
            
            // Step 2: Determine storage path based on series
            // ✅ FIX: Storage Rules expects direct paths (mainline/, premium/, etc.) NOT global/mainline/
            // Path examples: "mainline/$carId/$photoType", "premium/$carId/$photoType", "silver_series/$carId/$photoType"
            // Storage Rules: match /mainline/{carId}/{photoType} - expects direct path without "global/" prefix
            val photoRef = if (path.startsWith("mainline/") || 
                                path.startsWith("premium/") || 
                                path.startsWith("silver_series/") ||
                                path.startsWith("treasure_hunt/") || 
                                path.startsWith("super_treasure_hunt/") || 
                                path.startsWith("others/")) {
                // ✅ Direct path (matches Storage Rules: match /mainline/{carId}/{photoType})
                storageRef.child(path).child(fileName)
            } else {
                // ✅ For other paths (e.g., global/cars/...), use global/ prefix
                storageRef.child("global/$path/$fileName")
            }
            
            Log.d("StorageRepository", "Storage reference: ${photoRef.path}")
            
            val uri = Uri.fromFile(file)
            
            // Step 3: Set explicit contentType to satisfy Storage Rules
            // ✅ FIX: setContentLength() nu există în StorageMetadata.Builder()
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            
            Log.d("StorageRepository", "Uploading to Firebase Storage...")
            
            // Step 4: Upload file to Firebase Storage
            val uploadTask = photoRef.putFile(uri, metadata)
            val uploadResult = uploadTask.await()
            
            Log.d("StorageRepository", "✅ Upload completed. Bytes uploaded: ${uploadResult.bytesTransferred}")
            
            // Step 5: Get download URL
            val downloadUrl = photoRef.downloadUrl.await()
            val urlString = downloadUrl.toString()
            
            Log.i("StorageRepository", "✅ Photo uploaded successfully!")
            Log.d("StorageRepository", "Download URL: $urlString")
            
            // Step 6: Delete temporary file
            file.delete()
            
            urlString
            
        } catch (e: com.google.firebase.storage.StorageException) {
            Log.e("StorageRepository", "❌ Firebase Storage upload failed: ${e.message}", e)
            Log.e("StorageRepository", "  Error code: ${e.errorCode}")
            Log.e("StorageRepository", "  HTTP result: ${e.httpResultCode}")
            Log.e("StorageRepository", "  Storage path: $path")
            // ✅ FIX: Aruncă excepția mai departe în loc să returneze string gol
            // Astfel, codul apelant va ști că upload-ul a eșuat și va putea gestiona eroarea corect
            throw e
        } catch (e: Exception) {
            Log.e("StorageRepository", "❌ Photo upload failed: ${e.message}", e)
            Log.e("StorageRepository", "  Exception type: ${e.javaClass.simpleName}")
            Log.e("StorageRepository", "  Storage path: $path")
            // ✅ FIX: Aruncă excepția mai departe în loc să returneze string gol
            throw e
        } finally {
            // ✅ FIX: Asigură-te că fișierul temporar este șters chiar dacă apare o excepție
            try {
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted) {
                        Log.d("StorageRepository", "✅ Temporary file deleted: ${file.absolutePath}")
                    } else {
                        Log.w("StorageRepository", "⚠️ Failed to delete temporary file: ${file.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                // Ignoră erorile la ștergerea fișierului temporar
                Log.w("StorageRepository", "⚠️ Exception while deleting temp file: ${e.message}")
            }
        }
    }

    suspend fun deletePhoto(path: String) {
        storageRef.child(path).delete().await()
    }

    fun getLocalPhotoPath(fileName: String): String {
        return File(context.filesDir, fileName).absolutePath
    }

    suspend fun syncPhotos() {
        // Implement photo sync logic
    }
}