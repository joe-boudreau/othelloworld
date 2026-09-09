package com.othelloworld.benchmark

import com.othelloworld.engine.evaluation.v2.V2BoardEvaluator

import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.algorithms.DecayingEpsilonGreedyWrapper
import com.othelloworld.engine.algorithms.NegamaxWithAlphaBetaSearch
import com.othelloworld.engine.evaluation.blackPieceRatio
import com.othelloworld.engine.evaluation.pieceDiffScore
import com.othelloworld.engine.evaluation.v1.V1BoardEvaluator
import com.othelloworld.engine.evaluation.v3.V3BoardEvaluator
import com.othelloworld.engine.evaluation.whitePieceRatio

fun main(args: Array<String>) {
    require(args.size in 4..6) {
        "Expected: " +
            "<p1 evaluator version> " +
            "<p2 evaluator version> " +
            "<player 1 weights path> " +
            "<player 2 weights path> " +
            "[optional] <p1 search depth> default: 7" +
            "[optional] <p2 search depth> default: 7"
    }

    val p1EvaluatorVersion = args[0]
    val p2EvaluatorVersion = args[1]
    val p1WeightsPath = args[2]
    val p2WeightsPath = args[3]
    val p1SearchDepth = args.getOrNull(4)?.toInt() ?: 7
    val p2SearchDepth = args.getOrNull(5)?.toInt() ?: 7
    val seedPositions = SeedPositionsRepository().read()

    println("Benchmark configuration:")
    println("  player 1 evaluator version: $p1EvaluatorVersion")
    println("  player 2 evaluator version: $p2EvaluatorVersion")
    println("  player 1 weights: $p1WeightsPath")
    println("  player 2 weights: $p2WeightsPath")
    println("  player 1 search depth: $p1SearchDepth")
    println("  player 2 search depth: $p2SearchDepth")
    println("  seed positions: ${seedPositions.size}")

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

    seedPositions.forEachIndexed { gameNumber, seedPosition ->
        val p1IsBlack = gameNumber % 2 == 0

        //val randomSeed = 987654321 + gameNumber * 12345678L

        val player1Engine = Engine(NegamaxWithAlphaBetaSearch(p1SearchDepth,p1Evaluator))

        val player2Engine = Engine(NegamaxWithAlphaBetaSearch(p2SearchDepth,p2Evaluator))

        fun getMovingEngine(gameStatus : GameStatus) = if (gameStatus.blackToMove() && p1IsBlack || gameStatus.whiteToMove() && !p1IsBlack) player1Engine else player2Engine

        var boardState = seedPosition.board
        var gameStatus = seedPosition.gameStatus

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
        }
        else {
            p2Wins++
            val pieceRatioPercent = (if (p1IsBlack) whitePieceRatio else blackPieceRatio) * 100
            sumP2PieceRatioPercentForWins += pieceRatioPercent
            sumP2TotalMovesForWins += numberOfMoves
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
