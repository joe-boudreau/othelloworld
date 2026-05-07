package com.othelloworld

class InvalidMoveException(move: Int) : Exception("Invalid move: $move")