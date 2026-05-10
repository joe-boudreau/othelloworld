package com.othelloworld.engine

const val CENTER_4 = 0b00000000_00000000_00000000_00011000_00011000_00000000_00000000_00000000L
const val LEFT_COLUMN = 0b1000000_10000000_10000000_10000000_10000000_10000000_10000000_10000000L
const val RIGHT_COLUMN = 0b00000001_00000001_00000001_00000001_00000001_00000001_00000001_00000001L

/*

W/B - piece positions
w/b - valid moves

0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  0  b  0  0  0  0
0  0  b  W  B  0  0  0
0  0  0  B  W  b  0  0
0  0  0  0  b  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0

need to get a mask that isolates all the squares adjacent to a square
just make 64 masks?

adjacency mask for square 36
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  1  1  1  0  0  0
0  0  1  0  1  0  0  0
0  0  1  1  1  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0  0  0  0  0  0  0  0
0b00000000_00000000_00111000_00101000_00111000_00000000_00000000_00000000L

what are we looking for?
- unoccupied adjacent square
- a {moving colour} piece in the opposite direction of the unoccupied square
- search in {direction} until:
    - if {non-moving colour} piece, continue search
    - if {moving colour} piece, return TRUE
    - if unoccupied or reached edge of board, return FALSE
- search in all directions until
    - one direction returns TRUE
    - all are FALSE


*/

/*
63 62 61 60 59 58 57 56
55 54 53 52 51 50 49 48
47 46 45 44 43 42 41 40
39 38 37 36 35 34 33 32
31 30 29 28 27 26 25 24
23 22 21 20 19 18 17 16
15 14 13 12 11 10  9  8
 7  6  5  4  3  2  1  0
 */

fun getAllMoves(movingPieces: PiecePositions, otherPieces: PiecePositions): Long =
    getUpMoves(movingPieces, otherPieces) or
    getDownMoves(movingPieces, otherPieces) or
    getLeftMoves(movingPieces, otherPieces) or
    getRightMoves(movingPieces, otherPieces) or
    getUpLeftMoves(movingPieces, otherPieces) or
    getUpRightMoves(movingPieces, otherPieces) or
    getDownLeftMoves(movingPieces, otherPieces) or
    getDownRightMoves(movingPieces, otherPieces)

fun getUpMoves(movingPieces: PiecePositions, otherPieces: PiecePositions) = getMoves(movingPieces, otherPieces, 0L) { it shl 8 }
fun getDownMoves(movingPieces: PiecePositions, otherPieces: PiecePositions) = getMoves(movingPieces, otherPieces, 0L) { it ushr 8 }
fun getLeftMoves(movingPieces: PiecePositions, otherPieces: PiecePositions) = getMoves(movingPieces, otherPieces, LEFT_COLUMN) { it shl 1 }
fun getRightMoves(movingPieces: PiecePositions, otherPieces: PiecePositions) = getMoves(movingPieces, otherPieces, RIGHT_COLUMN) { it ushr 1 }

fun getUpLeftMoves(movingPieces: PiecePositions, otherPieces: PiecePositions) = getMoves(movingPieces, otherPieces, LEFT_COLUMN) { it shl 9 }
fun getUpRightMoves(movingPieces: PiecePositions, otherPieces: PiecePositions) = getMoves(movingPieces, otherPieces, RIGHT_COLUMN) { it shl 7 }

fun getDownLeftMoves(movingPieces: PiecePositions, otherPieces: PiecePositions) = getMoves(movingPieces, otherPieces, LEFT_COLUMN) { it ushr 7 }
fun getDownRightMoves(movingPieces: PiecePositions, otherPieces: PiecePositions) = getMoves(movingPieces, otherPieces, RIGHT_COLUMN) { it ushr 9 }

fun getMoves(movingPieces: PiecePositions, otherPieces: PiecePositions, ineligibleMask: Long, moveFn: (Long) -> Long): Long {
    val unoccupied = (movingPieces or otherPieces).inv()
    var eligible = movingPieces and ineligibleMask.inv()
    eligible = moveFn(eligible)
    eligible = eligible and otherPieces

    var validMoves = 0L
    while (eligible != 0L) {
        eligible = eligible and ineligibleMask.inv() // remove squares that are ineligible from the next move (e.g. leftmost column when shifting left)
        eligible = moveFn(eligible)
        validMoves = validMoves or (eligible and unoccupied) // add the valid squares
        eligible = eligible and otherPieces
    }
    return validMoves
}

fun PiecePositions.isOccupied(square: Int) = (this and (1L shl square)) != 0L

