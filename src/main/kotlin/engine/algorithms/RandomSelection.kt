package com.othelloworld.engine.algorithms

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.exceptions.InvalidGameStatusException
import com.othelloworld.engine.getNextPossibleMoves
import com.othelloworld.engine.updateBoardState
import kotlin.random.Random

/**
 * Always selects a random move
 */
class RandomSelection(private val randomSource: Random = Random.Default): MoveSelectionAlgorithm {

    companion object {
        const val NAME = "random"
    }
    override val name = NAME

    override fun selectMove(board: BoardState, gameStatus: GameStatus): BoardState {
        val moves = getNextPossibleMoves(board, gameStatus.blackToMove())
        if (moves.isEmpty()) {
            throw InvalidGameStatusException(board, gameStatus)
        }
        val chosen = moves.random(randomSource)
        return updateBoardState(board, gameStatus.blackToMove(), chosen)
    }
}