package com.othelloworld.engine

/**
 * Updates the board state based on a move (0-63)
 */
fun updateBoardState(board: BoardState, blackToMove: Boolean, move: Int): BoardState {
    val moveBitboard = 1L shl move

    return if (blackToMove) {
        val (newBlackPieces, newWhitePieces) = updateAllDirections(
            moveBitboard,
            board.blackPositions,
            board.whitePositions
        )
        BoardState(whitePositions = newWhitePieces, blackPositions = newBlackPieces)
    }
    else {
        val (newWhitePieces, newBlackPieces) = updateAllDirections(
            moveBitboard,
            board.whitePositions,
            board.blackPositions
        )
        BoardState(whitePositions = newWhitePieces, blackPositions = newBlackPieces)
    }
}

/**
 * @return Pair of new piece bitboards <moving pieces, other pieces>
 */
private fun updateAllDirections(move: Long, movingPieces: Long, otherPieces: Long): Pair<Long, Long> {
    val (uMp, uOp) = updateUpDirection(move, movingPieces, otherPieces)
    val (dMp, dOp) = updateDownDirection(move, movingPieces, otherPieces)
    val (lMp, lOp) = updateLeftDirection(move, movingPieces, otherPieces)
    val (rMp, rOp) = updateRightDirection(move, movingPieces, otherPieces)
    val (ulMp, ulOp) = updateUpLeftDirection(move, movingPieces, otherPieces)
    val (urMp, urOp) = updateUpRightDirection(move, movingPieces, otherPieces)
    val (dlMp, dlOp) = updateDownLeftDirection(move, movingPieces, otherPieces)
    val (drMp, drOp) = updateDownRightDirection(move, movingPieces, otherPieces)

    /*
    For the moving piece, we OR all the directional changes together because each bitboard has either the same pieces or MORE
    So, to persist any of the pieces gained in a specific direction, we OR them all
    Finally, we OR with the single-bit move bitboard to include the piece that was placed during the move
     */
    val newMovingPiecePositions = uMp or dMp or lMp or rMp or ulMp or urMp or dlMp or drMp or move

    /*
    For the opponent pieces, we AND all the directional changes together because each bitboard has either the same pieces or LESS
    So, to persist any of the pieces removed in a specific direction, we AND them all
     */
    val newOtherPiecePositions = uOp and dOp and lOp and rOp and ulOp and urOp and dlOp and drOp

    return newMovingPiecePositions to newOtherPiecePositions
}

private fun updateUpDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces, 0L) { it shl 8 }
private fun updateDownDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces, 0L) { it ushr 8 }
private fun updateLeftDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces, LEFT_COLUMN) { it shl 1 }
private fun updateRightDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces, RIGHT_COLUMN) { it ushr 1 }

private fun updateUpLeftDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces, LEFT_COLUMN) { it shl 9 }
private fun updateUpRightDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces, RIGHT_COLUMN) { it shl 7 }

private fun updateDownLeftDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces, LEFT_COLUMN) { it ushr 7 }
private fun updateDownRightDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces, RIGHT_COLUMN) { it ushr 9 }

private fun updateDirection(
    move: Long,
    movingPieces: Long,
    otherPieces: Long,
    ineligibleMask: Long,
    moveFn: (Long) -> Long
): Pair<Long, Long> {
    // move is a single bit. we mask it out if its on the leftmost column for a leftward move e.g.
    // then we move it, and check if it overlaps with an enemy piece. bit will still be set if all those conditions are met
    var currSq = moveFn(move and ineligibleMask.inv()) and otherPieces

    var newMovingPieces = movingPieces
    var newOtherPieces = otherPieces
    while (currSq != 0L) {
        // update both piece boards if a piece is flipping (potentially)
        newMovingPieces = newMovingPieces or currSq
        newOtherPieces = newOtherPieces and currSq.inv()

        // move to the next square
        currSq = moveFn(currSq and ineligibleMask.inv())

        // found a "sandwiching" piece for the moving colour to make it a valid flip direction
        if (currSq and movingPieces != 0L) return newMovingPieces to newOtherPieces

        // otherwise, keep moving only if it's a piece of the other colour
        currSq = currSq and otherPieces
    }

    // we exited without finding a "sandwiching" piece, so this is not a valid flip direction
    return movingPieces to otherPieces
}