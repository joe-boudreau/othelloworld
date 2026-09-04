package com.othelloworld.benchmark

import com.othelloworld.engine.evaluation.v2.V2BoardEvaluator

import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.algorithms.DecayingEpsilonGreedyWrapper
import com.othelloworld.engine.algorithms.NegamaxWithAlphaBetaSearch
import com.othelloworld.engine.evaluation.blackPieceRatio
import com.othelloworld.engine.evaluation.pieceDiffScore
import com.othelloworld.engine.evaluation.v1.V1BoardEvaluator
import com.othelloworld.engine.evaluation.v3.V3BoardEvaluator
import com.othelloworld.engine.evaluation.whitePieceRatio

fun main(args: Array<String>) {
    require(args.size == 7) {
        "Expected: <player 1 weights path> <player 2 weights path> <p1 search depth> <p2 search depth> <p1 evaluator version> <p2 evaluator version> <num games>"
    }

    val p1WeightsPath = args[0]
    val p2WeightsPath = args[1]
    val p1SearchDepth = args[2].toInt()
    val p2SearchDepth = args[3].toInt()
    val p1EvaluatorVersion = args[4]
    val p2EvaluatorVersion = args[5]
    val numGames = args[6].toInt()

    println("Benchmark configuration:")
    println("  player 1 weights: $p1WeightsPath")
    println("  player 2 weights: $p2WeightsPath")
    println("  player 1 search depth: $p1SearchDepth")
    println("  player 2 search depth: $p2SearchDepth")
    println("  num games: $numGames")

    var draws = 0
    var p1Wins = 0
    var p2Wins = 0
    var sumP1PieceRatioPercentForWins = 0.0
    var sumP2PieceRatioPercentForWins = 0.0
    var sumP1TotalMovesForWins = 0.0
    var sumP2TotalMovesForWins = 0.0

    val p1Evaluator = when (p1EvaluatorVersion) {
        "v1" -> V1BoardEvaluator()
        "v2" -> V2BoardEvaluator(p1WeightsPath)
        "v3" -> V3BoardEvaluator(p1WeightsPath)
        else -> throw IllegalArgumentException("Unknown evaluator version: $p1EvaluatorVersion")
    }

    val p2Evaluator = when (p2EvaluatorVersion) {
        "v1" -> V1BoardEvaluator()
        "v2" -> V2BoardEvaluator(p2WeightsPath)
        "v3" -> V3BoardEvaluator(p2WeightsPath)
        else -> throw IllegalArgumentException("Unknown evaluator version: $p2EvaluatorVersion")
    }

    repeat(numGames) {
        val p1IsBlack = it % 2 == 0

        println("\nGame $it - Player 1: ${if (p1IsBlack) "Black" else "White"}, Player 2: ${if (!p1IsBlack) "Black" else "White"}")

        val randomSeed = 987654321 + it * 12345678L

        val initialEpsilon = 0.5
        val floorEpsilon = 0.0
        val epsilonDecayFactor = 0.5

        val player1Engine = Engine(
            DecayingEpsilonGreedyWrapper(
                internalSelectionAlgorithm = NegamaxWithAlphaBetaSearch(p1SearchDepth,p1Evaluator),
                initialEpsilon = initialEpsilon,
                floorEpsilon = floorEpsilon,
                epsilonDecayFactor = epsilonDecayFactor,
                randomSeed = randomSeed
        ))

        val player2Engine = Engine(
            DecayingEpsilonGreedyWrapper(
                internalSelectionAlgorithm = NegamaxWithAlphaBetaSearch(p2SearchDepth,p2Evaluator),
                initialEpsilon = initialEpsilon,
                floorEpsilon = floorEpsilon,
                epsilonDecayFactor = epsilonDecayFactor,
                randomSeed = randomSeed + 1
            ))

        fun getMovingEngine(gameStatus : GameStatus) = if (gameStatus.blackToMove() && p1IsBlack || gameStatus.whiteToMove() && !p1IsBlack) player1Engine else player2Engine

        var boardState = STARTING_STATE
        var gameStatus = BLACK_TO_MOVE

        while (!gameStatus.isTerminal()) {
            val result = getMovingEngine(gameStatus).makeEngineMove(boardState, gameStatus)
            boardState = result.first
            gameStatus = result.second
        }

        val outcome = boardState.pieceDiffScore()  // normalized to [-1, 1]
        val blackPieceRatio = boardState.blackPieceRatio()
        val whitePieceRatio = boardState.whitePieceRatio()
        val numberOfMoves = boardState.turnNumber // max is 60

        val blackWins = outcome > 0
        val draw = outcome == 0.0

        if (draw) draws++
        else if (blackWins && p1IsBlack || !blackWins && !p1IsBlack) {
            p1Wins++
            val pieceRatioPercent = (if (p1IsBlack) blackPieceRatio else whitePieceRatio) * 100
            sumP1PieceRatioPercentForWins += pieceRatioPercent
            sumP1TotalMovesForWins += numberOfMoves
            println("Player 1 wins. Piece ratio: $pieceRatioPercent%, Total moves: $numberOfMoves")
        }
        else {
            p2Wins++
            val pieceRatioPercent = (if (p1IsBlack) whitePieceRatio else blackPieceRatio) * 100
            sumP2PieceRatioPercentForWins += pieceRatioPercent
            sumP2TotalMovesForWins += numberOfMoves
            println("Player 2 wins. Piece ratio: $pieceRatioPercent%, Total moves: $numberOfMoves")
        }
    }

    val avgP1PieceRatioPercentForWins = sumP1PieceRatioPercentForWins / p1Wins
    val avgP2PieceRatioPercentForWins = sumP2PieceRatioPercentForWins / p2Wins
    val avgP1TotalMovesForWins = sumP1TotalMovesForWins / p1Wins
    val avgP2TotalMovesForWins = sumP2TotalMovesForWins / p2Wins

    val p1WinPercent = (p1Wins.toDouble() / (p1Wins + p2Wins + draws)) * 100
    val p2WinPercent = (p2Wins.toDouble() / (p1Wins + p2Wins + draws)) * 100

    println("\n\nOverall stats: \n\tPlayer 1 wins: $p1Wins, Player 2 wins: $p2Wins, Draws: $draws")
    println("Player 1 win stats: \n\tavg win percentage: $p1WinPercent%, \n\tpiece ratio: $avgP1PieceRatioPercentForWins%, \n\tavg total moves: $avgP1TotalMovesForWins")
    println("Player 2 win stats: \n\tavg win percentage: $p2WinPercent%, \n\tpiece ratio: $avgP2PieceRatioPercentForWins%, \n\tavg total moves: $avgP2TotalMovesForWins")
}