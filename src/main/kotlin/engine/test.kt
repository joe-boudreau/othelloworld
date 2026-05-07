package com.othelloworld.engine

import java.lang.Thread.sleep
import com.othelloworld.engine.GameStatus.*

fun main() {
    var boardState = STARTING_STATE
    var status = ONGOING
    boardState.print()

    val engine = Engine()
    while(status == ONGOING) {
        sleep(100)
        val (updatedBoardState, updatedStatus) = engine.makeMove(boardState)
        boardState = updatedBoardState
        status = updatedStatus
        boardState.print()
    }
    println("Game Over!")
    println("Outcome: $status")
}