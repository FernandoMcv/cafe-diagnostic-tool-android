package com.fcbc.tesis.data.repository

import android.os.Bundle
import com.fcbc.tesis.domain.repository.AnalyticsRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class AnalyticsRepositoryImpl(
    private val analytics: FirebaseAnalytics?,
    private val crashlytics: FirebaseCrashlytics?
) : AnalyticsRepository {

    override fun trackEvent(eventName: String, params: Map<String, String>) {
        try {
            val bundle = Bundle()
            for ((key, value) in params) {
                bundle.putString(key, value)
            }
            analytics?.logEvent(eventName, bundle)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun recordException(throwable: Throwable) {
        try {
            crashlytics?.recordException(throwable)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
