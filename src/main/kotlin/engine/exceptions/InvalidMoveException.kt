package com.othelloworld.engine.exceptions

class InvalidMoveException(move: Int) : Exception("Invalid move: $move")