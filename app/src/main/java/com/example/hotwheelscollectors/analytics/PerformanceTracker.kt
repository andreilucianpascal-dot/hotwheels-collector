package com.example.hotwheelscollectors.analytics

import android.content.Context
import android.os.SystemClock
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class PerformanceTracker private constructor(context: Context) {

    private val analyticsManager = AnalyticsManager.getInstance(context)
    private val metrics = ConcurrentHashMap<String, MetricData>()
    private val thresholds = ConcurrentHashMap<String, Long>()
    private val operationTimings = LruCache<String, MutableList<Long>>(100)

    fun startTracking(metricName: String) {
        metrics[metricName] = MetricData(
            startTime = SystemClock.elapsedRealtime(),
            checkpoints = mutableListOf()
        )
    }

    fun addCheckpoint(metricName: String, checkpointName: String) {
        metrics[metricName]?.let { metricData ->
            metricData.checkpoints.add(
                Checkpoint(
                    name = checkpointName,
                    timestamp = SystemClock.elapsedRealtime() - metricData.startTime
                )
            )
        }
    }

    fun stopTracking(
        metricName: String,
        attributes: Map<String, String>? = null
    ): MetricResult {
        val metricData = metrics.remove(metricName) ?: return MetricResult(0, emptyList())
        val endTime = SystemClock.elapsedRealtime()
        val duration = endTime - metricData.startTime

        val timings = operationTimings.get(metricName) ?: mutableListOf()
        timings.add(duration)
        if (timings.size > MAX_TIMING_SAMPLES) {
            timings.removeAt(0)
        }
        operationTimings.put(metricName, timings)

        analyticsManager.trackPerformanceMetric(
            metricName = metricName,
            value = duration,
            attributes = attributes
        )

        thresholds[metricName]?.let { threshold ->
            if (duration > threshold) {
                analyticsManager.trackPerformanceMetric(
                    metricName = "${metricName}_threshold_exceeded",
                    value = duration,
                    attributes = attributes
                )
            }
        }

        return MetricResult(duration, metricData.checkpoints)
    }

    fun setThreshold(metricName: String, thresholdMs: Long) {
        thresholds[metricName] = thresholdMs
    }

    suspend fun getMetricStats(metricName: String): MetricStats? = withContext(Dispatchers.Default) {
        operationTimings.get(metricName)?.let { timings ->
            if (timings.isEmpty()) return@withContext null

            val sorted = timings.sorted()
            MetricStats(
                count = timings.size,
                min = sorted.first(),
                max = sorted.last(),
                mean = timings.average(),
                median = sorted[sorted.size / 2],
                percentile95 = sorted[(sorted.size * 0.95).toInt()],
                percentile99 = sorted[(sorted.size * 0.99).toInt()]
            )
        }
    }

    suspend fun exportMetricsAsJson(): JSONObject = withContext(Dispatchers.Default) {
        JSONObject().apply {
            put("metrics", JSONObject().apply {
                operationTimings.snapshot().forEach { (metricName, timings) ->
                    put(metricName, JSONObject().apply {
                        getMetricStats(metricName)?.let { stats ->
                            put("count", stats.count)
                            put("min", stats.min)
                            put("max", stats.max)
                            put("mean", stats.mean)
                            put("median", stats.median)
                            put("95th_percentile", stats.percentile95)
                            put("99th_percentile", stats.percentile99)
                        }
                    })
                }
            })
            put("thresholds", JSONObject().apply {
                thresholds.forEach { (metricName, threshold) ->
                    put(metricName, threshold)
                }
            })
        }
    }

    private data class MetricData(
        val startTime: Long,
        val checkpoints: MutableList<Checkpoint>
    )

    data class Checkpoint(
        val name: String,
        val timestamp: Long
    )

    data class MetricResult(
        val duration: Long,
        val checkpoints: List<Checkpoint>
    )

    data class MetricStats(
        val count: Int,
        val min: Long,
        val max: Long,
        val mean: Double,
        val median: Long,
        val percentile95: Long,
        val percentile99: Long
    )

    companion object {
        private const val MAX_TIMING_SAMPLES = 1000

        @Volatile
        private var INSTANCE: PerformanceTracker? = null

        fun getInstance(context: Context): PerformanceTracker {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceTracker(context).also { INSTANCE = it }
            }
        }
    }
}