package com.othelloworld.engine.evaluation.v3

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.evaluation.BoardEvaluator
import com.othelloworld.engine.evaluation.pieceDiffScore
import kotlin.io.path.Path
import kotlin.io.path.readText
import com.othelloworld.engine.evaluation.v2.mapWeightsToSquareWeight
import com.othelloworld.engine.evaluation.v2.phaseBucket
import kotlinx.serialization.json.Json

class V3BoardEvaluator(weightsFilePath: String): BoardEvaluator {

    // "early"/"mid"/"late" -> weight of each square (Array of 64 doubles)
    private val positionalWeightsByPhase: Map<String, DoubleArray> = run {
        val json = Path(weightsFilePath).readText()
        val raw: Map<String, List<Double>> = Json.decodeFromString(json)
        raw.mapValues { mapWeightsToSquareWeight(it.value) }
    }

    override fun evaluateBoard(board: BoardState, gameIsOver: Boolean?): Double {
        // Can override the end proximity calc if the caller knows the game is over due to no legal moves
        val gameIsOver = gameIsOver ?: (board.remainingMoves == 0)

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
        val squareWeights = positionalWeightsByPhase[boardState.phaseBucket()] ?: error("weights not loaded")
        var score = 0.0
        for (sq in 0..63) {
            val mask = 1L shl (63 - sq)
            val value = when {
                boardState.blackPositions and mask != 0L -> 1.0
                boardState.whitePositions and mask != 0L -> -1.0
                else -> 0.0
            }
            score += squareWeights[sq] * value
        }
        return score
    }
}