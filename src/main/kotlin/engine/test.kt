package com.othelloworld.engine

import java.lang.Thread.sleep
import com.othelloworld.engine.GameStatus.*
import com.othelloworld.engine.algorithms.Random

fun main() {
    var boardState = STARTING_STATE
    var status = BLACK_TO_MOVE
    boardState.print()

    val engine = Engine(Random())
    while(status.isTerminal().not()) {
        sleep(100)
        val (updatedBoardState, updatedStatus) = engine.makeEngineMove(boardState, status)
        boardState = updatedBoardState
        status = updatedStatus
        boardState.print()
    }
    println("Game Over!")
    println("Outcome: $status")
}