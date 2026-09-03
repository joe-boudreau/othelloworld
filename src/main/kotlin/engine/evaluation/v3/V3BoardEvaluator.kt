package com.othelloworld.engine.evaluation.v3

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.evaluation.BoardEvaluator
import com.othelloworld.engine.evaluation.endProximity
import com.othelloworld.engine.evaluation.pieceDiffScore
import kotlin.io.path.Path
import kotlin.io.path.readText
import com.othelloworld.engine.evaluation.simplePieceScore
import com.othelloworld.engine.evaluation.v2.phaseBucket
import com.othelloworld.engine.evaluation.v2.toFeatureVector
import kotlinx.serialization.json.Json

class V3BoardEvaluator(weightsFilePath: String): BoardEvaluator {

    // "early"/"mid"/"late" -> weights
    private val positionalWeightsByPhase: Map<String, DoubleArray> = run {
        val json = Path(weightsFilePath).readText()
        val raw: Map<String, List<Double>> = Json.decodeFromString(json)
        raw.mapValues { it.value.toDoubleArray() }
    }

    override fun evaluateBoard(board: BoardState, gameIsOver: Boolean?): Double {
        // Can override the end proximity calc if the caller knows the game is over due to no legal moves
        val gameIsOver = gameIsOver ?: (board.endProximity() == 1.0)

        return if (gameIsOver) {
            terminalBoardScore(board)
        } else {
            positionalScore(board)
        }
    }

    private fun terminalBoardScore(board: BoardState): Double {
        val diff = board.pieceDiffScore()
        return when {
            diff > 0 -> 1000.0 + diff
            diff < 0 -> -1000.0 + diff
            else -> 0.0
        }
    }

    private fun positionalScore(boardState: BoardState): Double {
        val weights = positionalWeightsByPhase[boardState.phaseBucket()] ?: error("weights not loaded")
        val features = boardState.toFeatureVector()
        return features.zip(weights.toList()).sumOf { (f, w) -> f * w }
    }
}