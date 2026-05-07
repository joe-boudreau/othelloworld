package com.othelloworld

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.updateBoardState

class GameOrchestrator(
    private var boardState: BoardState = STARTING_STATE
) {
    private val engine = Engine()
    private lateinit var gameStatus: GameStatus

    init {
        updateGameStatus()
    }

    fun makePlayerMove(move: Int) {
        // validate move
        if (!engine.validateMove(boardState, move)) {
            throw InvalidMoveException(move)
        }

        // it's valid, make the move
        boardState = updateBoardState(boardState, move)

        updateGameStatus()
    }

    fun makeEngineMove() {
        val (updatedBoardState, _) = engine.makeMove(boardState)
        boardState = updatedBoardState
        updateGameStatus()
    }

    private fun updateGameStatus(): Unit {
        if (engine.isGameOver(boardState)) {
            gameStatus = engine.getOutcome(boardState)
        }
    }

    fun getBoardState() = boardState
    fun getGameStatus() = gameStatus
}