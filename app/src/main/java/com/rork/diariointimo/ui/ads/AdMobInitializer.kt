package com.rork.diariointimo.ui.ads

import android.content.Context
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AdMobInitializer {
    private const val TAG = "AdMob"

    @Volatile
    var isInitialized = false
        private set

    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(
                    context,
                    InitializationConfig.Builder(AdMobConfig.APP_ID).build()
                ) {
                    Log.d(TAG, "GMA Next-Gen SDK adapters initialized")
                }
                isInitialized = true
                InterstitialAdManager.startPreloading()
                Log.d(TAG, "GMA Next-Gen SDK initialization complete")
            } catch (e: Exception) {
                Log.e(TAG, "GMA SDK initialization failed", e)
            }
        }
    }
}
