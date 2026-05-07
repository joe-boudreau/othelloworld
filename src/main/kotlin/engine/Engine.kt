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

        val chosenMoveBoardState = chooseMove(moves, board)

        /**
         * The design decision I've made here is to always return ONGOING as the status even if the game
         * could be over at this point.
         * We could check here if there are no more moves left for the other player, or if the board is filled.
         * I'm deciding to not go with that approach for now so we don't have to calculate the possible moves twice
         * every time this function is called.
         *
         * This is purely for efficiency for when makeMove() is called recursively during engine search.
         *
         * The game orchestrator parent code will still explicitly check the outcome of the game after every "real" move.
         */
        return chosenMoveBoardState to ONGOING
    }

    /**
     * Where the magic will inevitably happen.
     */
    private fun chooseMove(
        moves: List<Int>,
        board: BoardState
    ): BoardState {
        val updatedBoardStates = moves.map { updateBoardState(board, it) }

        // algorithm 1: pick a random move
        val chosenMoveBoardState = updatedBoardStates.random()
        return chosenMoveBoardState
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
}