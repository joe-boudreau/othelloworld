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
                .board { display: grid; grid-template-columns: repeat(8, 56px); grid-template-rows: repeat(8, 56px); gap: 2px; background: #1a1a1a; padding: 4px; border: 2px solid #1a1a1a; perspective: 800px; }
                .square { background: #2e7d32; display: flex; align-items: center; justify-content: center; padding: 0; border: none; cursor: default; }
                .square.valid { cursor: pointer; background: #388e3c; }
                .square.valid:hover { background: #4caf50; }
                .square.last-move { box-shadow: inset 0 0 0 3px #ffd54f; }
                .disc { width: 42px; height: 42px; border-radius: 50%; }
                .disc.W { background: radial-gradient(circle at 30% 30%, #fff, #d0d0d0); }
                .disc.B { background: radial-gradient(circle at 30% 30%, #555, #000); }

                /* 3D coin used for flipping pieces — two faces with a tiny Z thickness
                   so a thin edge stays visible as the disc rotates through 90°. */
                .disc-3d { width: 42px; height: 42px; position: relative; transform-style: preserve-3d; }
                .disc-3d.flipping { animation: flip 600ms ease-in-out forwards; }
                .disc-3d .face { position: absolute; inset: 0; border-radius: 50%; backface-visibility: hidden; }
                .disc-3d .face.front { transform: translateZ(2px); }
                .disc-3d .face.back  { transform: rotateY(180deg) translateZ(2px); }
                .disc-3d .face.W { background: radial-gradient(circle at 30% 30%, #fff, #d0d0d0); }
                .disc-3d .face.B { background: radial-gradient(circle at 30% 30%, #555, #000); }
                @keyframes flip {
                    from { transform: rotateY(0deg); }
                    to   { transform: rotateY(180deg); }
                }
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
    lastMove: Int? = null,
    flippedSquares: List<Int> = emptyList(),
    interactive: Boolean = true,
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
                val isValid = interactive && !gameOver && sq in validMoves

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
                    val classes = if (sq == lastMove) "square last-move" else "square"
                    val flipping = sq in flippedSquares
                    div(classes) {
                        when {
                            whiteHere && flipping -> flippingDisc(toColor = "W")
                            blackHere && flipping -> flippingDisc(toColor = "B")
                            whiteHere -> div("disc W") {}
                            blackHere -> div("disc B") {}
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a piece mid-flip. The wrapper rotates 0→180deg; we put the *previous* color
 * on the front face (visible at 0) and the *new* color on the back (visible at 180),
 * so the animation visually carries the piece from old → new color.
 */
private fun FlowContent.flippingDisc(toColor: String) {
    val fromColor = if (toColor == "W") "B" else "W"
    div("disc-3d flipping") {
        div("face front $fromColor") {}
        div("face back $toColor") {}
    }
}

fun renderErrorFragment(message: String): String = createHTML().div {
    id = "board"
    div("status") { +"Error: $message" }
}
