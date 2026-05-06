package com.othelloworld

class Engine {

    fun chooseMove(gameState: GameState): Int {
        val allMoves = when(gameState.turn) {
            WHITE_TO_MOVE -> getAllMoves(gameState.whitePositions, gameState.blackPositions)
            else -> getAllMoves(gameState.whitePositions, gameState.blackPositions)
        }

    }
}