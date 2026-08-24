package com.othelloworld.engine.algorithms

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus

class EpsilonGreedyWrapper(
    val internalSelectionAlgorithm: MoveSelectionAlgorithm,
    val epsilon: Double = 0.1
) : MoveSelectionAlgorithm {

    override val name = "epsGreedy(${internalSelectionAlgorithm.name})"

    private val randomSelection = Random()

    override fun selectMove(
        board: BoardState,
        gameStatus: GameStatus
    ): BoardState {
        val rand = kotlin.random.Random.nextDouble()
        return if (rand < epsilon) {
            randomSelection.selectMove(board, gameStatus)
        } else {
            internalSelectionAlgorithm.selectMove(board, gameStatus)
        }
    }
}