package com.othelloworld.engine.evaluation.v2

import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.algorithms.EpsilonGreedyWrapper
import com.othelloworld.engine.algorithms.NegamaxWithAlphaBetaSearch
import com.othelloworld.engine.algorithms.Random
import com.othelloworld.engine.evaluation.pieceDiffScore
import java.io.File

fun main() {
    generateSelfPlayData(100, "weights/weights_latest.json","data")
}

fun generateSelfPlayData(numGames: Int, weightsFilePath: String, outDir: String) {

    val rows: MutableMap<String, MutableList<FeatureRow>> = mutableMapOf(
        "early" to mutableListOf(),
        "mid" to mutableListOf(),
        "late" to mutableListOf()
    )

    var whiteWins = 0
    var blackWins = 0
    var draws = 0


    repeat(numGames) {
        println("Game $it")
        val gameHistory = mutableListOf<FeatureRow>()

        val boardEvaluator = V2BoardEvaluator(weightsFilePath)

        // Black
        val player1Engine = Engine(
            EpsilonGreedyWrapper(
                NegamaxWithAlphaBetaSearch(baseSearchDepth = 3, boardEvaluator)
            )
        )
        // White
        val player2Engine = Engine(
            EpsilonGreedyWrapper(
                NegamaxWithAlphaBetaSearch(baseSearchDepth = 3, boardEvaluator)
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

        if (outcome > 0) blackWins++
        else if (outcome < 0) whiteWins++
        else draws++

    }

    println("White wins: $whiteWins, Black wins: $blackWins, Draws: $draws")

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