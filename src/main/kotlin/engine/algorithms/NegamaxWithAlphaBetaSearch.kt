package com.othelloworld.engine.algorithms

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.evaluation.BoardEvaluator
import com.othelloworld.engine.exceptions.InvalidGameStatusException
import com.othelloworld.engine.getNextPossibleMoves
import com.othelloworld.engine.updateBoardState

class NegamaxWithAlphaBetaSearch(searchDepth: Int, private val boardEvaluator: BoardEvaluator): MoveSelectionAlgorithm {

    companion object {
        const val NAME = "negamax-alpha-beta"
    }
    override val name = NAME

    private val initDepth = searchDepth
    private var nodesSearched = 0

    override fun selectMove(board: BoardState, gameStatus: GameStatus): BoardState {
        nodesSearched = 0 // reset
        val (bestMove, bestMoveScore) = getBestMove(board, gameStatus)

        if (bestMove == -1) {
            throw InvalidGameStatusException(board, gameStatus)
        }

        // debug log
        val color = if (gameStatus.blackToMove()) "Black" else "White"
        //println("Moving piece: $color,  Best Move: $bestMove, Score: $bestMoveScore")
        //println("Nodes searched: $nodesSearched")

        return updateBoardState(board, gameStatus.blackToMove(), bestMove)
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
        depth: Int = initDepth,
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