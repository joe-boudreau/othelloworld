package com.othelloworld

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.evaluation.v2.V3_FEATURE_COUNT
import com.othelloworld.engine.evaluation.v2.toFeatureVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class V3FeatureExtractionTest {
    @Test
    fun `v3 feature vector preserves colour-swap symmetry`() {
        val swappedStartingState = BoardState(
            whitePositions = STARTING_STATE.blackPositions,
            blackPositions = STARTING_STATE.whitePositions,
        )

        val features = STARTING_STATE.toFeatureVector(blackToMove = true)
        val swappedFeatures = swappedStartingState.toFeatureVector(blackToMove = false)

        assertEquals(V3_FEATURE_COUNT, features.size)
        features.indices.forEach { index ->
            assertTrue(kotlin.math.abs(features[index] + swappedFeatures[index]) < 1e-12)
        }
    }

    @Test
    fun `v3 feature vector distinguishes the side to move`() {
        val blackToMove = STARTING_STATE.toFeatureVector(blackToMove = true)
        val whiteToMove = STARTING_STATE.toFeatureVector(blackToMove = false)

        assertEquals(blackToMove.take(8), whiteToMove.take(8))
        assertEquals(-blackToMove[8], whiteToMove[8])
        (9 until V3_FEATURE_COUNT).forEach { index ->
            assertEquals(-blackToMove[index], whiteToMove[index])
        }
    }
}
