package com.othelloworld

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import kotlinx.html.*
import kotlinx.html.stream.createHTML

fun renderGamePage(): String = createHTML().html {
    head {
        title("Othello, World!")
        script { src = "https://unpkg.com/htmx.org@2.0.4" }
        style {
            unsafe {
                +"""
                body { font-family: system-ui, sans-serif; background: #2b2b2b; color: #eee; display: flex; flex-direction: column; align-items: center; padding: 2rem; }
                h1 { margin-top: 0; }
                #board-container { display: inline-block; }
                .board { display: grid; grid-template-columns: repeat(8, 56px); grid-template-rows: repeat(8, 56px); gap: 2px; background: #1a1a1a; padding: 4px; border: 2px solid #1a1a1a; }
                .square { background: #2e7d32; display: flex; align-items: center; justify-content: center; padding: 0; border: none; cursor: default; }
                .square.valid { cursor: pointer; background: #388e3c; }
                .square.valid:hover { background: #4caf50; }
                .disc { width: 42px; height: 42px; border-radius: 50%; }
                .disc.W { background: radial-gradient(circle at 30% 30%, #fff, #d0d0d0); }
                .disc.B { background: radial-gradient(circle at 30% 30%, #555, #000); }
                .hint { width: 14px; height: 14px; border-radius: 50%; opacity: 0.4; }
                .hint.W { background: #fff; }
                .hint.B { background: #000; }
                .status { margin: 1rem 0; font-size: 1.1rem; }
                .meta { color: #aaa; font-size: 0.9rem; margin-bottom: 0.5rem; }
                .controls { margin-top: 1rem; }
                button.action { background: #555; color: #eee; border: none; padding: 0.5rem 1rem; cursor: pointer; border-radius: 4px; }
                button.action:hover { background: #666; }
                .htmx-request #board { opacity: 0.6; }
                """
            }
        }
    }
    body {
        h1 { +"Othello" }
        div {
            id = "board-container"
            // The board fragment swaps itself in here. On page load we POST to /api/new-game.
            div {
                id = "board"
                attributes["hx-post"] = "/api/new-game"
                attributes["hx-trigger"] = "load"
                attributes["hx-swap"] = "outerHTML"
            }
        }
        div("controls") {
            button(classes = "action") {
                attributes["hx-post"] = "/api/new-game"
                attributes["hx-target"] = "#board"
                attributes["hx-swap"] = "outerHTML"
                +"New Game"
            }
        }
    }
}

/**
 * Renders just the `<div id="board">` fragment — suitable for HTMX outerHTML swaps.
 * Must NOT be wrapped in an `<html>` document.
 */
fun renderBoardFragment(
    boardState: BoardState,
    validMoves: List<Int>,
    status: GameStatus,
): String = createHTML().div {
    id = "board"

    val white = boardState.whitePositions
    val black = boardState.blackPositions
    val gameOver = status != GameStatus.ONGOING
    val playerToMove = if (boardState.blackToMove) "B" else "W"

    // Hidden inputs that future HTMX requests will pick up via hx-include.
    input(type = InputType.hidden, name = "whitePos") { value = white.toString() }
    input(type = InputType.hidden, name = "blackPos") { value = black.toString() }

    div("meta") {
        +"Turn ${boardState.turnNumber} · "
        +"White ${boardState.whitePieceCount} · "
        +"Black ${boardState.blackPieceCount}"
    }
    div("status") {
        +when (status) {
            GameStatus.ONGOING -> if (boardState.blackToMove) "Black to move" else "White to move"
            GameStatus.WHITE_WINS -> "Game over — White wins"
            GameStatus.BLACK_WINS -> "Game over — Black wins"
            GameStatus.DRAW -> "Game over — Draw"
        }
    }

    // Hidden chain element. After a player move, the server sets HX-Trigger: computerMove,
    // which fires this element's request to make the engine respond.
    if (!gameOver) {
        div {
            attributes["hx-post"] = "/api/computer/move"
            attributes["hx-trigger"] = "computerMove from:body"
            attributes["hx-include"] = "[name='whitePos'],[name='blackPos']"
            attributes["hx-target"] = "#board"
            attributes["hx-swap"] = "outerHTML"
            attributes["style"] = "display:none"
        }
    }

    div("board") {
        // Match BoardState.print orientation: rank 7→0, file 7→0, bit = rank*8 + file.
        for (rank in 7 downTo 0) {
            for (file in 7 downTo 0) {
                val sq = rank * 8 + file
                val whiteHere = (white ushr sq) and 1L == 1L
                val blackHere = (black ushr sq) and 1L == 1L
                val isValid = !gameOver && sq in validMoves

                if (isValid) {
                    button(classes = "square valid") {
                        attributes["hx-post"] = "/api/player/move"
                        attributes["hx-vals"] = """{"square": $sq}"""
                        attributes["hx-include"] = "[name='whitePos'],[name='blackPos']"
                        attributes["hx-target"] = "#board"
                        attributes["hx-swap"] = "outerHTML"
                        div("hint $playerToMove") {}
                    }
                } else {
                    div("square") {
                        when {
                            whiteHere -> div("disc W") {}
                            blackHere -> div("disc B") {}
                        }
                    }
                }
            }
        }
    }
}

fun renderErrorFragment(message: String): String = createHTML().div {
    id = "board"
    div("status") { +"Error: $message" }
}
