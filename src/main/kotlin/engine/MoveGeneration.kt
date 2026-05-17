package com.othelloworld.engine

const val LEFT_COLUMN = 0b1000000_10000000_10000000_10000000_10000000_10000000_10000000_10000000L
const val RIGHT_COLUMN = 0b00000001_00000001_00000001_00000001_00000001_00000001_00000001_00000001L

fun getNextPossibleMoves(board: BoardState, blackToMove: Boolean): List<Int> {
    val allMoves =
        if (blackToMove) {
            getAllMoves(board.blackPositions, board.whitePositions)
        } else {
            getAllMoves(board.whitePositions, board.blackPositions)
        }

    val moves = mutableListOf<Int>()
    for (i in 0 until 64) {
        if ((allMoves and (1L shl i)) != 0L) {
            moves.add(i)
        }
    }
    return moves
}

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
