package com.othelloworld.engine

typealias PiecePositions = Long
typealias WhitePositions = PiecePositions
typealias BlackPositions = PiecePositions

const val WHITE_STARTING_POSITION = 0b00000000_00000000_00000000_00010000_00001000_00000000_00000000_00000000L
const val BLACK_STARTING_POSITION = 0b00000000_00000000_00000000_00001000_00010000_00000000_00000000_00000000L
val STARTING_STATE = BoardState(WHITE_STARTING_POSITION, BLACK_STARTING_POSITION)

/**
 * Represents the current state of the board which is simply where the pieces currently are.
 * This class does NOT contain information about the status of the game (ongoing, won, lost, etc).
 * Even though the game status can be inferred from the state of the board,
 * those calculations are the responsibility of the engine since it involves computing possible moves.
 *
 * Notation:
 *
 * One Long represents all 64 squares. Each bit represents if a piece is present at that position.
 *
 * Everything is based off the following bitboard representation:
 *
 * 63 62 61 60 59 58 57 56
 * 55 54 53 52 51 50 49 48
 * 47 46 45 44 43 42 41 40
 * 39 38 37 36 35 34 33 32
 * 31 30 29 28 27 26 25 24
 * 23 22 21 20 19 18 17 16
 * 15 14 13 12 11 10  9  8
 *  7  6  5  4  3  2  1  0
 *
 * where the 64-bits in the Long represent the following:
 * MSb [63, 62, 61 ... 0] LSb
 *
 * Examples:
 * White starting position:
 * 0  0  0  0  0  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0  0  0  1  0  0  0  0
 * 0  0  0  0  1  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0b00000000_00000000_00000000_00010000_00001000_00000000_00000000_00000000L
 *
 * Black starting position:
 * 0  0  0  0  0  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0  0  0  0  1  0  0  0
 * 0  0  0  1  0  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0b00000000_00000000_00000000_00001000_00010000_00000000_00000000_00000000L
 *
 *
 * @param whitePositions The positions of all white pieces on the board
 * @param blackPositions The positions of all black pieces on the board
 */
data class BoardState(
    val whitePositions: WhitePositions,
    val blackPositions: BlackPositions,
) {

    val whitePieceCount: Int = whitePositions.countOneBits()
    val blackPieceCount: Int = blackPositions.countOneBits()
    val turnNumber: Int = whitePieceCount + blackPieceCount - 4 // 4 is the number of pieces in the centre to start
    val remainingMoves: Int = 64 - whitePieceCount - blackPieceCount

    fun print(withMetadata: Boolean = true) {
        if (withMetadata) {
            println("Turn #: $turnNumber")
            println("Black piece count: $blackPieceCount")
            println("White piece count: $whitePieceCount")
            println("Remaining moves: $remainingMoves")
        }
        for (rank in 7 downTo 0) {
            for (file in 7 downTo 0) {
                val bit = rank * 8 + file
                val char = when {
                    (whitePositions ushr bit) and 1L == 1L -> "W"
                    (blackPositions ushr bit) and 1L == 1L -> "B"
                    else -> " " // don't show empty squares
                    //else -> "0"
                }
                print(if (file == 0) char else "$char  ")
            }
            println()
        }
    }
}

