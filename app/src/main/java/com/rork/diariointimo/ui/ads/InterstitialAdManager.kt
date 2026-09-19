package com.rork.diariointimo.ui.ads

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader

object InterstitialAdManager {
    private const val TAG = "AdMob"
    private const val AD_UNIT_ID = AdMobConfig.INTERSTITIAL_AD_UNIT_ID
    private const val MIN_ACTIONS_BETWEEN_ADS = 3

    private var actionCount = 0

    fun startPreloading() {
        try {
            val adRequest = AdRequest.Builder(AD_UNIT_ID).build()
            InterstitialAdPreloader.start(AD_UNIT_ID, PreloadConfiguration(adRequest))
            Log.d(TAG, "Interstitial preloading started")
        } catch (e: Exception) {
            Log.e(TAG, "Interstitial preloading failed", e)
        }
    }

    fun incrementAction() {
        actionCount++
    }

    fun maybeShow(activity: Activity, onDismissedOrUnavailable: () -> Unit) {
        if (actionCount < MIN_ACTIONS_BETWEEN_ADS) {
            onDismissedOrUnavailable()
            return
        }

        val ad = InterstitialAdPreloader.pollAd(AD_UNIT_ID)
        if (ad == null) {
            Log.d(TAG, "Interstitial not available, continuing")
            onDismissedOrUnavailable()
            return
        }

        actionCount = 0
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial shown")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed")
                onDismissedOrUnavailable()
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                Log.w(TAG, "Interstitial failed to show: $error")
                onDismissedOrUnavailable()
            }

            override fun onAdImpression() {
                Log.d(TAG, "Interstitial impression recorded")
            }

            override fun onAdClicked() {
                Log.d(TAG, "Interstitial clicked")
            }
        }
        ad.show(activity)
    }
}
