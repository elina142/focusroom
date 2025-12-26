package com.focusroom.muse.model

/**
 * Raw EEG sample captured from the Muse headset. All values are in microvolts.
 */
data class RawEegSample(
    val timestampMillis: Long,
    val channelsMicrovolts: Map<Channel, Double>,
)

enum class Channel {
    TP9, AF7, AF8, TP10, AUX,
}

/**
 * Lightweight feature summary used by the attention pipeline.
 */
data class AttentionResult(
    val normalizedScore: Double,
    val discreteLevel: Int,
    val lowFrequencyEnergy: Double,
    val fastChangeEnergy: Double,
)
