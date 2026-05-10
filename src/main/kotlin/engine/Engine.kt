package com.othelloworld.engine

import com.othelloworld.engine.GameStatus.*

class Engine {

    fun getOutcome(board: BoardState): GameStatus {
        return if (board.whitePieceCount > board.blackPieceCount) {
            WHITE_WINS
        } else if (board.blackPieceCount > board.whitePieceCount) {
            BLACK_WINS
        } else {
            DRAW
        }
    }

    fun isGameOver(board: BoardState): Boolean {
        val moves = getNextPossibleMoves(board)
        return moves.isEmpty() || board.remainingMoves == 0
    }

    fun validateMove(board: BoardState, move: Int): Boolean {
        val moves = getNextPossibleMoves(board)
        return move in moves
    }

    fun getNextPossibleMoves(board: BoardState): List<Int> {
        val allMoves =
            if (board.blackToMove) {
                getAllMoves(board.blackPositions, board.whitePositions)
            } else {
                getAllMoves(board.whitePositions, board.blackPositions)
            }

        val moves = mutableListOf<Int>()
        for (i in 0 until 64) {
            if ((allMoves and (1L shl i)) != 0L) {
                moves.add(i)
            }
        }
        return moves
    }

    fun makeMove(board: BoardState): Pair<BoardState, GameStatus> {

        val moves = getNextPossibleMoves(board)

        /**
         * Game Over!
         */
        if (moves.isEmpty() || board.remainingMoves == 0) {
            // Technically the 2nd condition isn't needed as moves will be empty if the board is filled, but including it for clarity
            val gameStatus = getOutcome(board)
            return board to gameStatus
        }

        //val chosenMoveBoardState = chooseMoveRandom(moves, board)
        //val chosenMoveBoardState = chooseMoveGreedy(moves, board)
        val chosenMoveBoardState = chooseMoveNegamax(board)

        /**
         * The design decision I've made here is to always return ONGOING as the status even if the game
         * could be over at this point.
         * We could check here if there are no more moves left for the other player, or if the board is filled.
         * I'm deciding to not go with that approach for now so we don't have to calculate the possible moves twice
         * every time this function is called.
         *
         * This is purely for efficiency for when makeMove() is called recursively during engine search.
         * TODO: Could change this later by introducing an in-memory cache of possible moves. investigate.
         *
         * The game orchestrator parent code will still explicitly check the outcome of the game after every "real" move.
         */
        return chosenMoveBoardState to ONGOING
    }

    /**
     * Algorithm 1: Random
     */
    private fun chooseMoveRandom(
        moves: List<Int>,
        board: BoardState
    ): BoardState {
        val updatedBoardStates = moves.map { updateBoardState(board, it) }

        // algorithm 1: pick a random move
        val chosenMoveBoardState = updatedBoardStates.random()
        return chosenMoveBoardState
    }

    /**
     * Algorithm 2: Greedy
     */
    private fun chooseMoveGreedy(
        moves: List<Int>,
        board: BoardState,
    ): BoardState {
        val blackToMove = board.blackToMove
        val updatedBoardStates = moves.map { updateBoardState(board, it) }

        return updatedBoardStates.maxByOrNull { if (blackToMove) it.blackPieceCount else it.whitePieceCount }!!
    }

    /**
     * function negamax(node, depth, color) is
     *     if depth = 0 or node is a terminal node then
     *         return color × the heuristic value of node
     *     value := −∞
     *     for each child of node do
     *         value := max(value, −negamax(child, depth − 1, −color))
     *     return value
     */
    private fun chooseMoveNegamax(board: BoardState): BoardState {
        val (bestMove, bestMoveScore) = getBestMove(board, 5)
        // debug
        val color = if (board.blackToMove) "Black" else "White"
        println("Moving piece: $color,  Best Move: $bestMove, Score: $bestMoveScore")
        return updateBoardState(board, bestMove)
    }

    private fun getBestMove(
        board: BoardState,
        depth: Int,
    ): Pair<Int, Double> {
        val color = if (board.blackToMove) 1 else -1
        if (depth == 0 || board.remainingMoves == 0) {
            return -1 to color * evaluateBoard(board)
        }

        val moves = getNextPossibleMoves(board)
        if (moves.isEmpty()) { // Don't want to check this upfront just to save on some computation in leaf and endgame nodes
            return -1 to color * evaluateBoard(board)
        }

        var bestMoveAndScore = -1 to Double.NEGATIVE_INFINITY
        for (move in moves) {
            val updatedBoardState = updateBoardState(board, move)
            val (_, score) = getBestMove(updatedBoardState, depth - 1)
            val currentMoverScore = -1 * score
            if (currentMoverScore > bestMoveAndScore.second) {
                bestMoveAndScore = move to currentMoverScore
            }
        }
        return bestMoveAndScore
    }
}