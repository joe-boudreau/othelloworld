package com.othelloworld.engine.evaluation.v3

import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.algorithms.DecayingEpsilonGreedyWrapper
import com.othelloworld.engine.algorithms.NegamaxWithAlphaBetaSearch
import com.othelloworld.engine.evaluation.pieceDiffScore
import com.othelloworld.engine.evaluation.v2.FeatureRow
import com.othelloworld.engine.evaluation.v2.phaseBucket
import com.othelloworld.engine.evaluation.v2.toFeatureVector
import java.io.File

fun main() {
    generateSelfPlayData(5000, "weights/v3_evaluator/weights_v2.json","data/v3_evaluator")
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

    val maxSearchDepth = 8
    fun getSearchDepth(gameNum: Int) = if (gameNum < numGames / 3) maxSearchDepth else if (gameNum < 2 * numGames / 3) maxSearchDepth-1 else maxSearchDepth-2

    repeat(numGames) {
        println("Game $it")
        val gameHistory = mutableListOf<FeatureRow>()

        val boardEvaluator = V3BoardEvaluator(weightsFilePath)

        val randomSeed = 91234567L + it * 987654327L

        val initialEpsilon = 0.5
        val floorEpsilon = 0.05
        val epsilonDecayFactor = 0.75
        val searchDepth = getSearchDepth(it)

        // Black
        val player1Engine = Engine(
            DecayingEpsilonGreedyWrapper(
                internalSelectionAlgorithm = NegamaxWithAlphaBetaSearch(searchDepth = searchDepth, boardEvaluator),
                initialEpsilon = initialEpsilon,
                floorEpsilon = floorEpsilon,
                epsilonDecayFactor = epsilonDecayFactor,
                randomSeed = randomSeed
            )
        )
        // White
        val player2Engine = Engine(
            DecayingEpsilonGreedyWrapper(
                internalSelectionAlgorithm = NegamaxWithAlphaBetaSearch(searchDepth = searchDepth, boardEvaluator),
                initialEpsilon = initialEpsilon,
                floorEpsilon = floorEpsilon,
                epsilonDecayFactor = epsilonDecayFactor,
                randomSeed = randomSeed + 1
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