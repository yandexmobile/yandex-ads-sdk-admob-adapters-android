/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2015 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */

package com.admob.mobileads;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitial;
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitialListener;
import com.yandex.mobile.ads.AdEventListener;
import com.yandex.mobile.ads.AdRequestError;
import com.yandex.mobile.ads.InterstitialAd;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class YandexInterstitial implements CustomEventInterstitial {

    private static final String TAG = "Yandex AdMob Adapter";

    private static final String BLOCK_ID = "blockID";
    private static final String OPEN_LINKS_IN_APP = "openLinksInApp";

    private String mBlockId;
    private boolean mOpenLinksInApp;

    private InterstitialAd mInterstitialAd;
    private CustomEventInterstitialListener mInterstitialListener;

    @Override
    public void requestInterstitialAd(final Context context,
                                      final CustomEventInterstitialListener customEventInterstitialListener,
                                      final String serverParameter,
                                      final MediationAdRequest mediationAdRequest,
                                      final Bundle adMobExtras) {

        mInterstitialListener = customEventInterstitialListener;
        if (mInterstitialListener == null) {
            Log.w(TAG, "customEventInterstitialListener must not be null");
            return;
        }

        if (isValidServerExtras(serverParameter)) {
            try {
                parseServerExtras(serverParameter);

                final com.yandex.mobile.ads.AdRequest adRequest = configureAdRequest(mediationAdRequest);

                mInterstitialAd = new InterstitialAd(context);
                mInterstitialAd.setBlockId(mBlockId);
                mInterstitialAd.shouldOpenLinksInApp(mOpenLinksInApp);
                mInterstitialAd.setAdEventListener(mInterstitialAdEventListener);

                mInterstitialAd.loadAd(adRequest);
            } catch (JSONException e) {
                mInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
            }
        } else {
            mInterstitialListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
        }
    }

    private static boolean isValidServerExtras(final String serverParameter) {
        try {
            final JSONObject serverExtras = new JSONObject(serverParameter);
            return !TextUtils.isEmpty(serverExtras.getString(BLOCK_ID));
        } catch (JSONException e) {
            return false;
        }
    }

    private void parseServerExtras(final String serverParameter) throws JSONException {
        final JSONObject serverExtras = new JSONObject(serverParameter);

        mBlockId = serverExtras.getString(BLOCK_ID);
        mOpenLinksInApp = Boolean.parseBoolean(serverExtras.optString(OPEN_LINKS_IN_APP));
    }

    private com.yandex.mobile.ads.AdRequest configureAdRequest(final MediationAdRequest mediationAdRequest) {
        final com.yandex.mobile.ads.AdRequest.Builder adRequestBuilder = com.yandex.mobile.ads.AdRequest.builder();
        if (mediationAdRequest != null) {
            final List<String> keywords = new ArrayList<>(mediationAdRequest.getKeywords());
            final Location location = mediationAdRequest.getLocation();

            adRequestBuilder.withContextTags(keywords);
            adRequestBuilder.withLocation(location);
        }

        return adRequestBuilder.build();
    }

    @Override
    public void showInterstitial() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            Log.d(TAG, "Tried to show a MobMetricaAds interstitial ad before it finished loading. Please try again.");
        }
    }

    private AdEventListener mInterstitialAdEventListener = new AdEventListener() {

        @Override
        public void onAdClosed() {
            mInterstitialListener.onAdClosed();
        }

        @Override
        public void onAdFailedToLoad(final AdRequestError adRequestError) {
            final int adMobErrorCode;
            switch (adRequestError.getCode()) {
                case AdRequestError.Code.INTERNAL_ERROR :
                case AdRequestError.Code.SYSTEM_ERROR :
                    adMobErrorCode = AdRequest.ERROR_CODE_INTERNAL_ERROR;
                    break;
                case AdRequestError.Code.INVALID_REQUEST :
                    adMobErrorCode = AdRequest.ERROR_CODE_INVALID_REQUEST;
                    break;
                case AdRequestError.Code.NETWORK_ERROR :
                    adMobErrorCode = AdRequest.ERROR_CODE_NETWORK_ERROR;
                    break;
                case AdRequestError.Code.NO_FILL :
                    adMobErrorCode = AdRequest.ERROR_CODE_NO_FILL;
                    break;
                default:
                    adMobErrorCode = AdRequest.ERROR_CODE_NO_FILL;
                    break;
            }

            mInterstitialListener.onAdFailedToLoad(adMobErrorCode);
        }

        @Override
        public void onAdLeftApplication() {
            mInterstitialListener.onAdLeftApplication();
        }

        @Override
        public void onAdLoaded() {
            mInterstitialListener.onAdLoaded();
        }

        @Override
        public void onAdOpened() {
            mInterstitialListener.onAdOpened();
            mInterstitialListener.onAdClicked();
        }
    };

    @Override
    public void onDestroy() {
        if (mInterstitialAd != null) {
            mInterstitialAd.destroy();
            mInterstitialAd = null;
        }
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }
}
