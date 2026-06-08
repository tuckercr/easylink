package com.tuckercr.ezlauncher.data.cache

import android.content.Context
import androidx.startup.Initializer

/**
 * Jetpack Startup initializer that pre-warms the installed-apps cache.
 *
 * Startup initializers run during [androidx.startup.InitializationProvider]'s
 * [android.content.ContentProvider.onCreate], which is called before
 * [android.app.Application.onCreate]. This means the package-manager query
 * is already in flight before any Activity or ViewModel is created, so "All
 * Apps" feels instant rather than showing a loading spinner.
 *
 * The actual query runs on [AppListCache.scope] (Dispatchers.Default), so
 * this [create] method returns immediately without blocking the main thread.
 */
class AppListInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        AppListCache.prewarm(context.applicationContext)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
