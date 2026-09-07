package com.othelloworld

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus.BLACK_TO_MOVE
import com.othelloworld.engine.GameStatus.WHITE_TO_MOVE
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.algorithms.NegamaxWithAlphaBetaSearch
import com.othelloworld.engine.evaluation.BoardEvaluator
import com.othelloworld.engine.getNextPossibleMoves
import com.othelloworld.engine.updateBoardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NegamaxWithAlphaBetaSearchTest {

    @Test
    fun `zero temperature always chooses the highest scoring move`() {
        val scoredBoards = scoredStartingMoves()
        val search = NegamaxWithAlphaBetaSearch(
            searchDepth = 1,
            boardEvaluator = evaluator(scoredBoards),
            temperature = 0.0,
        )

        val expected = scoredBoards.maxBy { it.value }.key
        repeat(20) {
            assertEquals(expected, search.selectMove(STARTING_STATE, BLACK_TO_MOVE))
        }
    }

    @Test
    fun `zero temperature does not sample an inferior move from a fail-soft tie`() {
        val board = BoardState(
            whitePositions = 134217728L,
            blackPositions = 17695533694976L,
        )
        val search = NegamaxWithAlphaBetaSearch(
            searchDepth = 4,
            boardEvaluator = object : BoardEvaluator {
                override fun evaluateBoard(board: BoardState, gameIsOver: Boolean?): Double =
                    (board.blackPieceCount - board.whitePieceCount).toDouble()
            },
            temperature = 0.0,
            randomSeed = 0L,
        )

        val bestBoards = setOf(
            updateBoardState(board, blackToMove = false, move = 29),
            updateBoardState(board, blackToMove = false, move = 45),
        )

        assertTrue(search.selectMove(board, WHITE_TO_MOVE) in bestBoards)
    }

    @Test
    fun `positive temperature samples reproducibly from multiple moves`() {
        val scoredBoards = scoredStartingMoves()
        val firstSearch = NegamaxWithAlphaBetaSearch(
            searchDepth = 1,
            boardEvaluator = evaluator(scoredBoards),
            temperature = 1.0,
            randomSeed = 42L,
        )
        val secondSearch = NegamaxWithAlphaBetaSearch(
            searchDepth = 1,
            boardEvaluator = evaluator(scoredBoards),
            temperature = 1.0,
            randomSeed = 42L,
        )

        val firstSequence = List(100) { firstSearch.selectMove(STARTING_STATE, BLACK_TO_MOVE) }
        val secondSequence = List(100) { secondSearch.selectMove(STARTING_STATE, BLACK_TO_MOVE) }

        assertEquals(firstSequence, secondSequence)
        assertTrue(firstSequence.toSet().size > 1)
    }

    @Test
    fun `temperature probabilities are invariant to evaluator scale and offset`() {
        val scoredBoards = scoredStartingMoves()
        val transformedScores = scoredBoards.mapValues { (_, score) -> score * 1_000_000.0 + 1_000_000_000.0 }
        val originalSearch = NegamaxWithAlphaBetaSearch(
            searchDepth = 1,
            boardEvaluator = evaluator(scoredBoards),
            temperature = 0.7,
            randomSeed = 99L,
        )
        val transformedSearch = NegamaxWithAlphaBetaSearch(
            searchDepth = 1,
            boardEvaluator = evaluator(transformedScores),
            temperature = 0.7,
            randomSeed = 99L,
        )

        repeat(100) {
            assertEquals(
                originalSearch.selectMove(STARTING_STATE, BLACK_TO_MOVE),
                transformedSearch.selectMove(STARTING_STATE, BLACK_TO_MOVE),
            )
        }
    }

    @Test
    fun `temperature must be finite and non-negative`() {
        val evaluator = evaluator(scoredStartingMoves())

        assertFailsWith<IllegalArgumentException> {
            NegamaxWithAlphaBetaSearch(1, evaluator, temperature = -0.1)
        }
        assertFailsWith<IllegalArgumentException> {
            NegamaxWithAlphaBetaSearch(1, evaluator, temperature = Double.POSITIVE_INFINITY)
        }
    }

    private fun scoredStartingMoves(): Map<BoardState, Double> =
        getNextPossibleMoves(STARTING_STATE, blackToMove = true)
            .mapIndexed { index, move ->
                updateBoardState(STARTING_STATE, blackToMove = true, move) to index.toDouble()
            }
            .toMap()

    private fun evaluator(scores: Map<BoardState, Double>) = object : BoardEvaluator {
        override fun evaluateBoard(board: BoardState, gameIsOver: Boolean?): Double =
            scores.getValue(board)
    }
}
