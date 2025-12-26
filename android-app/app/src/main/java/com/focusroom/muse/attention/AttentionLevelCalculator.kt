package com.focusroom.muse.attention

import com.focusroom.muse.model.AttentionResult
import com.focusroom.muse.model.Channel
import com.focusroom.muse.model.RawEegSample
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Very small, real-time friendly feature extractor that estimates attention level from raw EEG.
 *
 * The Muse raw stream arrives at 256Hz. Instead of a full FFT-based bandpower estimate
 * (which is heavier on mobile), this class tracks two envelope features per channel:
 * - lowFrequencyEnergy: a smoothed absolute amplitude that approximates low-frequency power
 * - fastChangeEnergy: a smoothed first derivative magnitude that approximates beta/gamma activity
 *
 * The attention score is the ratio between fast/slow envelopes with an exponential squashing
 * to keep the value in [0,1]. The discrete level can be mapped to 3 or 5 buckets.
 */
class AttentionLevelCalculator(
    private val smoothingFactor: Double = 0.12,
    private val levelCount: Int = 3,
) {
    private var previousSample: RawEegSample? = null
    private var lowEnvelope: Double = 0.0
    private var fastEnvelope: Double = 0.0

    fun process(sample: RawEegSample): AttentionResult {
        val previous = previousSample
        previousSample = sample

        val channelValues = sample.channelsMicrovolts.values
        if (channelValues.isEmpty()) {
            return AttentionResult(
                normalizedScore = 0.0,
                discreteLevel = 0,
                lowFrequencyEnergy = 0.0,
                fastChangeEnergy = 0.0,
            )
        }

        val meanAmplitude = channelValues.sumOf { abs(it) } / channelValues.size
        val derivative = if (previous != null) {
            val diffs = Channel.values().mapNotNull { channel ->
                val current = sample.channelsMicrovolts[channel] ?: return@mapNotNull null
                val prev = previous.channelsMicrovolts[channel] ?: return@mapNotNull null
                abs(current - prev)
            }
            if (diffs.isNotEmpty()) diffs.sum() / diffs.size else 0.0
        } else {
            0.0
        }

        // Exponential smoothing to maintain real-time stability.
        lowEnvelope = (1 - smoothingFactor) * lowEnvelope + smoothingFactor * meanAmplitude
        fastEnvelope = (1 - smoothingFactor) * fastEnvelope + smoothingFactor * derivative

        val ratio = fastEnvelope / (lowEnvelope + 1e-3)
        val normalized = squashToUnit(ratio)
        val level = quantize(normalized)

        return AttentionResult(
            normalizedScore = normalized,
            discreteLevel = level,
            lowFrequencyEnergy = lowEnvelope,
            fastChangeEnergy = fastEnvelope,
        )
    }

    private fun squashToUnit(value: Double): Double {
        // A log-exp squashing that keeps useful contrast in [0,1].
        val safe = max(value, 1e-5)
        val contracted = 1.0 - exp(-ln(1.0 + safe))
        return min(1.0, max(0.0, contracted))
    }

    private fun quantize(score: Double): Int {
        val buckets = levelCount.coerceAtLeast(2)
        val clamped = min(1.0, max(0.0, score))
        val level = (clamped * buckets).toInt()
        return level.coerceIn(0, buckets - 1)
    }
}
