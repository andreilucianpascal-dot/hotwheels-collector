package com.example.hotwheelscollectors.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class AnalyticsManager private constructor(private val context: Context) {

    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics
    private val eventBuffer = ConcurrentHashMap<String, MutableList<Bundle>>()
    private val maxBufferSize = 100

    fun trackScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        bufferEvent("screen_view", bundle)
    }

    fun trackUserAction(
        action: String,
        category: String,
        label: String? = null,
        value: Long? = null
    ) {
        val bundle = Bundle().apply {
            putString("action", action)
            putString("category", category)
            label?.let { putString("label", it) }
            value?.let { putLong("value", it) }
        }
        firebaseAnalytics.logEvent("user_action", bundle)
        bufferEvent("user_action", bundle)
    }

    fun trackCollectionEvent(
        eventType: CollectionEventType,
        carId: String,
        additionalParams: Map<String, Any>? = null
    ) {
        val bundle = Bundle().apply {
            putString("event_type", eventType.name)
            putString("car_id", carId)
            additionalParams?.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        firebaseAnalytics.logEvent("collection_event", bundle)
        bufferEvent("collection_event", bundle)
    }

    fun trackSearch(
        query: String,
        resultCount: Int,
        filters: Map<String, Any>? = null
    ) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SEARCH_TERM, query)
            putInt("result_count", resultCount)
            filters?.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        bufferEvent("search", bundle)
    }

    fun trackPerformanceMetric(
        metricName: String,
        value: Long,
        attributes: Map<String, String>? = null
    ) {
        val bundle = Bundle().apply {
            putString("metric_name", metricName)
            putLong("value", value)
            attributes?.forEach { (key, value) ->
                putString(key, value)
            }
        }
        firebaseAnalytics.logEvent("performance_metric", bundle)
        bufferEvent("performance_metric", bundle)
    }

    private fun bufferEvent(eventType: String, bundle: Bundle) {
        eventBuffer.getOrPut(eventType) { mutableListOf() }.apply {
            add(bundle)
            if (size > maxBufferSize) {
                removeAt(0)
            }
        }
    }

    suspend fun getAnalyticsData(eventType: String): List<JSONObject> = withContext(Dispatchers.Default) {
        eventBuffer[eventType]?.map { bundle ->
            JSONObject().apply {
                bundle.keySet().forEach { key ->
                    put(key, bundle.get(key))
                }
            }
        } ?: emptyList()
    }

    fun clearBuffer() {
        eventBuffer.clear()
    }

    enum class CollectionEventType {
        CAR_ADDED,
        CAR_REMOVED,
        CAR_UPDATED,
        PHOTO_ADDED,
        PHOTO_REMOVED
    }

    companion object {
        @Volatile
        private var INSTANCE: AnalyticsManager? = null

        fun getInstance(context: Context): AnalyticsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnalyticsManager(context).also { INSTANCE = it }
            }
        }
    }
}