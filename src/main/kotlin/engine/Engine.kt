package com.othelloworld.engine

class Engine {

    fun makeMove(gameState: GameState): GameState {
        val allMoves = when(gameState.turn) {
            WHITE_TO_MOVE -> getAllMoves(gameState.whitePositions, gameState.blackPositions)
            else -> getAllMoves(gameState.whitePositions, gameState.blackPositions)
        }

        val moves = mutableListOf<Int>()
        for (i in 0 until 64) {
            if ((allMoves and (1L shl i)) != 0L) {
                moves.add(i)
            }
        }
        val updatedGameStates = moves.map { updateGameState(gameState, it) }

        // Algorithm 1: pick a random move
        return updatedGameStates.random()
    }
}