package com.othelloworld.benchmark

import com.othelloworld.engine.evaluation.v2.V2BoardEvaluator

import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.algorithms.NegamaxWithAlphaBetaSearch
import com.othelloworld.engine.evaluation.pieceDiffScore

fun main(args: Array<String>) {
    require(args.size == 5) {
        "Expected: <player 1 weights path> <player 2 weights path> <p1 search depth> <p2 search depth> <num games>"
    }

    val p1WeightsPath = args[0]
    val p2WeightsPath = args[1]
    val p1SearchDepth = args[2].toInt()
    val p2SearchDepth = args[3].toInt()
    val numGames = args[4].toInt()

    println("Benchmark configuration:")
    println("  player 1 weights: $p1WeightsPath")
    println("  player 2 weights: $p2WeightsPath")
    println("  player 1 search depth: $p1SearchDepth")
    println("  player 2 search depth: $p2SearchDepth")
    println("  num games: $numGames")

    var p1Wins = 0
    var p2Wins = 0
    var draws = 0


    repeat(numGames) {
        val p1IsBlack = it % 2 == 0

        println("Game $it - Player 1: ${if (p1IsBlack) "Black" else "White"}, Player 2: ${if (!p1IsBlack) "Black" else "White"}")

        val player1Engine = Engine(NegamaxWithAlphaBetaSearch(
            p1SearchDepth,
            V2BoardEvaluator(p1WeightsPath)
        ))

        val player2Engine = Engine(NegamaxWithAlphaBetaSearch(
            p2SearchDepth,
            V2BoardEvaluator(p2WeightsPath)
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

        val blackWins = outcome > 0
        val draw = outcome == 0.0

        if (draw) draws++
        else if (blackWins && p1IsBlack || !blackWins && !p1IsBlack) p1Wins++
        else p2Wins++
    }

    println("Player 1 wins: $p1Wins, Player 2 wins: $p2Wins, Draws: $draws")
    println("Player 1 win ratio: ${p1Wins.toDouble() / (p1Wins + p2Wins + draws)}")
    println("Player 2 win ratio: ${p2Wins.toDouble() / (p1Wins + p2Wins + draws)}")
}