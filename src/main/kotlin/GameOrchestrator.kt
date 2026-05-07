package com.othelloworld

import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.updateBoardState

class GameOrchestrator {

    private val engine = Engine()
    private var boardState = STARTING_STATE
    private var gameStatus = ONGOING

    fun makePlayerMove(move: Int) {
        // validate move
        if (!engine.validateMove(boardState, move)) {
            throw InvalidMoveException(move)
        }

        // it's valid, make the move
        boardState = updateBoardState(boardState, move)

        if (engine.isGameOver(boardState)) {
            gameStatus = engine.getOutcome(boardState)
        }
    }

    fun makeEngineMove() {
        val (updatedBoardState, _) = engine.makeMove(boardState)
        boardState = updatedBoardState
        if (engine.isGameOver(boardState)) {
            gameStatus = engine.getOutcome(boardState)
        }
    }

    fun getBoardState() = boardState
    fun getGameStatus() = gameStatus
}