package com.othelloworld

import kotlin.test.*

class MoveGenerationTest {

    @Test
    fun `test starting state`() {
        val gameState = STARTING_STATE
        println("\nStarting state:")
        println("Black pieces: ")
        printBoard(gameState.blackPositions)
        println("White pieces: ")
        printBoard(gameState.whitePositions)
        println()
    }

    @Test
    fun `test is valid game state`() {
        assertTrue(isValidGameState(STARTING_STATE))
    }

    @Test
    fun `test move generation starting state`() {
        val gameState = STARTING_STATE
        val blackMoves = getAllMoves(gameState.blackPositions, gameState.whitePositions)
        println("Black moves: ")
        printBoard(blackMoves)
    }

    @Test
    fun `test move generation`() {
        val gameState = generateGameStateFromBoardString(
            """
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  W  W  0  W  W  0
0  0  0  0  W  0  0  0
0  0  0  W  W  0  0  0
0  0  0  W  0  0  0  0
0  0  0  W  0  0  0  0
            """,
            BLACK_TO_MOVE
        )
        val blackMoves = getAllMoves(gameState.blackPositions, gameState.whitePositions)
        println("Black moves: ")
        printBoard(blackMoves)
    }
}