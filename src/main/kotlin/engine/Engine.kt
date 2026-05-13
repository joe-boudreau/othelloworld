package com.othelloworld.engine

import com.othelloworld.engine.exceptions.InvalidMoveException
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.algorithms.MoveSelectionAlgorithm

class Engine(private val moveSelectionAlgorithm: MoveSelectionAlgorithm) {

    fun getValidMoves(board: BoardState, gameStatus: GameStatus): List<Int> {
        if (gameStatus.isTerminal()) {
            return emptyList()
        }
        return getNextPossibleMoves(board, gameStatus.blackToMove())
    }

    fun makePlayerMove(board: BoardState, gameStatus: GameStatus, move: Int): Pair<BoardState, GameStatus> {
        // validate move
        if (!validateMove(board, gameStatus, move)) {
            throw InvalidMoveException(move)
        }

        val updatedBoardState = updateBoardState(board, gameStatus.blackToMove(), move)

        val updatedGameStatus = getNextGameStatus(updatedBoardState, gameStatus)

        return updatedBoardState to updatedGameStatus
    }

    fun makeEngineMove(board: BoardState, gameStatus: GameStatus): Pair<BoardState, GameStatus> {
        val chosenMoveBoardState = moveSelectionAlgorithm.selectMove(board, gameStatus)

        val updatedGameStatus = getNextGameStatus(chosenMoveBoardState, gameStatus)

        return chosenMoveBoardState to updatedGameStatus
    }

    private fun getNextGameStatus(updatedBoardState: BoardState, lastGameStatus: GameStatus): GameStatus {
        if (updatedBoardState.remainingMoves == 0) {
            return getGameOverOutcome(updatedBoardState)
        }

        val potentialNextStatus = if (lastGameStatus.blackToMove()) WHITE_TO_MOVE else BLACK_TO_MOVE
        val potentialNextMoves = getNextPossibleMoves(updatedBoardState, potentialNextStatus.blackToMove())

        if (potentialNextMoves.isNotEmpty()) {
            return potentialNextStatus
        }

        // The next player can't move, so it's either passing to the other player or the game is over.
        val isBlackNextNextToMove = !potentialNextStatus.blackToMove()
        val potentialNextNextMoves = getNextPossibleMoves(updatedBoardState, isBlackNextNextToMove)

        // Neither player can move next, so the game is over.
        if (potentialNextNextMoves.isEmpty()) {
            return getGameOverOutcome(updatedBoardState)
        } else {
            return if (isBlackNextNextToMove) {
                BLACK_TO_MOVE_WHITE_PASSING
            } else {
                WHITE_TO_MOVE_BLACK_PASSING
            }
        }
    }

    private fun getGameOverOutcome(board: BoardState): GameStatus {
        return if (board.whitePieceCount > board.blackPieceCount) {
            WHITE_WINS
        } else if (board.blackPieceCount > board.whitePieceCount) {
            BLACK_WINS
        } else {
            DRAW
        }
    }

    private fun validateMove(board: BoardState, gameStatus: GameStatus, move: Int): Boolean {
        if (gameStatus.isTerminal()) {
            return false
        }
        val moves = getNextPossibleMoves(board, gameStatus.blackToMove())
        return move in moves
    }
}