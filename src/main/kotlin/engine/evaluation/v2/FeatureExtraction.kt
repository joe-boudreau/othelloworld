package com.othelloworld.engine.evaluation.v2

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.evaluation.endProximity
import kotlinx.serialization.json.Json
import java.io.File

data class FeatureRow(
    val features: List<Double>,
    val phase: String,   // "early" | "mid" | "late"
    val outcome: Double  // filled in after the game ends
)

val SYMMETRY_CLASS = intArrayOf(
    0, 1, 1, 1, 1, 1, 1, 0,
    1, 2, 3, 4, 4, 3, 2, 1,
    1, 3, 5, 6, 6, 5, 3, 1,
    1, 4, 6, 7, 7, 6, 4, 1,
    1, 4, 6, 7, 7, 6, 4, 1,
    1, 3, 5, 6, 6, 5, 3, 1,
    1, 2, 3, 4, 4, 3, 2, 1,
    0, 1, 1, 1, 1, 1, 1, 0
)

val NUM_SYMMETRY_CLASSES = SYMMETRY_CLASS.distinct().size

fun BoardState.toFeatureVector(): List<Double> {
    // symmetry-folded
    val features = DoubleArray(NUM_SYMMETRY_CLASSES)
    for (i in 0..63) {
        val mask = 1L shl (63 - i)
        val value = when {
            blackPositions and mask != 0L -> 1.0
            whitePositions and mask != 0L -> -1.0
            else -> 0.0
        }
        features[SYMMETRY_CLASS[i]] += value
    }
    return features.toList()
}

fun BoardState.phaseBucket(): String = when {
    endProximity() < 0.33 -> "early"
    endProximity() < 0.66 -> "mid"
    else -> "late"
}