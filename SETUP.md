# Die-Cast Collector Setup Guide

## Prerequisites
- Android Studio Electric Eel (2022.1.1) or newer
- JDK 11 or newer
- A Firebase account
- A Facebook Developer account

## Step 1: Firebase Setup
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project named "Die-Cast Collector"
3. Add Android app:
   - Package name: com.example.hotwheelscollectors
   - App nickname: Die-Cast Collector
   - Debug signing SHA-1: (Get this from Android Studio)
4. Download `google-services.json`
5. Place `google-services.json` in the app/ folder

## Step 2: Facebook Setup
1. Go to [Facebook Developers](https://developers.facebook.com/)
2. Create a new app
3. Get your App ID and Client Token
4. Update strings.xml with your Facebook credentials:
   - facebook_app_id
   - fb_login_protocol_scheme
   - facebook_client_token

## Step 3: Android Studio Setup
1. Open the project in Android Studio
2. Sync project with Gradle files
3. Build the project
4. Run on an emulator or physical device

## Common Issues

### Build Fails
- Make sure google-services.json is in the correct location
- Verify Gradle sync completed successfully
- Check Facebook credentials are properly configured

### Login Issues
- Verify SHA-1 in Firebase console matches your debug keystore
- Check internet connectivity
- Verify Facebook App ID is correct

## Need Help?
If you encounter any issues:
1. Check the error logs in Android Studio
2. Verify all credentials are correctly configured
3. Make sure all dependencies are up to date

## Testing
After setup, test:
1. Firebase connection (try guest login)
2. Facebook login
3. Photo upload to Firebase Storage
4. Cloud Firestore data sync 