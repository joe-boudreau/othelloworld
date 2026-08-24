package com.othelloworld.engine.evaluation.v2

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.evaluation.BoardEvaluator
import com.othelloworld.engine.evaluation.endProximity
import com.othelloworld.engine.evaluation.pieceDiffScore
import com.othelloworld.engine.evaluation.simplePieceScore

class V2BoardEvaluator: BoardEvaluator {

    init {
        Weights.load("weights/weights_v1.json")
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
        val weights = Weights.positional[boardState.phaseBucket()] ?: error("weights not loaded")
        val features = boardState.toFeatureVector()
        return features.zip(weights.toList()).sumOf { (f, w) -> f * w }
    }
}