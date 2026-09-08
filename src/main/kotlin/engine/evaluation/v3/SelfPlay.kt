package com.othelloworld.engine.evaluation.v3

import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.algorithms.DecayingEpsilonGreedyWrapper
import com.othelloworld.engine.algorithms.NegamaxWithAlphaBetaSearch
import com.othelloworld.engine.evaluation.pieceDiffScore
import com.othelloworld.engine.evaluation.v2.FeatureRow
import com.othelloworld.engine.evaluation.v2.phaseBucket
import com.othelloworld.engine.evaluation.v2.toFeatureVector
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    generateSelfPlayData(
        numGames = 10_000,
        weightsFilePath = "weights/v3_evaluator/weights_v3.json",
        outDir = "data/v3_evaluator",
        workerCount = 8,
    )
}

suspend fun generateSelfPlayData(
    numGames: Int,
    weightsFilePath: String,
    outDir: String,
    workerCount: Int = 8,
) {
    require(numGames > 0) { "numGames must be positive" }
    require(workerCount > 0) { "workerCount must be positive" }

    val boardEvaluator = V3BoardEvaluator(weightsFilePath)
    val gameDispatcher = Dispatchers.Default.limitedParallelism(workerCount)
    val gameResults = coroutineScope {
        (0 until numGames).map { gameNumber ->
            async(gameDispatcher) {
                playSelfPlayGame(gameNumber, boardEvaluator)
            }
        }.awaitAll()
    }

    val rows: MutableMap<String, MutableList<FeatureRow>> = mutableMapOf(
        "early" to mutableListOf(),
        "mid" to mutableListOf(),
        "late" to mutableListOf()
    )

    gameResults.forEach { result ->
        result.rows.forEach { row -> rows.getValue(row.phase).add(row) }
    }

    val whiteWins = gameResults.count { it.outcome < 0 }
    val blackWins = gameResults.count { it.outcome > 0 }
    val draws = gameResults.count { it.outcome == 0.0 }
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

private data class GameResult(
    val rows: List<FeatureRow>,
    val outcome: Double,
)

private fun playSelfPlayGame(
    gameNumber: Int,
    boardEvaluator: V3BoardEvaluator,
): GameResult {
    val randomSeed = 123456123L + gameNumber * 987654327L
    val initialEpsilon = 0.75
    val floorEpsilon = 0.0
    val epsilonDecayFactor = 0.75
    val negamaxTemperature = 0.7
    val searchDepth = 8

    val blackEngine = Engine(
        DecayingEpsilonGreedyWrapper(
            internalSelectionAlgorithm = NegamaxWithAlphaBetaSearch(
                searchDepth = searchDepth,
                boardEvaluator = boardEvaluator,
                temperature = negamaxTemperature,
                randomSeed = randomSeed,
            ),
            initialEpsilon = initialEpsilon,
            floorEpsilon = floorEpsilon,
            epsilonDecayFactor = epsilonDecayFactor,
            randomSeed = randomSeed,
        )
    )
    val whiteEngine = Engine(
        DecayingEpsilonGreedyWrapper(
            internalSelectionAlgorithm = NegamaxWithAlphaBetaSearch(
                searchDepth = searchDepth,
                boardEvaluator = boardEvaluator,
                temperature = negamaxTemperature,
                randomSeed = randomSeed + 1,
            ),
            initialEpsilon = initialEpsilon,
            floorEpsilon = floorEpsilon,
            epsilonDecayFactor = epsilonDecayFactor,
            randomSeed = randomSeed + 1,
        )
    )

    val gameHistory = mutableListOf<FeatureRow>()
    var boardState = STARTING_STATE
    var gameStatus = BLACK_TO_MOVE

    while (!gameStatus.isTerminal()) {
        val movingEngine = if (gameStatus.blackToMove()) blackEngine else whiteEngine
        val (updatedBoardState, updatedGameStatus) = movingEngine.makeEngineMove(boardState, gameStatus)
        boardState = updatedBoardState
        gameStatus = updatedGameStatus
        gameHistory.add(FeatureRow(boardState.toFeatureVector(), boardState.phaseBucket(), 0.0))
    }

    val outcome = boardState.pieceDiffScore()
    return GameResult(
        rows = gameHistory.map { row -> row.copy(outcome = outcome) },
        outcome = outcome,
    )
}
