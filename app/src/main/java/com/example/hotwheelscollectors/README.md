# Die-Cast Collector

A comprehensive collection management app for die-cast model car enthusiasts.

## Features

- Collection Management
- Barcode Scanning
- Photo Management
- Cloud Sync
- Multiple Themes
- Search & Filter

## Project Structure

```
app/
├── data/           # Data layer (repositories, local storage)
├── domain/         # Business logic (use cases, models)
└── ui/             # UI layer (screens, components)
    ├── components/ # Reusable UI components
    ├── screens/    # App screens
    └── theme/      # Theme configuration
```

## Setup Instructions

1. Clone the repository
2. Add your `google-services.json` for Firebase
3. Configure Facebook Login in `strings.xml`
4. Build and run

## Dependencies

See `app/build.gradle.kts` for complete list.

## User Guide

```kotlin:app/src/main/assets/user_guide.md
# Die-Cast Collector User Guide

## Getting Started

1. Launch the app
2. Choose login method:
   - Email/Password
   - Google Account
   - Facebook Account
   - Guest Mode

## Managing Your Collection

### Adding Cars

1. Choose "Add Mainline" or "Add Premium"
2. Scan barcode or enter details manually
3. Take front and back photos
4. Select category/folder
5. Add notes (optional)
6. Save

### Viewing Collection

- "My Collection": View your entire collection
- "Mainline": Browse by brand/model
- "Premium": Browse by series
- "Others": Custom categories

### Features

- Search: Use the search bar to find cars
- Filter: Sort by brand, year, or type
- Share: Share car details or photos
- Cloud Backup: Enable in settings

## Settings

- Storage: Choose local or cloud storage
- Theme: Select from multiple themes
- About: App information
- Terms & Privacy: Legal information
```

Would you like me to:
1. Continue with more implementation details
2. Add more tests
3. Enhance any specific feature
4. Something else

Let me know what you'd like me to focus on next! 