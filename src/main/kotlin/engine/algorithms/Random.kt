package com.othelloworld.engine.algorithms

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.exceptions.InvalidGameStatusException
import com.othelloworld.engine.getNextPossibleMoves
import com.othelloworld.engine.updateBoardState

/**
 * Always selects a random move
 */
class Random: MoveSelectionAlgorithm {

    override val name: String = "random"

    override fun selectMove(board: BoardState, gameStatus: GameStatus): BoardState {
        val moves = getNextPossibleMoves(board, gameStatus.blackToMove())
        if (moves.isEmpty()) {
            throw InvalidGameStatusException(board, gameStatus)
        }
        val chosen = moves.random()
        return updateBoardState(board, gameStatus.blackToMove(), chosen)
    }
}