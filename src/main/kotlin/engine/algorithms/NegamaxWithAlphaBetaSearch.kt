package com.othelloworld.engine.algorithms

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.evaluation.BoardEvaluator
import com.othelloworld.engine.exceptions.InvalidGameStatusException
import com.othelloworld.engine.getNextPossibleMoves
import com.othelloworld.engine.updateBoardState
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

class NegamaxWithAlphaBetaSearch(
    private val searchDepth: Int,
    private val boardEvaluator: BoardEvaluator,
    private val temperature: Double = 0.0,
    randomSeed: Long = 123456789L,
): MoveSelectionAlgorithm {

    companion object {
        const val NAME = "negamax-alpha-beta"
    }
    override val name = NAME

    init {
        require(temperature >= 0.0 && temperature.isFinite()) {
            "temperature must be finite and non-negative"
        }
    }

    private val rng = Random(randomSeed)
    private var nodesSearched = 0

    override fun selectMove(board: BoardState, gameStatus: GameStatus): BoardState {
        nodesSearched = 0

        val (bestMove, _) = if (temperature == 0.0) {
            getBestMove(board, gameStatus)
        } else {
            chooseRootMoveWithTemperature(board, gameStatus)
        }

        if (bestMove == -1) {
            throw InvalidGameStatusException(board, gameStatus)
        }

        return updateBoardState(board, gameStatus.blackToMove(), bestMove)
    }

    /**
     * Scores every root move, then samples one with softmax. Deeper search nodes
     * remain deterministic.
     *
     * Alpha is shared across root moves so that moves which cannot beat the best
     * score found so far can be cut off early. Consequently, scores after the
     * first move can be fail-soft upper bounds rather than exact minimax scores.
     * That trade-off preserves alpha-beta's performance while remaining suitable
     * for an exploratory self-play policy.
     *
     * Root scores are standardized within the current position before applying
     * temperature. This makes temperature dimensionless, so multiplying or
     * shifting an evaluator's scores does not change the move probabilities.
     */
    private fun chooseRootMoveWithTemperature(
        board: BoardState,
        gameStatus: GameStatus,
    ): Pair<Int, Double> {
        if (searchDepth == 0 || board.remainingMoves == 0) {
            return -1 to Double.NEGATIVE_INFINITY
        }

        val blackToMove = gameStatus.blackToMove()
        val nextStatus = if (blackToMove) WHITE_TO_MOVE else BLACK_TO_MOVE
        var rootAlpha = Double.NEGATIVE_INFINITY
        val moveScores = getNextPossibleMoves(board, blackToMove)
            .sortedByDescending(this::moveSorter)
            .map { move ->
                val updatedBoardState = updateBoardState(board, blackToMove, move)
                val (_, opponentScore) = getBestMove(
                    board = updatedBoardState,
                    gameStatus = nextStatus,
                    alpha = Double.NEGATIVE_INFINITY,
                    beta = -rootAlpha,
                    depth = searchDepth - 1,
                )
                val currentMoverScore = -opponentScore
                rootAlpha = maxOf(rootAlpha, currentMoverScore)
                move to currentMoverScore
            }

        if (moveScores.isEmpty()) {
            return -1 to Double.NEGATIVE_INFINITY
        }
        if (moveScores.size == 1) {
            return moveScores.single()
        }

        require(moveScores.all { (_, score) -> score.isFinite() }) {
            "board evaluator must return finite scores when temperature is enabled"
        }

        val scores = moveScores.map { it.second }

        // Scaling first avoids overflow while calculating variance for evaluators
        // whose scores have a large (but still finite) magnitude.
        val largestMagnitude = scores.maxOf(::abs)
        val scaledScores = if (largestMagnitude == 0.0) {
            scores
        } else {
            scores.map { it / largestMagnitude }
        }
        val mean = scaledScores.average()
        val standardDeviation = sqrt(
            scaledScores.sumOf { score ->
                val deviation = score - mean
                deviation * deviation
            } / scaledScores.size
        )

        val logits = if (standardDeviation == 0.0) {
            List(moveScores.size) { 0.0 }
        } else {
            val denominator = standardDeviation * temperature
            if (denominator == 0.0) {
                // A positive temperature can still underflow to zero. At that
                // point its intended behavior is indistinguishable from T = 0.
                val bestScore = scaledScores.max()
                scaledScores.map { if (it == bestScore) 0.0 else Double.NEGATIVE_INFINITY }
            } else {
                val bestScore = scaledScores.max()
                scaledScores.map { (it - bestScore) / denominator }
            }
        }

        // Subtracting the largest logit above keeps every exponential in [0, 1],
        // which makes the softmax numerically stable even for small temperatures.
        val weights = logits.map(::exp)
        val sample = rng.nextDouble() * weights.sum()
        var cumulativeWeight = 0.0
        for (index in moveScores.indices) {
            cumulativeWeight += weights[index]
            if (sample < cumulativeWeight) {
                return moveScores[index]
            }
        }

        // Protect against the final cumulative sum rounding down by a few ulps.
        return moveScores.last()
    }

    /**
     * function negamax(node, depth, α, β, color) is
     *     if depth = 0 or node is a terminal node then
     *         return color × the heuristic value of node
     *
     *     childNodes := generateMoves(node)
     *     childNodes := orderMoves(childNodes)
     *     value := −∞
     *     foreach child in childNodes do
     *         value := max(value, −negamax(child, depth − 1, −β, −α, −color))
     *         α := max(α, value)
     *         if α ≥ β then
     *             break (* cut-off *)
     *     return value
     */
    private fun getBestMove(
        board: BoardState,
        gameStatus: GameStatus,
        alpha: Double = Double.NEGATIVE_INFINITY,
        beta: Double = Double.POSITIVE_INFINITY,
        depth: Int = searchDepth,
    ): Pair<Int, Double> {
        nodesSearched++
        val blackToMove = gameStatus.blackToMove()
        val color = if (blackToMove) 1 else -1

        if (depth == 0 || board.remainingMoves == 0) {
            return -1 to color * boardEvaluator.evaluateBoard(board)
        }

        val moves = getNextPossibleMoves(board, blackToMove)
        val orderedMoves = moves.sortedByDescending(this::moveSorter)
        if (orderedMoves.isEmpty()) {
            if (gameStatus.previousPlayerPassed()) {
                // if the previous player passed, and the current player has no moves either, then the game is over
                return -1 to color * boardEvaluator.evaluateBoard(board, gameIsOver = true)
            }

            val newGameStatus = if (blackToMove) WHITE_TO_MOVE_BLACK_PASSING else BLACK_TO_MOVE_WHITE_PASSING
            val (_, score) = getBestMove(board, newGameStatus, -beta, -alpha, depth)
            return -1 to -1 * score
        }

        var bestMoveAndScore = -1 to Double.NEGATIVE_INFINITY
        var newAlpha = alpha
        for (move in orderedMoves) {
            val updatedBoardState = updateBoardState(board, gameStatus.blackToMove(), move)
            val nextStatus = if (blackToMove) WHITE_TO_MOVE else BLACK_TO_MOVE
            val (_, score) = getBestMove(updatedBoardState, nextStatus, -beta, -newAlpha, depth - 1)
            val currentMoverScore = -1 * score // the current player's motive is to minimize the max score the other player can achieve
            if (currentMoverScore > bestMoveAndScore.second) {
                bestMoveAndScore = move to currentMoverScore
            }

            newAlpha = maxOf(newAlpha, currentMoverScore)
            if (newAlpha >= beta) {
                break
            }

        }
        return bestMoveAndScore
    }


    /**
     * basic sorting by distance from board center
     * notation
     * 63 62 61 60 59 58 57 56
     * 55 54 53 52 51 50 49 48
     * 47 46 45 44 43 42 41 40
     * 39 38 37 36 35 34 33 32
     * 31 30 29 28 27 26 25 24
     * 23 22 21 20 19 18 17 16
     * 15 14 13 12 11 10  9  8
     *  7  6  5  4  3  2  1  0
     *
     * empty
     * 4  3  3  3  3  3  3  4
     * 3  2  2  2  2  2  2  3
     * 3  2  1  1  1  1  2  3
     * 3  2  1  0  0  1  2  3
     * 3  2  1  0  0  1  2  3
     * 3  2  1  1  1  1  2  3
     * 3  2  2  2  2  2  2  3
     * 4  3  3  3  3  3  3  4
     */
    private fun moveSorter(move: Int) = squareSortingScore[move]

    private val squareSortingScore = intArrayOf(
        4, 3, 3, 3, 3, 3, 3, 4,
        3, 2, 2, 2, 2, 2, 2, 3,
        3, 2, 1, 1, 1, 1, 2, 3,
        3, 2, 1, 0, 0, 1, 2, 3,
        3, 2, 1, 0, 0, 1, 2, 3,
        3, 2, 1, 1, 1, 1, 2, 3,
        3, 2, 2, 2, 2, 2, 2, 3,
        4, 3, 3, 3, 3, 3, 3, 4,
    )
}
