// SecurityRules.kt
package com.example.hotwheelscollectors.security

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRules @Inject constructor(
    private val db: FirebaseFirestore
) {
    fun getFirestoreRules(): String {
        return """
        rules_version = '2';
        service cloud.firestore {
          match /databases/{database}/documents {
            // User profiles
            match /users/{userId} {
              allow read: if request.auth != null && request.auth.uid == userId;
              allow write: if request.auth != null && request.auth.uid == userId;
              
              // Car collection
              match /cars/{carId} {
                allow read: if request.auth != null && request.auth.uid == userId;
                allow write: if request.auth != null && request.auth.uid == userId;
                
                // Validate car data
                function isValidCar() {
                  let car = request.resource.data;
                  return car.name is string &&
                         car.brand is string &&
                         (car.year == null || car.year is number) &&
                         (car.price == null || car.price is number);
                }
                
                allow create: if isValidCar();
                allow update: if isValidCar();
              }
              
              // Photos
              match /photos/{photoId} {
                allow read: if request.auth != null && request.auth.uid == userId;
                allow write: if request.auth != null && 
                           request.auth.uid == userId && 
                           request.resource.size < 5 * 1024 * 1024; // 5MB limit
              }
              
              // Trade offers
              match /trades/{tradeId} {
                allow read: if request.auth != null && 
                          (request.auth.uid == userId || 
                           request.auth.uid == resource.data.targetUserId);
                allow create: if request.auth != null && 
                            request.auth.uid == userId;
                allow update: if request.auth != null && 
                            (request.auth.uid == userId || 
                             request.auth.uid == resource.data.targetUserId);
              }
            }
            
            // Public data
            match /public/{document=**} {
              allow read: if true;
              allow write: if false;
            }
            
            // ✅ GLOBAL COLLECTIONS - Allow authenticated users to contribute
            match /globalBarcodes/{barcode} {
              allow read: if true; // Anyone can read global data
              allow write: if request.auth != null; // Only authenticated users can write
              
              // Validate global barcode data
              function isValidGlobalBarcode() {
                let data = request.resource.data;
                return data.barcode is string &&
                       data.carName is string &&
                       data.brand is string &&
                       data.series is string &&
                       data.contributorUserId is string &&
                       data.contributorUserId == request.auth.uid; // User can only contribute as themselves
              }
              
              allow create: if isValidGlobalBarcode();
              allow update: if isValidGlobalBarcode();
            }
            
            match /globalCars/{carId} {
              allow read: if true; // Anyone can read global data
              allow write: if request.auth != null; // Only authenticated users can write
              
              // Validate global car data
              function isValidGlobalCar() {
                let data = request.resource.data;
                return data.carName is string &&
                       data.brand is string &&
                       data.series is string &&
                       data.contributorUserId is string &&
                       data.contributorUserId == request.auth.uid; // User can only contribute as themselves
              }
              
              allow create: if isValidGlobalCar();
              allow update: if isValidGlobalCar();
            }
          }
        }
        """.trimIndent()
    }

    fun getStorageRules(): String {
        return """
        rules_version = '2';
        service firebase.storage {
          match /b/{bucket}/o {
            match /users/{userId}/{allPaths=**} {
              allow read: if request.auth != null && request.auth.uid == userId;
              allow write: if request.auth != null && 
                         request.auth.uid == userId &&
                         request.resource.size < 1 * 1024 * 1024 && // 1MB limit per file
                         request.resource.contentType.matches('image/.*');
            }
            
            // ✅ GLOBAL PHOTOS - Allow authenticated users to upload global photos
            match /global/{allPaths=**} {
              allow read: if true; // Anyone can read global photos
              allow write: if request.auth != null &&
                         request.resource.size < 1 * 1024 * 1024 && // 1MB limit per file
                         request.resource.contentType.matches('image/.*'); // Only images
            }
            
            match /public/{allPaths=**} {
              allow read: if true;
              allow write: if false;
            }
          }
        }
        """.trimIndent()
    }
}