package com.thelightphone.sdk

import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@Keep
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EntryPoint

interface LightEntryPoint {
    fun onToolCreate(lightOsData: StateFlow<LightOsData?>, toolScope: CoroutineScope)
}
