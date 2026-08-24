package com.othelloworld.engine.evaluation

import com.othelloworld.engine.BoardState

fun BoardState.endProximity(): Double {
    return (60 - this.remainingMoves) / 60.0
}

fun BoardState.simplePieceScore(): Double {
    return if (this.blackPieceCount > this.whitePieceCount) 1.0 else if (this.whitePieceCount > this.blackPieceCount) -1.0 else 0.0
}

fun BoardState.pieceDiffScore(): Double {
    // max diff would be 64
    return (this.blackPieceCount - this.whitePieceCount) / 64.0
}