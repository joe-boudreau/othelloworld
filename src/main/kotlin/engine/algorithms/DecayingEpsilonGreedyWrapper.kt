package com.othelloworld.engine.algorithms

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import kotlin.math.pow

class DecayingEpsilonGreedyWrapper(
    val internalSelectionAlgorithm: MoveSelectionAlgorithm,
    val initialEpsilon: Double = 0.1,
    val floorEpsilon: Double = 0.0,
    val epsilonDecayFactor: Double = 0.65,
    randomSeed: Long = 123456789L
) : MoveSelectionAlgorithm {

    override val name = "epsGreedy(${internalSelectionAlgorithm.name})"

    private val rng = kotlin.random.Random(randomSeed)

    private val randomSelectionAlgorithm = RandomSelection(rng)

    override fun selectMove(
        board: BoardState,
        gameStatus: GameStatus
    ): BoardState {
        val rand = rng.nextDouble()

        // divide by 2 to make it equally random between both players
        val moveNumber = board.turnNumber / 2
        val epsilon = floorEpsilon + ((initialEpsilon-floorEpsilon) * epsilonDecayFactor.pow(moveNumber))

        return if (rand < epsilon) {
            randomSelectionAlgorithm.selectMove(board, gameStatus)
        } else {
            internalSelectionAlgorithm.selectMove(board, gameStatus)
        }
    }
}