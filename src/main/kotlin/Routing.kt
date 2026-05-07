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

        post("/api/new-game") {
            call.respondBoard(STARTING_STATE)
        }

        post("/api/player/move") {
            val params = call.receiveParameters()
            val board = params.readBoard()
            val square = params["square"]?.toIntOrNull()
                ?: throw MissingFieldException("square")

            val orchestrator = GameOrchestrator(board)
            val (newBoard, status) = orchestrator.makePlayerMove(square)
            val orchestratorAfter = GameOrchestrator(newBoard)

            // Tell HTMX to chain into the engine's move iff the game continues
            // and the next-to-move side actually has a move (no pass logic yet).
            if (status == GameStatus.ONGOING && orchestratorAfter.validMoves().isNotEmpty()) {
                call.response.headers.append("HX-Trigger-After-Settle", "computerMove")
            }

            call.respondText(
                renderBoardFragment(newBoard, orchestratorAfter.validMoves(), status),
                ContentType.Text.Html,
                HttpStatusCode.OK,
            )
        }

        post("/api/computer/move") {
            val params = call.receiveParameters()
            val board = params.readBoard()

            val orchestrator = GameOrchestrator(board)
            val (newBoard, status) = orchestrator.makeEngineMove()
            call.respondBoard(newBoard, status = status)
        }
    }
}

private suspend fun ApplicationCall.respondBoard(
    boardState: BoardState,
    status: GameStatus? = null,
) {
    val orch = GameOrchestrator(boardState)
    val effectiveStatus = status ?: orch.getGameStatus()
    respondText(
        renderBoardFragment(boardState, orch.validMoves(), effectiveStatus),
        ContentType.Text.Html,
        HttpStatusCode.OK,
    )
}

private fun Parameters.readBoard(): BoardState {
    val white = this["whitePos"]?.toLongOrNull()
        ?: throw MissingFieldException("whitePos")
    val black = this["blackPos"]?.toLongOrNull()
        ?: throw MissingFieldException("blackPos")
    return BoardState(white, black)
}

class MissingFieldException(field: String) : Exception("Missing or invalid field: $field")
