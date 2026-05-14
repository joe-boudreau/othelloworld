package com.othelloworld.engine.algorithms

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.evaluateBoard
import com.othelloworld.engine.exceptions.InvalidGameStatusException
import com.othelloworld.engine.getNextPossibleMoves
import com.othelloworld.engine.updateBoardState

class NegamaxSearch(private val depth: Int = 7): MoveSelectionAlgorithm {

    override val name = "negamax"
    private var nodesSearched = 0

    /**
     * function negamax(node, depth, color) is
     *     if depth = 0 or node is a terminal node then
     *         return color × the heuristic value of node
     *     value := −∞
     *     for each child of node do
     *         value := max(value, −negamax(child, depth − 1, −color))
     *     return value
     */
    override fun selectMove(board: BoardState, gameStatus: GameStatus): BoardState {
        nodesSearched = 0 // reset
        val (bestMove, bestMoveScore) = getBestMove(board, gameStatus, depth)

        if (bestMove == -1) {
            throw InvalidGameStatusException(board, gameStatus)
        }

        // debug log
        val color = if (gameStatus.blackToMove()) "Black" else "White"
        println("Moving piece: $color,  Best Move: $bestMove, Score: $bestMoveScore")
        println("Nodes searched: $nodesSearched")

        return updateBoardState(board, gameStatus.blackToMove(), bestMove)
    }

    private fun getBestMove(
        board: BoardState,
        gameStatus: GameStatus,
        depth: Int,
    ): Pair<Int, Double> {
        nodesSearched++
        val blackToMove = gameStatus.blackToMove()
        val color = if (blackToMove) 1 else -1

        if (depth == 0 || board.remainingMoves == 0) {
            return -1 to color * evaluateBoard(board)
        }

        val moves = getNextPossibleMoves(board, blackToMove)
        if (moves.isEmpty()) {
            if (gameStatus.previousPlayerPassed()) {
                // if the previous player passed, and the current player has no moves either, then the game is over
                return -1 to color * evaluateBoard(board, gameIsOver = true)
            }

            val newGameStatus = if (blackToMove) WHITE_TO_MOVE_BLACK_PASSING else BLACK_TO_MOVE_WHITE_PASSING
            val (_, score) = getBestMove(board, newGameStatus, depth)
            return -1 to -1 * score
        }

        var bestMoveAndScore = -1 to Double.NEGATIVE_INFINITY
        for (move in moves) {
            val updatedBoardState = updateBoardState(board, gameStatus.blackToMove(), move)
            val nextStatus = if (blackToMove) WHITE_TO_MOVE else BLACK_TO_MOVE
            val (_, score) = getBestMove(updatedBoardState, nextStatus, depth - 1)

            val currentMoverScore = -1 * score // the current player's motive is to minimize the max score the other player can achieve
            if (currentMoverScore > bestMoveAndScore.second) {
                bestMoveAndScore = move to currentMoverScore
            }
        }
        return bestMoveAndScore
    }
}

/**
 * Normal way it works
 *
 * black to move
 *
 * move, score after making that move = getBestmove()
 *
 * fun getBestMove(board, player) {
 *
 * all moves = getNextPossibleMoves(board, player)
 *
 * for each move in allMoves {
 *     updated board = updateBoardState(board, player, move)
 *     score = getBestMove(updatedBoard, player) // this is the best possible score the other player can get IF you make this move
 *     // so we multiply by -1 to
 * }
 *
 * }
 *
 */