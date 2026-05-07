package com.othelloworld

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.updateBoardState
import java.lang.Thread.sleep

/**
 * Not sure if should make this completely stateless or not. For now, stateful but can support stateless patterns.
 * e.g. just instantiate a new object for each move.
 */
class GameOrchestrator(
    private var boardState: BoardState = STARTING_STATE
) {
    private val engine = Engine()
    private lateinit var gameStatus: GameStatus

    init {
        updateGameStatus()
    }

    fun makePlayerMove(move: Int): Pair<BoardState, GameStatus> {
        // validate move
        if (!engine.validateMove(boardState, move)) {
            throw InvalidMoveException(move)
        }

        // it's valid, make the move
        boardState = updateBoardState(boardState, move)
        updateGameStatus()
        return boardState to gameStatus
    }

    fun makeEngineMove(): Pair<BoardState, GameStatus> {
        // artificial delay for now
        sleep(500)

        val (updatedBoardState, _) = engine.makeMove(boardState)
        boardState = updatedBoardState
        updateGameStatus()
        return boardState to gameStatus
    }

    private fun updateGameStatus() {
        gameStatus = if (engine.isGameOver(boardState)) {
            engine.getOutcome(boardState)
        } else {
            GameStatus.ONGOING
        }
    }

    fun getBoardState() = boardState
    fun getGameStatus() = gameStatus
    fun validMoves(): List<Int> = engine.getNextPossibleMoves(boardState)
}