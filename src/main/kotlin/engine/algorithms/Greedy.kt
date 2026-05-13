package com.othelloworld.engine.algorithms

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.exceptions.InvalidGameStatusException
import com.othelloworld.engine.getNextPossibleMoves
import com.othelloworld.engine.updateBoardState

/**
 * Always selects the move that flips the most number of pieces for the player to move.
 */
class Greedy: MoveSelectionAlgorithm {

    override val name: String = "greedy"

    override fun selectMove(board: BoardState, gameStatus: GameStatus): BoardState {
        val moves = getNextPossibleMoves(board, gameStatus.blackToMove())
        if (moves.isEmpty()) {
            throw InvalidGameStatusException(board, gameStatus)
        }
        val updatedBoardStates = moves.map { updateBoardState(board, gameStatus.blackToMove(), it) }
        return updatedBoardStates.maxByOrNull { if (gameStatus.blackToMove()) it.blackPieceCount else it.whitePieceCount }!!
    }
}