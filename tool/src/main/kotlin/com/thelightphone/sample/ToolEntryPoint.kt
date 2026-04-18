package com.thelightphone.sample

import android.util.Log
import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.sdk.LightOsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@EntryPoint
object ToolEntryPoint : LightEntryPoint {
    // called when Tool first launches, use to initialize dependencies etc
    override fun onToolCreate(
        lightOsData: StateFlow<LightOsData?>,
        toolScope: CoroutineScope
    ) {
        toolScope.launch {
            lightOsData.collect {
                Log.d("ToolEntryPoint", "Current LightOS registration data: $it")
            }
        }
    }
}