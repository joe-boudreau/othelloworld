package com.othelloworld.engine.evaluation.v2

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.evaluation.BoardEvaluator
import com.othelloworld.engine.evaluation.endProximity
import com.othelloworld.engine.evaluation.pieceDiffScore
import kotlin.io.path.Path
import kotlin.io.path.readText
import com.othelloworld.engine.evaluation.simplePieceScore
import kotlinx.serialization.json.Json

class V2BoardEvaluator(weightsFilePath: String): BoardEvaluator {

    // "early"/"mid"/"late" -> weights
    private val positionalWeightsByPhase: Map<String, DoubleArray> = run {
        val json = Path(weightsFilePath).readText()
        val raw: Map<String, List<Double>> = Json.decodeFromString(json)
        raw.mapValues { it.value.toDoubleArray() }
    }

    override fun evaluateBoard(board: BoardState, gameIsOver: Boolean?): Double {
        // Can override the end proximity calc if the caller knows the game is over due to no legal moves
        val ep = if (gameIsOver ?: false) 1.0 else board.endProximity()
        return (ep * board.simplePieceScore()) + ((1 - ep) * board.heuristicEval())
    }

    private fun BoardState.heuristicEval(): Double {
        return heuristicEvalWeightsAndFunctions.sumOf { (weight, function) -> weight * function(this) }
    }

    // for V2, keeping these static
    private val heuristicEvalWeightsAndFunctions = listOf(
        0.4 to BoardState::pieceDiffScore,
        0.6 to this::positionalScore,
    )

    fun positionalScore(boardState: BoardState): Double {
        val weights = positionalWeightsByPhase[boardState.phaseBucket()] ?: error("weights not loaded")
        val features = boardState.toFeatureVector()
        return features.zip(weights.toList()).sumOf { (f, w) -> f * w }
    }
}