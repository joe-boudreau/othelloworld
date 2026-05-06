package com.othelloworld.engine

fun printBoard(board: Long) {
    for (rank in 7 downTo 0) {
        for (file in 7 downTo 0) {
            val bit = rank * 8 + file
            val isset = (board ushr bit) and 1L == 1L
            print(if (isset) "1 " else "0 ")
        }
        println()
    }
}

/**
 * Generates a game state from a board string of the form:
 * 0  0  0  0  0  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0  0  0  B  0  0  0  0
 * 0  0  B  W  B  0  0  0
 * 0  0  0  B  W  W  0  0
 * 0  0  0  0  B  0  0  0
 * 0  0  0  0  0  0  0  0
 * 0  0  0  0  0  0  0  0
 *
 * Spacing doesn't matter, the board string just needs 8 lines of 8 characters each
 * W = White, B = Black, 0 = Empty
 *
 */
fun generateGameStateFromBoardString(boardString: String, turn: Boolean = BLACK_TO_MOVE): GameState {
    val chars = boardString.filter { it == 'W' || it == 'B' || it == '0' }
    require(chars.length == 64) { "Board string must contain exactly 64 piece characters (W/B/0), got ${chars.length}" }

    var white = 0L
    var black = 0L
    for (i in 0 until 64) {
        val bit = 63 - i
        when (chars[i]) {
            'W' -> white = white or (1L shl bit)
            'B' -> black = black or (1L shl bit)
        }
    }
    return GameState(white, black, turn)
}