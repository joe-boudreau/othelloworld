package com.othelloworld.engine

enum class GameStatus {
    BLACK_TO_MOVE,
    BLACK_TO_MOVE_WHITE_PASSING,
    WHITE_TO_MOVE,
    WHITE_TO_MOVE_BLACK_PASSING,
    WHITE_WINS,
    BLACK_WINS,
    DRAW;

    fun isTerminal() = this == WHITE_WINS || this == BLACK_WINS || this == DRAW
    fun blackToMove() = this == BLACK_TO_MOVE || this == BLACK_TO_MOVE_WHITE_PASSING
    fun whiteToMove() = this == WHITE_TO_MOVE || this == WHITE_TO_MOVE_BLACK_PASSING
    fun previousPlayerPassed() = this == BLACK_TO_MOVE_WHITE_PASSING || this == WHITE_TO_MOVE_BLACK_PASSING
}