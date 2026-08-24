package com.othelloworld.engine.evaluation

import com.othelloworld.engine.BoardState

interface BoardEvaluator {

    fun evaluateBoard(board: BoardState, gameIsOver: Boolean? = null): Double
}