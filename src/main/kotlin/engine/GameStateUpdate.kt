package com.othelloworld.engine

/**
 * Updates the board state based on the move (0-63)
 */
fun updateGameState(gameState: GameState, move: Int): GameState {
    val moveBitboard = 1L shl move
    when(gameState.turn) {
        BLACK_TO_MOVE -> {
            val (newBlackPieces, newWhitePieces) = updateAllDirections(
                moveBitboard,
                gameState.blackPositions,
                gameState.whitePositions
            )
            return GameState(
                whitePositions = newWhitePieces,
                blackPositions = newBlackPieces,
                turn = WHITE_TO_MOVE
            )
        }
        else -> {
            val (newWhitePieces, newBlackPieces) = updateAllDirections(
                moveBitboard,
                gameState.whitePositions,
                gameState.blackPositions
            )
            return GameState(
                whitePositions = newWhitePieces,
                blackPositions = newBlackPieces,
                turn = BLACK_TO_MOVE
            )
        }
    }
}

/**
 * @return Pair of new piece bitboards <moving pieces, other pieces>
 */
private fun updateAllDirections(move: Long, movingPieces: Long, otherPieces: Long, ): Pair<Long, Long> {
    val (uMp, uOp) = updateUpDirection(move, movingPieces, otherPieces)
    val (dMp, dOp) = updateDownDirection(move, movingPieces, otherPieces)
    val (lMp, lOp) = updateLeftDirection(move, movingPieces, otherPieces)
    val (rMp, rOp) = updateRightDirection(move, movingPieces, otherPieces)
    val (ulMp, ulOp) = updateUpLeftDirection(move, movingPieces, otherPieces)
    val (urMp, urOp) = updateUpRightDirection(move, movingPieces, otherPieces)
    val (dlMp, dlOp) = updateDownLeftDirection(move, movingPieces, otherPieces)
    val (drMp, drOp) = updateDownRightDirection(move, movingPieces, otherPieces)

    // combine all the new piece positions, OR so overlap is fine
    // remember to add the move square to the moving piece new bitboard
    return (uMp or dMp or lMp or rMp or ulMp or urMp or dlMp or drMp or move) to
            (uOp or dOp or lOp or rOp or ulOp or urOp or dlOp or drOp)
}

private fun updateUpDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces, 0L) { it shl 8 }
private fun updateDownDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces, 0L) { it ushr 8 }
private fun updateLeftDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces,
    LEFT_COLUMN
) { it shl 1 }
private fun updateRightDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces,
    RIGHT_COLUMN
) { it ushr 1 }

private fun updateUpLeftDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces,
    LEFT_COLUMN
) { it shl 9 }
private fun updateUpRightDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces,
    RIGHT_COLUMN
) { it shl 7 }

private fun updateDownLeftDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces,
    LEFT_COLUMN
) { it ushr 7 }
private fun updateDownRightDirection(move: Long, movingPieces: Long, otherPieces: Long) = updateDirection(move, movingPieces, otherPieces,
    RIGHT_COLUMN
) { it ushr 9 }

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
        // update both piece boards if a piece is flipping
        newMovingPieces = newMovingPieces or currSq
        newOtherPieces = newOtherPieces and currSq.inv()
        // continue searching using same logic as before
        currSq = moveFn(currSq and ineligibleMask.inv()) and otherPieces
    }
    return newMovingPieces to newOtherPieces
}