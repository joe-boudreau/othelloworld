package com.othelloworld.engine

/**
 * Score is between -1 and 1
 * 1 is win for black
 * -1 is win for white
 *
 * ep = end proximity = (60 - remaining moves / 60)
 * start of game: ep = (60 - 60) / 60 = 0
 * end of game: ep = (60 - 0) / 60 = 1
 *
 * eval function = (ep) * (simple piece score) + (1 - ep) * (heuristic eval)
 *
 * simple piece score = 1 if black pieces > white pieces, -1 if white pieces > black pieces, 0 otherwise
 *
 * heuristic eval = weighted mix of heuristic eval functions (see below)
 */
fun evaluateBoard(board: BoardState): Double {
    val ep = board.endProximity()
    return (ep * board.simplePieceScore()) + ((1 - ep) * board.heuristicEval())
}

fun BoardState.endProximity(): Double {
    return (60 - this.remainingMoves) / 60.0
}

fun BoardState.simplePieceScore(): Double {
    return if (this.blackPieceCount > this.whitePieceCount) 1.0 else if (this.whitePieceCount > this.blackPieceCount) -1.0 else 0.0
}

/**
 * the weights need to always sum to 1
 */
val heuristicEvalWeightsAndFunctions = listOf(
    0.4 to BoardState::pieceDiffScore,
    0.6 to BoardState::positionalScore,
)

fun BoardState.heuristicEval(): Double {
    return heuristicEvalWeightsAndFunctions.sumOf { (weight, function) -> weight * function(this) }
}

fun BoardState.pieceDiffScore(): Double {
    // max diff would be 64
    return (this.blackPieceCount - this.whitePieceCount) / 64.0
}

fun BoardState.positionalScore(): Double {
    return this.blackPositions.positionalScore() - this.whitePositions.positionalScore()
}

//fun BoardState.availableMovesScore(): Double {
//    val blackMoves = getAllMoves(this.blackPositions, this.whitePositions).countOneBits()
//    val whiteMoves = getAllMoves(this.whitePositions, this.blackPositions).countOneBits()
//    // ... not sure about this one yet and if it's useful
//}



fun PiecePositions.positionalScore(): Double {
    var score = 0
    for (i in 0..63) {
        if (this and (1L shl (63-i)) != 0L) {
            score += POSITIONAL_SCORE_WEIGHTS[i]
        }
    }
    return score / maxPositionalScore
}

val POSITIONAL_SCORE_WEIGHTS = intArrayOf(
    10, 5, 5, 5, 5, 5, 5, 10,
    5, 3, 3, 3, 3, 3, 3, 5,
    5, 3, 2, 2, 2, 2, 3, 5,
    5, 3, 2, 1, 1, 2, 3, 5,
    5, 3, 2, 1, 1, 2, 3, 5,
    5, 3, 2, 2, 2, 2, 3, 5,
    5, 3, 3, 3, 3, 3, 3, 5,
    10, 5, 5, 5, 5, 5, 5, 10
    )
val maxPositionalScore = POSITIONAL_SCORE_WEIGHTS.sum().toDouble()

/**
 *  * 63 62 61 60 59 58 57 56
 *  * 55 54 53 52 51 50 49 48
 *  * 47 46 45 44 43 42 41 40
 *  * 39 38 37 36 35 34 33 32
 *  * 31 30 29 28 27 26 25 24
 *  * 23 22 21 20 19 18 17 16
 *  * 15 14 13 12 11 10  9  8
 *  *  7  6  5  4  3  2  1  0
 *  *
 *  * where the 64-bits in the Long represent the following:
 *  * MSb [63, 62, 61 ... 0] LSb
 */