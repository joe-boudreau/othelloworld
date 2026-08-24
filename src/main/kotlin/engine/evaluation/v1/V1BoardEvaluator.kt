package com.othelloworld.engine.evaluation.v1

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.PiecePositions
import com.othelloworld.engine.evaluation.BoardEvaluator
import com.othelloworld.engine.evaluation.endProximity
import com.othelloworld.engine.evaluation.pieceDiffScore
import com.othelloworld.engine.evaluation.simplePieceScore

/**
 * Score is between -1 and 1
 * 1 is win for black
 * -1 is win for white
 *
 * ep = end proximity = (60 - remaining moves / 60)
 * start of game: ep = (60 - 60) / 60 = 0
 * end of game: ep = (60 - 0) / 60 = 1
 *
 * eval function = (ep) * (simple piece score) + (1 - ep) * (heuristic eval)
 *
 * simple piece score = 1 if black pieces > white pieces, -1 if white pieces > black pieces, 0 otherwise
 *
 * heuristic eval = weighted mix of heuristic eval functions (see below)
 */

class V1BoardEvaluator: BoardEvaluator {

    override fun evaluateBoard(board: BoardState, gameIsOver: Boolean?): Double {
        // Can override the end proximity calc if the caller knows the game is over due to no legal moves
        val ep = if (gameIsOver ?: false) 1.0 else board.endProximity()
        return (ep * board.simplePieceScore()) + ((1 - ep) * board.heuristicEval())
    }

    private fun BoardState.heuristicEval(): Double {
        return heuristicEvalWeightsAndFunctions.sumOf { (weight, function) -> weight * function(this) }
    }

    /**
     * the weights need to always sum to 1
     */
    private val heuristicEvalWeightsAndFunctions = listOf(
        0.4 to BoardState::pieceDiffScore,
        0.6 to this::staticPositionalScore,
    )

    private fun staticPositionalScore(boardState: BoardState): Double {
        return boardState.blackPositions.positionalScore() - boardState.whitePositions.positionalScore()
    }

    private fun PiecePositions.positionalScore(): Double {
        var score = 0
        for (i in 0..63) {
            if (this and (1L shl (63-i)) != 0L) {
                score += POSITIONAL_SCORE_WEIGHTS[i]
            }
        }
        return score / maxPositionalScore
    }

    private val POSITIONAL_SCORE_WEIGHTS = intArrayOf(
        10, 5, 5, 5, 5, 5, 5, 10,
        5, 3, 3, 3, 3, 3, 3, 5,
        5, 3, 2, 2, 2, 2, 3, 5,
        5, 3, 2, 1, 1, 2, 3, 5,
        5, 3, 2, 1, 1, 2, 3, 5,
        5, 3, 2, 2, 2, 2, 3, 5,
        5, 3, 3, 3, 3, 3, 3, 5,
        10, 5, 5, 5, 5, 5, 5, 10
    )

    private val maxPositionalScore = POSITIONAL_SCORE_WEIGHTS.sum().toDouble()
}