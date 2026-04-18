package com.thelightphone.sdk

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.thelightphone.sdk.LightCrypto.LIGHTOS_PACKAGE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// TODO - this is a placeholder for data returned by LightOS when an SDK tool registers with it
// Might contain things like push notification tokens etc
typealias LightOsData = String
open class LightSdkApplication : Application() {

    companion object {
        private const val TAG = "LightSdkApplication"
        private const val ACTION_SDK_HANDSHAKE = "com.lightos.ACTION_SDK_HANDSHAKE"
        private const val RESULT_OK = 0

        private val _lightOSData = MutableStateFlow<String?>(null)
        val lightOsData: StateFlow<LightOsData?> = _lightOSData.asStateFlow()
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        invokeEntryPoint()
        registerWithLightOs()
    }

    // Tool may have registered an initialization function, call it
    private fun invokeEntryPoint() {
        val entryPoint = LightSdkRegistry.entryPoint ?: return

        runCatching {
            entryPoint.onToolCreate(lightOsData, applicationScope)
        }.onFailure {
            Log.e(TAG, "Failed to invoke @EntryPoint", it)
        }
    }

    private fun registerWithLightOs() {
        val publicKey = LightCrypto.getPublicKeyBase64()

        val intent = Intent(ACTION_SDK_HANDSHAKE).apply {
            setPackage(LIGHTOS_PACKAGE)

            // a PendingIntent will be annotated with the sending package name (this tool)
            // by the system, this lets LightOS confidently know where the broadcast came from
            val identity = PendingIntent.getActivity(
                this@LightSdkApplication,
                0,
                Intent(),
                PendingIntent.FLAG_IMMUTABLE
            )
            putExtra("sender_identity", identity)
            putExtra("public_key", publicKey)
        }

        // Send registration broadcast to LightOS, expect encrypted response
        sendOrderedBroadcast(
            intent,
            null,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent?) {
                    if (resultCode != RESULT_OK) {
                        Log.w(TAG, "System app responded with code $resultCode")
                        return
                    }

                    val encryptedResponse = resultData ?: return

                    val decrypted = runCatching { LightCrypto.decrypt(encryptedResponse) }
                        .onFailure { Log.e(TAG, "Failed to decrypt system app response", it) }
                        .getOrNull() ?: return

                    _lightOSData.value = decrypted
                }
            },
            Handler(Looper.getMainLooper()),
            -1,
            null,
            null
        )
    }
}
