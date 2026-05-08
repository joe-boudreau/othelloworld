package com.othelloworld

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.STARTING_STATE
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
            val humanIsBlack = when (rawColor) {
                "B" -> true
                "W" -> false
                "R" -> kotlin.random.Random.nextBoolean()
                else -> throw MissingFieldException("color (must be B, W, or R)")
            }

            val orchestrator = GameOrchestrator(STARTING_STATE)
            // Black always moves first in Othello. If the human chose white, the computer plays
            // black first — chain into /api/computer/move and render the board non-interactive.
            val computerToMoveFirst = !humanIsBlack
            if (computerToMoveFirst) {
                call.response.headers.append("HX-Trigger-After-Settle", "computerMove")
            }
            call.respondBoard(
                board = orchestrator.getBoardState(),
                status = orchestrator.getGameStatus(),
                validMoves = orchestrator.validMoves(),
                interactive = !computerToMoveFirst,
            )
        }

        post("/api/player/move") {
            val params = call.receiveParameters()
            val board = params.readBoard()
            val square = params["square"]?.toIntOrNull() ?: throw MissingFieldException("square")

            val orchestrator = GameOrchestrator(board)
            val (newBoard, status) = orchestrator.makePlayerMove(square)

            // Tell HTMX to chain into the engine's move iff the game continues
            // and the next-to-move side actually has a move (no pass logic yet).
            if (status == GameStatus.ONGOING) {
                call.response.headers.append("HX-Trigger-After-Settle", "computerMove")
            }

            call.respondBoard(
                board = newBoard,
                status = status,
                validMoves = orchestrator.validMoves(),
                lastMove = findLastMove(board, newBoard),
                flippedSquares = findFlippedSquares(board, newBoard),
                interactive = false, // computer is up next; lock the board until it responds
            )
        }

        post("/api/computer/move") {
            val params = call.receiveParameters()
            val board = params.readBoard()

            val orchestrator = GameOrchestrator(board)
            val (newBoard, status) = orchestrator.makeEngineMove()

            call.respondBoard(
                board = newBoard,
                status = status,
                validMoves = orchestrator.validMoves(),
                lastMove = findLastMove(board, newBoard),
                flippedSquares = findFlippedSquares(board, newBoard),
            )
        }
    }
}

private suspend fun ApplicationCall.respondBoard(
    board: BoardState,
    status: GameStatus,
    validMoves: List<Int>,
    lastMove: Int? = null,
    flippedSquares: List<Int> = emptyList(),
    interactive: Boolean = true,
) {
    respondText(
        renderBoardFragment(board, validMoves, status, lastMove, flippedSquares, interactive),
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

class MissingFieldException(field: String) : Exception("Missing or invalid field: $field")
