package com.tuckercr.ezlauncher.di

import javax.inject.Qualifier

/**
 * Qualifier for a [kotlinx.coroutines.CoroutineScope] that lives for the
 * lifetime of the application process. Inject this wherever you need work to
 * outlive any individual ViewModel or screen — e.g. repository init blocks
 * that pre-warm caches.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
