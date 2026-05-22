package com.othelloworld

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import kotlinx.serialization.Serializable

@Serializable
data class PlayerMoveRequest(
    val whitePositions: Long,
    val blackPositions: Long,
    val status: GameStatus,
    val square: Int,
    val algorithm: String? = null,
)

@Serializable
data class ComputerMoveRequest(
    val whitePositions: Long,
    val blackPositions: Long,
    val status: GameStatus,
    val algorithm: String? = null,
)

@Serializable
data class GameStateResponse(
    val whitePositions: Long,
    val blackPositions: Long,
    val status: GameStatus,
    val algorithm: String,
    val validMoves: List<Int>,
    val lastMove: Int? = null,
    val flippedSquares: List<Int> = emptyList(),
    val whitePieceCount: Int,
    val blackPieceCount: Int,
    val turnNumber: Int,
) {
    companion object {
        fun from(
            board: BoardState,
            status: GameStatus,
            algorithm: String,
            validMoves: List<Int>,
            lastMove: Int? = null,
            flippedSquares: List<Int> = emptyList(),
        ) = GameStateResponse(
            whitePositions = board.whitePositions,
            blackPositions = board.blackPositions,
            status = status,
            algorithm = algorithm,
            validMoves = validMoves,
            lastMove = lastMove,
            flippedSquares = flippedSquares,
            whitePieceCount = board.whitePieceCount,
            blackPieceCount = board.blackPieceCount,
            turnNumber = board.turnNumber,
        )
    }
}

@Serializable
data class ErrorResponse(val error: String)
