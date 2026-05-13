package com.othelloworld.engine.algorithms

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.exceptions.InvalidGameStatusException

interface MoveSelectionAlgorithm {

    val name: String

    /**
     * @throws InvalidGameStatusException
     */
    fun selectMove(board: BoardState, gameStatus: GameStatus): BoardState
}