package com.othelloworld.engine.exceptions

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus

class InvalidGameStatusException(boardState: BoardState, gameStatus: GameStatus) :
    Exception("Invalid game status: $gameStatus, board:\n ${boardState.print(false)}")