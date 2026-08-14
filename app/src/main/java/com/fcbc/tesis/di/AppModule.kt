package com.fcbc.tesis.di

import androidx.room.Room
import com.fcbc.tesis.data.local.AppDatabase
import com.fcbc.tesis.data.classifier.PytorchLeafClassifier
import com.fcbc.tesis.data.repository.AuthRepositoryImpl
import com.fcbc.tesis.data.repository.CommentRepositoryImpl
import com.fcbc.tesis.data.repository.ForumRepositoryImpl
import com.fcbc.tesis.data.repository.HistoryRepositoryImpl
import com.fcbc.tesis.data.repository.SupabaseStorageRepositoryImpl
import com.fcbc.tesis.domain.repository.AuthRepository
import com.fcbc.tesis.domain.repository.CommentRepository
import com.fcbc.tesis.domain.repository.ForumRepository
import com.fcbc.tesis.domain.repository.HistoryRepository
import com.fcbc.tesis.data.repository.UserRepositoryImpl
import com.fcbc.tesis.domain.repository.UserRepository
import com.fcbc.tesis.domain.repository.LeafClassifier
import com.fcbc.tesis.domain.repository.StorageRepository
import com.fcbc.tesis.domain.usecase.*
import com.fcbc.tesis.ui.viewmodel.ForumViewModel
import com.fcbc.tesis.ui.viewmodel.HistoryViewModel
import com.fcbc.tesis.ui.viewmodel.HomeViewModel
import com.fcbc.tesis.ui.viewmodel.AuthViewModel
import com.fcbc.tesis.ui.viewmodel.PublicProfileViewModel
import com.fcbc.tesis.ui.viewmodel.UserInteractionsViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import com.fcbc.tesis.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<SupabaseClient> {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth)
            install(Storage)
            defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).addMigrations(AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8)
         .fallbackToDestructiveMigration()
         .fallbackToDestructiveMigrationOnDowngrade()
         .build()
    }

    single { get<AppDatabase>().historyDao() }
    single { get<AppDatabase>().forumDao() }
    single { get<AppDatabase>().commentDao() }
    single { get<AppDatabase>().chatDao() }
    single { get<AppDatabase>().interactionDao() }

    single<HistoryRepository> { HistoryRepositoryImpl(get(), get()) }
    single<ForumRepository> { ForumRepositoryImpl(get(), get(), get(), androidContext()) }
    single<CommentRepository> { CommentRepositoryImpl(get(), get(), androidContext()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<StorageRepository> { SupabaseStorageRepositoryImpl(get()) }
    single<LeafClassifier> { PytorchLeafClassifier(androidContext()) }
    single<com.fcbc.tesis.domain.repository.ChatRepository> { com.fcbc.tesis.data.repository.ChatRepositoryImpl(get(), get(), androidContext()) }
    single<UserRepository> { com.fcbc.tesis.data.repository.UserRepositoryImpl(get(), get(), androidContext()) }
    single<com.fcbc.tesis.domain.repository.AppMetricsRepository> { com.fcbc.tesis.data.repository.AppMetricsRepositoryImpl(androidContext()) }

    // Firebase Analytics y Crashlytics (Proteger contra dispositivos sin Google Play Services como Xiaomi/Huawei)
    single<com.google.firebase.analytics.FirebaseAnalytics?> {
        try {
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(androidContext())
        } catch (e: Throwable) {
            null
        }
    }
    single<com.google.firebase.crashlytics.FirebaseCrashlytics?> {
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
        } catch (e: Throwable) {
            null
        }
    }
    single<com.fcbc.tesis.domain.repository.AnalyticsRepository> { com.fcbc.tesis.data.repository.AnalyticsRepositoryImpl(getOrNull(), getOrNull()) }

    // Capa de Dominio - Casos de Uso (Interactors)
    factory { AnalyzeLeafUseCase(get(), get()) }
    factory { SaveHistoryUseCase(get()) }
    factory { GetHistoryUseCase(get()) }
    factory { DeleteHistoryItemUseCase(get()) }
    factory { ClearHistoryUseCase(get()) }
    factory { SyncRemoteHistoryUseCase(get()) }
    factory { GetForumPostsUseCase(get()) }
    factory { SyncRemotePostsUseCase(get()) }
    factory { AddForumPostUseCase(get()) }
    factory { ToggleLikeUseCase(get()) }
    factory { GetCommentsUseCase(get()) }
    factory { GetAllCommentsUseCase(get()) }
    factory { AddCommentUseCase(get()) }
    factory { LoginWithGoogleUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { SignInWithEmailUseCase(get()) }
    factory { SignUpWithEmailUseCase(get()) }
    factory { VerifyOtpUseCase(get()) }
    factory { ResendCodeUseCase(get()) }
    factory { ResetPasswordUseCase(get()) }
    factory { com.fcbc.tesis.domain.usecase.GetChatRoomsUseCase(get()) }
    factory { com.fcbc.tesis.domain.usecase.GetMessagesUseCase(get()) }
    factory { com.fcbc.tesis.domain.usecase.GetOrCreateChatRoomUseCase(get()) }
    factory { com.fcbc.tesis.domain.usecase.SendMessageUseCase(get()) }
    
    // Novedad: Users & Social Interactions
    factory { GetPublicProfileUseCase(get()) }
    factory { FollowUserUseCase(get()) }
    factory { UnfollowUserUseCase(get()) }
    factory { IsFollowingUseCase(get()) }
    factory { GetFollowingListUseCase(get()) }
    factory { UpdateProfileDataUseCase(get()) }
    factory { GetPostsByUserIdUseCase(get()) }
    factory { GetSavedPostsUseCase(get()) }
    factory { ToggleSavePostUseCase(get()) }
    factory { DeletePostUseCase(get()) }
    factory { com.fcbc.tesis.domain.usecase.CalculateAppSizeUseCase(get()) }

    single { com.fcbc.tesis.auth.GoogleAuthClient(androidContext()) }

    // Capa de Presentación - ViewModels
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { HistoryViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ForumViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { AuthViewModel(get(), get(), get(), get(), get(), get(), get(), get(), androidContext()) }
    viewModel { com.fcbc.tesis.ui.viewmodel.ChatViewModel(get(), get(), get(), get()) }
    viewModel { PublicProfileViewModel(get(), get(), get(), get(), get()) }
    viewModel { UserInteractionsViewModel(get(), get(), get()) }
}
