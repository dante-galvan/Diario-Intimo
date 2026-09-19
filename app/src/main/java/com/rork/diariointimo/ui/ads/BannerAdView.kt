package com.rork.diariointimo.ui.ads

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BannerAdView(
    adUnitId: String = AdMobConfig.BANNER_AD_UNIT_ID,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    var bannerAd by remember { mutableStateOf<BannerAd?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current

    LaunchedEffect(adUnitId) {
        if (isPreview) return@LaunchedEffect
        // Wait for SDK init with backoff (max ~5s)
        repeat(10) {
            if (AdMobInitializer.isInitialized) return@LaunchedEffect
            delay(500)
        }
        // Destroy old ad before loading new one
        bannerAd?.destroy()
        bannerAd = null
        val screenWidth = context.resources.displayMetrics.widthPixels
        val adSize = AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, screenWidth)
        coroutineScope.launch {
            when (val result = BannerAd.load(BannerAdRequest.Builder(adUnitId, adSize).build())) {
                is AdLoadResult.Success -> {
                    Log.d("AdMob", "Banner loaded")
                    bannerAd = result.ad
                }
                is AdLoadResult.Failure -> {
                    Log.w("AdMob", "Banner failed: ${result.error}")
                    bannerAd = null
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bannerAd?.destroy()
            bannerAd = null
        }
    }

    bannerAd?.let { ad ->
        Box(modifier = modifier.fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.wrapContentSize(),
                factory = { ad.getView(activity) }
            )
        }
    }
}
