package com.example.hotwheelscollectors.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.hotwheelscollectors.data.local.AppDatabase
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.local.dao.UserDao
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.dao.PriceHistoryDao
import com.example.hotwheelscollectors.data.local.dao.SearchHistoryDao
import com.example.hotwheelscollectors.data.local.dao.SearchKeywordDao
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.LocalRepository
import com.example.hotwheelscollectors.data.repository.GoogleDriveRepository
import com.example.hotwheelscollectors.data.repository.CloudUserSettingsRepository
import com.example.hotwheelscollectors.data.repository.OneDriveRepository
import com.example.hotwheelscollectors.data.repository.DropboxRepository
import com.example.hotwheelscollectors.data.repository.CarSyncRepository
import com.example.hotwheelscollectors.data.repository.PhotoProcessingRepository
import com.example.hotwheelscollectors.data.repository.StorageRepository
import com.example.hotwheelscollectors.data.repository.UserCloudSyncRepository
import com.example.hotwheelscollectors.data.repository.UserCloudSyncManager
import com.example.hotwheelscollectors.data.repository.UserCloudRestoreManager
import com.example.hotwheelscollectors.data.auth.GoogleDriveAuthService
import com.example.hotwheelscollectors.utils.DatabaseCleanup
import com.example.hotwheelscollectors.utils.PhotoOptimizer

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth
    ): AuthRepository = AuthRepository(auth)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideCarDao(db: AppDatabase): CarDao = db.carDao()

    @Provides
    fun providePhotoDao(db: AppDatabase): PhotoDao = db.photoDao()

    @Provides
    fun providePriceHistoryDao(db: AppDatabase): PriceHistoryDao = db.priceHistoryDao()

    @Provides
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideSearchKeywordDao(db: AppDatabase): SearchKeywordDao = db.searchKeywordDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences =
        UserPreferences(context)

    @Provides
    @Singleton
    fun provideFirestoreRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        storage: FirebaseStorage,
        carDao: CarDao,
        photoDao: PhotoDao,
        userDao: UserDao
    ): FirestoreRepository = FirestoreRepository(firestore, auth, storage, carDao, photoDao, userDao)

    @Provides
    @Singleton
    fun provideLocalRepository(
        @ApplicationContext context: Context,
        carDao: CarDao,
        photoDao: PhotoDao,
    ): LocalRepository = LocalRepository(context, carDao, photoDao)

    @Provides
    @Singleton
    fun provideGoogleDriveRepository(
        @ApplicationContext context: Context,
        appDatabase: AppDatabase,
        carDao: CarDao,
        photoDao: PhotoDao,
        gson: Gson,
        userPreferences: UserPreferences,
        authRepository: AuthRepository,
        driveRootFolderManager: com.example.hotwheelscollectors.data.repository.DriveRootFolderManager
    ): GoogleDriveRepository = GoogleDriveRepository(context, appDatabase, carDao, photoDao, gson, userPreferences, authRepository, driveRootFolderManager)

    @Provides
    @Singleton
    fun provideGoogleDriveAuthService(
        @ApplicationContext context: Context,
    ): GoogleDriveAuthService = GoogleDriveAuthService(context)

    @Provides
    @Singleton
    fun provideCloudUserSettingsRepository(
        @ApplicationContext context: Context,
        authRepository: AuthRepository,
        userPreferences: UserPreferences,
        gson: Gson,
        driveRootFolderManager: com.example.hotwheelscollectors.data.repository.DriveRootFolderManager
    ): CloudUserSettingsRepository = CloudUserSettingsRepository(context, authRepository, userPreferences, gson, driveRootFolderManager)

    @Provides
    @Singleton
    fun providePhotoOptimizer(
        @ApplicationContext context: Context,
    ): PhotoOptimizer = PhotoOptimizer(context)

    @Provides
    @Singleton
    fun provideDropboxRepository(
        @ApplicationContext context: Context,
        carDao: CarDao,
        photoDao: PhotoDao
    ): DropboxRepository = DropboxRepository(context, carDao, photoDao)

    @Provides
    @Singleton
    fun provideOneDriveRepository(
        @ApplicationContext context: Context,
        carDao: CarDao,
        photoDao: PhotoDao
    ): OneDriveRepository = OneDriveRepository(context, carDao, photoDao)


    @Provides
    @Singleton
    fun provideStorageRepository(
        @ApplicationContext context: Context
    ): StorageRepository = StorageRepository(context)
    
    @Provides
    @Singleton
    fun provideAddCarUseCase(
        @ApplicationContext context: Context,
        userStorageRepository: com.example.hotwheelscollectors.data.repository.UserStorageRepository,
        photoProcessingRepository: PhotoProcessingRepository,
        carSyncRepository: CarSyncRepository,
        authRepository: AuthRepository,
        userDao: com.example.hotwheelscollectors.data.local.dao.UserDao,
        carDao: com.example.hotwheelscollectors.data.local.dao.CarDao,
        firestoreRepository: FirestoreRepository,
        userCloudSyncRepository: UserCloudSyncRepository
    ): com.example.hotwheelscollectors.domain.usecase.collection.AddCarUseCase = 
        com.example.hotwheelscollectors.domain.usecase.collection.AddCarUseCase(
            context,
            userStorageRepository,
            photoProcessingRepository,
            carSyncRepository,
            authRepository,
            userDao,
            carDao,
            firestoreRepository,
            userCloudSyncRepository
        )

    @Provides
    @Singleton
    fun provideDatabaseCleanup(
        @ApplicationContext context: Context,
        carDao: CarDao,
        photoDao: PhotoDao
    ): DatabaseCleanup = DatabaseCleanup(context, carDao, photoDao)

    @Provides
    @Singleton
    fun provideUserCloudSyncRepository(
        @ApplicationContext context: Context,
        appDatabase: AppDatabase,
        userPreferences: UserPreferences,
        gson: Gson,
        googleDriveRepository: GoogleDriveRepository,
        oneDriveRepository: OneDriveRepository,
        dropboxRepository: DropboxRepository
    ): UserCloudSyncRepository = UserCloudSyncRepository(
        context,
        appDatabase,
        userPreferences,
        gson,
        googleDriveRepository,
        oneDriveRepository,
        dropboxRepository
    )

    @Provides
    @Singleton
    fun provideUserCloudSyncManager(
        @ApplicationContext context: Context,
        userCloudSyncRepository: UserCloudSyncRepository,
        userPreferences: UserPreferences
    ): UserCloudSyncManager = UserCloudSyncManager(
        context,
        userCloudSyncRepository,
        userPreferences
    )

    @Provides
    @Singleton
    fun provideUserCloudRestoreManager(
        @ApplicationContext context: Context,
        appDatabase: AppDatabase,
        userPreferences: UserPreferences,
        authRepository: AuthRepository,
        gson: Gson,
        googleDriveRepository: GoogleDriveRepository,
        oneDriveRepository: OneDriveRepository,
        dropboxRepository: DropboxRepository
    ): UserCloudRestoreManager = UserCloudRestoreManager(
        context,
        appDatabase,
        userPreferences,
        authRepository,
        gson,
        googleDriveRepository,
        oneDriveRepository,
        dropboxRepository
    )
}


