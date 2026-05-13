package com.othelloworld

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.algorithms.Greedy
import com.othelloworld.engine.algorithms.MoveSelectionAlgorithm
import com.othelloworld.engine.algorithms.NegamaxSearch
import com.othelloworld.engine.algorithms.Random
import com.othelloworld.engine.exceptions.InvalidMoveException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(StatusPages) {
        exception<InvalidMoveException> { call, cause ->
            call.respondText(
                renderErrorFragment(cause.message ?: "Invalid move"),
                ContentType.Text.Html,
                HttpStatusCode.BadRequest,
            )
        }
        exception<MissingFieldException> { call, cause ->
            call.respondText(
                renderErrorFragment(cause.message ?: "Missing field"),
                ContentType.Text.Html,
                HttpStatusCode.BadRequest,
            )
        }
    }

    routing {
        get("/") {
            call.respondText(renderGamePage(), ContentType.Text.Html, HttpStatusCode.OK)
        }

        get("/api/new-game-modal") {
            call.respondText(renderColorPickerFragment(), ContentType.Text.Html, HttpStatusCode.OK)
        }

        post("/api/new-game") {
            val params = call.receiveParameters()
            val rawColor = params["color"] ?: throw MissingFieldException("color")
            val algorithm = params.readAlgorithm()
            val playerPlaysAsBlack = when (rawColor) {
                "B" -> true
                "W" -> false
                "R" -> kotlin.random.Random.nextBoolean()
                else -> throw MissingFieldException("color (must be B, W, or R)")
            }

            val engine = Engine(algorithm)
            val initialBoardState = STARTING_STATE
            val initialGameStatus = params.readGameStatus()

            // Black always moves first in Othello. If the human chose white, the computer plays
            // black first — chain into /api/computer/move and render the board non-interactive.
            val playerPlaysNext = playerPlaysAsBlack
            if (!playerPlaysNext) {
                call.response.headers.append("HX-Trigger-After-Settle", "computerMove")
            }

            call.respondBoard(
                board = initialBoardState,
                status = initialGameStatus,
                algorithm = algorithm.name,
                validMoves = engine.getValidMoves(initialBoardState, initialGameStatus),
                playerPlaysNext = playerPlaysNext,
                playerJustPlayed = null, // game just started — no one has played yet
            )
        }

        post("/api/player/move") {
            val params = call.receiveParameters()
            val board = params.readBoard()
            val algorithm = params.readAlgorithm()
            val gameStatus = params.readGameStatus()
            val moveSquare = params["square"]?.toIntOrNull() ?: throw MissingFieldException("square")
            val playerPlaysAsBlack = gameStatus.blackToMove()

            val engine = Engine(algorithm)

            val (newBoard, newStatus) = engine.makePlayerMove(board, gameStatus, moveSquare)

            val playerPlaysNext = playerPlaysAsBlack && newStatus.blackToMove() || !playerPlaysAsBlack && newStatus.whiteToMove()

            call.respondBoard(
                board = newBoard,
                status = newStatus,
                algorithm = algorithm.name,
                validMoves = engine.getValidMoves(newBoard, newStatus),
                lastMove = findLastMove(board, newBoard),
                flippedSquares = findFlippedSquares(board, newBoard),
                playerPlaysNext = playerPlaysNext,
                playerJustPlayed = true,
            )
        }

        post("/api/computer/move") {
            val params = call.receiveParameters()
            val board = params.readBoard()
            val algorithm = params.readAlgorithm()
            val gameStatus = params.readGameStatus()
            val playerPlaysAsBlack = gameStatus.whiteToMove()

            val engine = Engine(algorithm)
            val (newBoard, newStatus) = engine.makeEngineMove(board, gameStatus)

            val playerPlaysNext = playerPlaysAsBlack && newStatus.blackToMove() || !playerPlaysAsBlack && newStatus.whiteToMove()

            call.respondBoard(
                board = newBoard,
                status = newStatus,
                algorithm = algorithm.name,
                validMoves = engine.getValidMoves(newBoard, newStatus),
                lastMove = findLastMove(board, newBoard),
                flippedSquares = findFlippedSquares(board, newBoard),
                playerPlaysNext = playerPlaysNext,
                playerJustPlayed = false,
            )
        }
    }
}

private suspend fun ApplicationCall.respondBoard(
    board: BoardState,
    status: GameStatus,
    algorithm: String,
    validMoves: List<Int>,
    lastMove: Int? = null,
    flippedSquares: List<Int> = emptyList(),
    playerPlaysNext: Boolean,
    playerJustPlayed: Boolean?,
) {
    if (!status.isTerminal() && !playerPlaysNext && playerJustPlayed != false) {
        // Instantly chain the computer move into the next request. Covers both the
        // normal "player just played, computer's turn" case and the game-start case
        // where the player chose white and the computer plays first.
        this.response.headers.append("HX-Trigger-After-Settle", "computerMove")
    }
    // When playerJustPlayed == false && !playerPlaysNext (computer just played, the
    // player has to pass, computer plays again), we deliberately do NOT chain here.
    // The rendered board fragment shows a pass-modal explaining the situation and
    // fires `computerMove` itself after a delay, giving the player time to read it.

    respondText(
        renderBoardFragment(board, status, algorithm, validMoves, lastMove, flippedSquares, playerPlaysNext, playerJustPlayed),
        ContentType.Text.Html,
        HttpStatusCode.OK,
    )
}

/**
 * Identifies squares whose piece changed color between two board states (i.e. were flipped).
 * A square is flipped iff it was occupied both before and after, but its color changed —
 * detected via XOR on the white bitboards, masked to squares occupied in both states.
 */
private fun findFlippedSquares(before: BoardState, after: BoardState): List<Int> {
    val occupiedBoth = (before.whitePositions or before.blackPositions) and
        (after.whitePositions or after.blackPositions)
    val changedColor = (before.whitePositions xor after.whitePositions) and occupiedBoth
    val result = mutableListOf<Int>()
    for (i in 0 until 64) {
        if ((changedColor ushr i) and 1L == 1L) result.add(i)
    }
    return result
}

/**
 * Identifies the square just placed by diffing two board states.
 * A move places exactly one new piece on a previously empty square — that's the bit
 * that flipped from empty to occupied. Returns null if no piece was placed (e.g., game-over).
 */
private fun findLastMove(before: BoardState, after: BoardState): Int? {
    val emptyBefore = (before.whitePositions or before.blackPositions).inv()
    val occupiedAfter = after.whitePositions or after.blackPositions
    val placed = emptyBefore and occupiedAfter
    return if (placed == 0L) null else java.lang.Long.numberOfTrailingZeros(placed)
}

private fun Parameters.readBoard(): BoardState {
    val white = this["whitePos"]?.toLongOrNull()
        ?: throw MissingFieldException("whitePos")
    val black = this["blackPos"]?.toLongOrNull()
        ?: throw MissingFieldException("blackPos")
    return BoardState(white, black)
}

private fun Parameters.readGameStatus(): GameStatus {
    val status = this["status"] ?: throw MissingFieldException("status")
    return GameStatus.valueOf(status)
}

private fun Parameters.readAlgorithm(): MoveSelectionAlgorithm {
    val algorithm = this["algorithm"] ?: NegamaxSearch().name
    return when (algorithm) {
        Random().name -> Random()
        Greedy().name -> Greedy()
        else -> NegamaxSearch()
    }
}

class MissingFieldException(field: String) : Exception("Missing or invalid field: $field")
