package dev.hossain.trmnl.di

import android.app.Activity
import dagger.MapKey
import kotlin.reflect.KClass

/**
 * A Dagger multi-binding key used for registering a [Activity] into the top level dagger graphs.
 */
@MapKey annotation class ActivityKey(
    val value: KClass<out Activity>,
)
