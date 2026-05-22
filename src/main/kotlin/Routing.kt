package com.othelloworld

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.algorithms.Greedy
import com.othelloworld.engine.algorithms.MoveSelectionAlgorithm
import com.othelloworld.engine.algorithms.NegamaxSearch
import com.othelloworld.engine.algorithms.NegamaxWithAlphaBetaSearch
import com.othelloworld.engine.algorithms.Random
import com.othelloworld.engine.exceptions.InvalidMoveException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.htmx.hx
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json

@OptIn(ExperimentalKtorApi::class, ExperimentalSerializationApi::class)
fun Application.configureRouting() {
    val baseSearchDepth = environment.config.property("app.engine.baseSearchDepth").getString().toInt()

    install(StatusPages) {
        exception<InvalidMoveException> { call, cause ->
            call.respondError(cause.message ?: "Invalid move")
        }
        exception<MissingFieldException> { call, cause ->
            call.respondError(cause.message ?: "Missing field")
        }
        exception<com.othelloworld.MissingFieldException> { call, cause ->
            call.respondError(cause.message ?: "Missing field")
        }
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    routing {
        get("/") {
            val prefix = call.request.headers["X-Forwarded-Prefix"]?.trimEnd('/') ?: ""
            call.respondText(renderGamePage(prefix), ContentType.Text.Html, HttpStatusCode.OK)
        }

        hx {
            get("/api/new-game-modal") {
                call.respondText(renderColorPickerFragment(), ContentType.Text.Html, HttpStatusCode.OK)
            }

            post("/api/resume-game") {
                val params = call.receiveParameters()
                val board = params.readBoard()
                val algorithm = params.readAlgorithm(baseSearchDepth)
                val gameStatus = params.readGameStatus()
                val playerColor = params["playerColor"] ?: throw MissingFieldException("playerColor")
                val playerPlaysAsBlack = when (playerColor) {
                    "B" -> true
                    "W" -> false
                    else -> throw MissingFieldException("playerColor (must be B or W)")
                }
                val playerPlaysNext = (playerPlaysAsBlack && gameStatus.blackToMove()) ||
                        (!playerPlaysAsBlack && gameStatus.whiteToMove())

                val engine = Engine(algorithm)
                call.respondBoard(
                    board = board,
                    status = gameStatus,
                    algorithm = algorithm.name,
                    validMoves = engine.getValidMoves(board, gameStatus),
                    playerPlaysNext = playerPlaysNext,
                    playerJustPlayed = null,
                )
            }

            post("/api/new-game") {
                val params = call.receiveParameters()
                val rawColor = params["color"] ?: throw MissingFieldException("color")
                val algorithm = params.readAlgorithm(baseSearchDepth)
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
                val algorithm = params.readAlgorithm(baseSearchDepth)
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
                val algorithm = params.readAlgorithm(baseSearchDepth)
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

        // Stateless JSON REST API. Clients hold all game state (board bitboards,
        // status, algorithm) and pass it in on each request. These routes share
        // paths with the HTMX endpoints above, but the `hx { }` block only matches
        // requests with the HX-Request header, so non-HTMX requests fall through here.
        post("/api/new-game") {
            val engine = Engine(NegamaxSearch(baseSearchDepth))
            call.respond(GameStateResponse.from(
                board = STARTING_STATE,
                status = GameStatus.BLACK_TO_MOVE,
                algorithm = NegamaxSearch.NAME,
                validMoves = engine.getValidMoves(STARTING_STATE, GameStatus.BLACK_TO_MOVE),
            ))
        }

        post("/api/player/move") {
            val req = call.receive<PlayerMoveRequest>()
            val board = BoardState(req.whitePositions, req.blackPositions)
            val algorithm = resolveAlgorithm(req.algorithm, baseSearchDepth)
            val engine = Engine(algorithm)

            val (newBoard, newStatus) = engine.makePlayerMove(board, req.status, req.square)

            call.respond(GameStateResponse.from(
                board = newBoard,
                status = newStatus,
                algorithm = algorithm.name,
                validMoves = engine.getValidMoves(newBoard, newStatus),
                lastMove = findLastMove(board, newBoard),
                flippedSquares = findFlippedSquares(board, newBoard),
            ))
        }

        post("/api/computer/move") {
            val req = call.receive<ComputerMoveRequest>()
            val board = BoardState(req.whitePositions, req.blackPositions)
            val algorithm = resolveAlgorithm(req.algorithm, baseSearchDepth)
            val engine = Engine(algorithm)

            val (newBoard, newStatus) = engine.makeEngineMove(board, req.status)

            call.respond(GameStateResponse.from(
                board = newBoard,
                status = newStatus,
                algorithm = algorithm.name,
                validMoves = engine.getValidMoves(newBoard, newStatus),
                lastMove = findLastMove(board, newBoard),
                flippedSquares = findFlippedSquares(board, newBoard),
            ))
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

private fun Parameters.readAlgorithm(baseSearchDepth: Int): MoveSelectionAlgorithm =
    resolveAlgorithm(this["algorithm"], baseSearchDepth)

class MissingFieldException(field: String) : Exception("Missing or invalid field: $field")

private fun resolveAlgorithm(name: String?, baseSearchDepth: Int): MoveSelectionAlgorithm = when (name) {
    null, NegamaxSearch.NAME -> NegamaxSearch(baseSearchDepth)
    Random.NAME -> Random()
    Greedy.NAME -> Greedy()
    NegamaxWithAlphaBetaSearch.NAME -> NegamaxWithAlphaBetaSearch(baseSearchDepth)
    else -> throw MissingFieldException("algorithm (unknown: $name)")
}

// Errors thrown from handlers respond as HTML for HTMX requests (so the UI can
// swap the fragment into an error slot) and as JSON otherwise (for the REST API).
private suspend fun ApplicationCall.respondError(message: String) {
    if (request.headers["HX-Request"] != null) {
        respondText(renderErrorFragment(message), ContentType.Text.Html, HttpStatusCode.BadRequest)
    } else {
        respond(HttpStatusCode.BadRequest, ErrorResponse(message))
    }
}
