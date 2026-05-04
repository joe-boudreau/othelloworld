package com.othelloworld

const val CENTER_4 = 0b00000000_00000000_00000000_00011000_00011000_00000000_00000000_00000000L

/**
 * Doesn't exactly check for whether the position is possible to reach, not sure if that's possible
 * This is mostly a sanity check for during development and debugging to catch obvious errors
 */
fun isValidGameState(gameState: GameState): Boolean {
    val white = gameState.whitePositions
    val whitePieceCount = white.countOneBits()
    val black = gameState.blackPositions
    val blackPieceCount = black.countOneBits()
    val blackTurn = gameState.turn == BLACK_TO_MOVE

    // 1. are any white and black pieces on the same square?
    if (white and black != 0L) return false
    // 2. are any pieces not adjacent to each other? (any islands)
    // todo
    // 3. are any of the 4 center squares empty?
    if ((white or black) and CENTER_4 != CENTER_4) return false
    // 4. if black turn, does white pieces == black pieces?
    if (blackTurn && blackPieceCount != whitePieceCount) return false
    // 5. if white turn, does black pieces == white pieces + 1?
    if (!blackTurn && blackPieceCount != whitePieceCount+1) return false

    return true
}

