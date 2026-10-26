package com.pet.memories.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd

object AdManager {
    private const val TAG = "AdManager"

    // TEST AD UNIT IDS ONLY (Mandatory as per specification)
    const val BANNER_TEST_ID = "ca-app-pub-9771662959477951/7989689040"
    const val INTERSTITIAL_TEST_ID = "ca-app-pub-9771662959477951/9855762494"
    const val NATIVE_ADVANCED_TEST_ID = "ca-app-pub-9771662959477951/5147903286"

    private var interstitialAd: InterstitialAd? = null

    fun initialize(context: Context) {
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "AdMob Initialized: \$status")
            loadInterstitial(context)
        }
    }

    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_TEST_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onAdDismissed: (() -> Unit)? = null) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onAdDismissed?.invoke()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onAdDismissed?.invoke()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onAdDismissed?.invoke()
            loadInterstitial(activity)
        }
    }

    fun loadNativeAd(context: Context, onLoaded: (NativeAd) -> Unit) {
        val adLoader = AdLoader.Builder(context, NATIVE_ADVANCED_TEST_ID)
            .forNativeAd { nativeAd ->
                onLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.message}")
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }
}
