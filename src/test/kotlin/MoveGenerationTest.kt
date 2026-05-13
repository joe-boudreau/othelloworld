package com.othelloworld

import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.generateBoardStateFromBoardString
import com.othelloworld.engine.getAllMoves
import com.othelloworld.engine.printBoard
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
    fun `test move generation starting state`() {
        val gameState = STARTING_STATE
        val blackMoves = getAllMoves(gameState.blackPositions, gameState.whitePositions)
        println("Black moves: ")
        printBoard(blackMoves)
    }

    @Test
    fun `test move generation`() {
        val gameState = generateBoardStateFromBoardString(
            """
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  W  W  0  W  W  0
0  0  0  0  W  0  0  0
0  0  0  W  W  0  0  0
0  0  0  W  0  0  0  0
0  0  0  W  0  0  0  0
            """
        )
        val blackMoves = getAllMoves(gameState.blackPositions, gameState.whitePositions)
        println("Black moves: ")
        printBoard(blackMoves)
    }
}