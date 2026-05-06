package com.othelloworld.engine

typealias Square = Int // 0 to 63

typealias PiecePositions = Long
typealias WhitePositions = PiecePositions
typealias BlackPositions = PiecePositions

const val WHITE_TO_MOVE = true
const val BLACK_TO_MOVE = false

data class GameState(
    val whitePositions: WhitePositions,
    val blackPositions: BlackPositions,
    val turn: Boolean,
) {

    fun whitePieceCount(): Int = whitePositions.countOneBits()
    fun blackPieceCount(): Int = blackPositions.countOneBits()
    fun turnNumber() = whitePieceCount() + blackPieceCount() - 4 // 4 is the number of pieces in the center to start
    fun remainingMoves() = 64 - whitePieceCount() - blackPieceCount()

    fun print() {
        println("Turn #: ${turnNumber()}")
        println("Black piece count: ${blackPieceCount()}")
        println("White piece count: ${whitePieceCount()}")
        println("Remaining moves: ${remainingMoves()}")
        println("To Move: ${if (turn == WHITE_TO_MOVE) "White" else "Black"}")
        for (rank in 7 downTo 0) {
            for (file in 7 downTo 0) {
                val bit = rank * 8 + file
                val char = when {
                    (whitePositions ushr bit) and 1L == 1L -> "W"
                    (blackPositions ushr bit) and 1L == 1L -> "B"
                    else -> "0"
                }
                print(if (file == 0) char else "$char  ")
            }
            println()
        }
    }
}

/*
Notation:

One long represents 64 pieces. Each bit represents if a piece is present at that position.

Everything is based off the following bitboard representation:

63 62 61 60 59 58 57 56
55 54 53 52 51 50 49 48
47 46 45 44 43 42 41 40
39 38 37 36 35 34 33 32
31 30 29 28 27 26 25 24
23 22 21 20 19 18 17 16
15 14 13 12 11 10  9  8
 7  6  5  4  3  2  1  0

where the 64-bits in the Long represent the following:
MSb [63, 62, 61 .. 0] LSb

Examples:
White starting position:
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  0  1  0  0  0  0
0  0  0  0  1  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0b00000000_00000000_00000000_00010000_00001000_00000000_00000000_00000000L

Black starting position:
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  1  0  0  0
0  0  0  1  0  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0b00000000_00000000_00000000_00001000_00010000_00000000_00000000_00000000L
*/

const val WHITE_STARTING_POSITION = 0b00000000_00000000_00000000_00010000_00001000_00000000_00000000_00000000L
const val BLACK_STARTING_POSITION = 0b00000000_00000000_00000000_00001000_00010000_00000000_00000000_00000000L

val STARTING_STATE = GameState(
    WHITE_STARTING_POSITION,
    BLACK_STARTING_POSITION,
    BLACK_TO_MOVE
)

