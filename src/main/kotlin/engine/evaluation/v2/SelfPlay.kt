package com.othelloworld.engine.evaluation.v2

import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.algorithms.EpsilonGreedyWrapper
import com.othelloworld.engine.algorithms.NegamaxWithAlphaBetaSearch
import com.othelloworld.engine.evaluation.pieceDiffScore
import java.io.File

fun main() {
    generateSelfPlayData(1, "data")
}

fun generateSelfPlayData(numGames: Int, outDir: String) {

    val rows: MutableMap<String, MutableList<FeatureRow>> = mutableMapOf(
        "early" to mutableListOf(),
        "mid" to mutableListOf(),
        "late" to mutableListOf()
    )

    repeat(numGames) {
        val gameHistory = mutableListOf<FeatureRow>()

        val boardEvaluator = V2BoardEvaluator()

        // Black
        val player1Engine = Engine(
            EpsilonGreedyWrapper(
                NegamaxWithAlphaBetaSearch(baseSearchDepth = 5, boardEvaluator)
            )
        )
        // White
        val player2Engine = Engine(
            EpsilonGreedyWrapper(
                NegamaxWithAlphaBetaSearch(baseSearchDepth = 5, boardEvaluator)
            )
        )

        fun getMovingEngine(gameStatus : GameStatus) = if (gameStatus.blackToMove()) player1Engine else player2Engine

        var boardState = STARTING_STATE
        var gameStatus = BLACK_TO_MOVE

        while (!gameStatus.isTerminal()) {
            val result = getMovingEngine(gameStatus).makeEngineMove(boardState, gameStatus)
            boardState = result.first
            gameStatus = result.second
            gameHistory.add(FeatureRow(boardState.toFeatureVector(), boardState.phaseBucket(), 0.0))
        }

        val outcome = boardState.pieceDiffScore()  // normalized to [-1, 1]
        gameHistory.forEach { row -> rows[row.phase]!!.add(row.copy(outcome = outcome)) }
    }

    rows.forEach { (phase, phaseRows) ->
        File("$outDir/samples_$phase.csv").bufferedWriter().use { writer ->
            val featureCount = phaseRows.first().features.size
            writer.write((0 until featureCount).joinToString(",") { "f$it" } + ",outcome\n")
            phaseRows.forEach { row ->
                writer.write(row.features.joinToString(",") + ",${row.outcome}\n")
            }
        }
    }
}