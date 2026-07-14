package com.fangjet.launcher.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.fangjet.launcher.data.tts.TtsRepositoryImpl
import com.fangjet.launcher.domain.repository.TtsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// DataStore is created as a top-level property per the official docs.
// The delegate ensures only one instance is created per application process.
private val Context.ttsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tts_preferences",
)

@Module
@InstallIn(SingletonComponent::class)
object TtsDataStoreModule {

    /**
     * Provides the [DataStore<Preferences>] instance for TTS preferences.
     *
     * Kept in a separate @Provides module from [TtsBindingsModule] because
     * @Provides and @Binds cannot coexist in the same module class.
     */
    @Provides
    @Singleton
    fun provideTtsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.ttsDataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsBindingsModule {

    /**
     * Binds [TtsRepositoryImpl] to [TtsRepository].
     *
     * @Singleton is correct here: [TextToSpeech] initialisation takes
     * ~200–400ms. One shared instance means every long-press after the
     * first responds instantly with no re-init delay.
     */
    @Binds
    @Singleton
    abstract fun bindTtsRepository(impl: TtsRepositoryImpl): TtsRepository
}
