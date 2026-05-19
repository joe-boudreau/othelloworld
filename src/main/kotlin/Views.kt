package com.othelloworld

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import kotlinx.html.*
import kotlinx.html.stream.createHTML

fun renderGamePage(pathPrefix: String = ""): String = createHTML().html {
    head {
        title("Othello World")
        // <base href> makes every relative URL on the page (including HTMX requests)
        // resolve against the proxy prefix. When the header is absent, base="/" is a no-op.
        base { href = "$pathPrefix/" }
        script { src = "https://unpkg.com/htmx.org@2.0.4" }
        script {
            unsafe {
                +"""
                (function() {
                  const KEY = 'othello-game-state';
                  const TERMINAL = ['BLACK_WINS', 'WHITE_WINS', 'DRAW'];

                  function readBoardState() {
                    const board = document.querySelector('#board');
                    if (!board) return null;
                    const get = (n) => board.querySelector('input[name="' + n + '"]');
                    const wp = get('whitePos'), bp = get('blackPos'), algo = get('algorithm'),
                          stat = get('status'), pc = get('playerColor');
                    if (!wp || !bp || !algo || !stat || !pc) return null;
                    return {
                      whitePos: wp.value, blackPos: bp.value, algorithm: algo.value,
                      status: stat.value, playerColor: pc.value
                    };
                  }

                  document.addEventListener('htmx:afterSwap', function() {
                    const s = readBoardState();
                    if (!s) {
                      // Picker (or other non-board content) was swapped in.
                      if (document.querySelector('#board .picker')) localStorage.removeItem(KEY);
                      return;
                    }
                    if (TERMINAL.includes(s.status)) {
                      localStorage.removeItem(KEY);
                      return;
                    }
                    localStorage.setItem(KEY, JSON.stringify(s));
                  });

                  document.addEventListener('DOMContentLoaded', function() {
                    const raw = localStorage.getItem(KEY);
                    if (!raw) return;
                    let s;
                    try { s = JSON.parse(raw); } catch(e) { localStorage.removeItem(KEY); return; }
                    if (!s || TERMINAL.includes(s.status)) { localStorage.removeItem(KEY); return; }
                    htmx.ajax('POST', 'api/resume-game', {
                      target: '#board', swap: 'outerHTML', values: s
                    });
                  });
                })();
                """.trimIndent()
            }
        }
        style {
            unsafe {
                +"""
                * { box-sizing: border-box; }
                body { font-family: ui-monospace, "SF Mono", Menlo, Consolas, monospace; background: #f2f2ef; color: #000; padding: 2rem; margin: 0; width: max-content; }
                h1 { margin: 0 0 1.5rem; font-size: 2rem; font-weight: 700; letter-spacing: -0.02em; text-transform: uppercase; border-bottom: 3px solid #000; padding-bottom: 0.5rem; }
                #board-container { display: block; width: 454px; }
                .board { display: grid; grid-template-columns: repeat(8, 56px); grid-template-rows: repeat(8, 56px); gap: 0; background: #000; padding: 0; border: 3px solid #000; perspective: 800px; }
                .square { background: #2e7d32; display: flex; align-items: center; justify-content: center; padding: 0; border: 1px solid #000; cursor: default; }
                .square.valid { cursor: pointer; background: #2e7d32; }
                .square.valid:hover { background: #4caf50; }
                .square.last-move { outline: 3px solid #ff3b00; outline-offset: -3px; }
                .disc { width: 42px; height: 42px; border-radius: 50%; }
                .disc.W { background: #fff; border: 1px solid #000; }
                .disc.B { background: #000; }

                /* 3D coin used for flipping pieces — two faces with a tiny Z thickness
                   so a thin edge stays visible as the disc rotates through 90°. */
                .disc-3d { width: 42px; height: 42px; position: relative; transform-style: preserve-3d; }
                .disc-3d.flipping { animation: flip 600ms ease-in-out forwards; }
                .disc-3d .face { position: absolute; inset: 0; border-radius: 50%; backface-visibility: hidden; }
                .disc-3d .face.front { transform: translateZ(2px); }
                .disc-3d .face.back  { transform: rotateY(180deg) translateZ(2px); }
                .disc-3d .face.W { background: #fff; border: 1px solid #000; }
                .disc-3d .face.B { background: #000; }
                @keyframes flip {
                    from { transform: rotateY(0deg); }
                    to   { transform: rotateY(180deg); }
                }
                .hint { width: 14px; height: 14px; border-radius: 50%; opacity: 0.35; }
                .hint.W { background: #fff; border: 1px solid #000; }
                .hint.B { background: #000; }
                .meta { color: #000; font-size: 0.75rem; margin-bottom: 0.5rem; text-transform: uppercase; letter-spacing: 0.05em; opacity: 0.7; }
                .turn-bar { display: flex; align-items: center; gap: 0.75rem; padding: 0.6rem 0.8rem; margin-bottom: 0.5rem; border: 3px solid #000; font-size: 0.95rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; }
                .turn-bar.player { background: #000; color: #f2f2ef; }
                .turn-bar.engine { background: #f2f2ef; color: #000; }
                .turn-bar.gameover { background: #ff3b00; color: #000; justify-content: center; }
                .turn-bar.gameover.lose { background: #000; color: #ff3b00; }
                .turn-bar.gameover.draw { background: #f2f2ef; color: #000; }
                .turn-disc { width: 22px; height: 22px; border-radius: 50%; flex-shrink: 0; }
                .turn-disc.W { background: #fff; border: 1px solid #000; }
                .turn-disc.B { background: #000; border: 1px solid #f2f2ef; }
                .turn-label { flex: 1; }
                .thinking-blocks { display: flex; gap: 4px; }
                .thinking-blocks .blk { width: 14px; height: 14px; background: #000; animation: think 900ms steps(1) infinite; }
                .thinking-blocks .blk:nth-child(2) { animation-delay: 150ms; }
                .thinking-blocks .blk:nth-child(3) { animation-delay: 300ms; }
                @keyframes think { 0%, 50% { opacity: 1; } 50.01%, 100% { opacity: 0.15; } }
                .htmx-request .turn-bar.engine { background: #000; color: #f2f2ef; }
                .htmx-request .turn-bar.engine .thinking-blocks .blk { background: #f2f2ef; }
                .controls { margin-top: 1.5rem; }
                button.action { background: #000; color: #f2f2ef; border: 3px solid #000; padding: 0.5rem 1rem; cursor: pointer; border-radius: 0; font-family: inherit; font-size: 0.9rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; }
                button.action:hover { background: #f2f2ef; color: #000; }
                body:has(.picker) button.action { opacity: 0.4; pointer-events: none; cursor: not-allowed; }
                .htmx-request #board { opacity: 0.6; }

                /* Color-picker shown in #board on initial load and on New Game. */
                .picker { display: block; padding: 1.25rem; background: #f2f2ef; border: 3px solid #000; width: 454px; min-height: 454px; }
                .picker h2 { margin: 0 0 1rem; font-size: 1.1rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 1px solid #000; padding-bottom: 0.5rem; }
                .picker .choices { display: flex; gap: 0; }
                .picker button { background: #f2f2ef; color: #000; border: 2px solid #000; border-right-width: 0; padding: 0.75rem 1rem; cursor: pointer; border-radius: 0; font-family: inherit; font-size: 0.9rem; display: flex; flex-direction: row; align-items: center; gap: 0.6rem; flex: 1; text-align: left; }
                .picker button:last-child { border-right-width: 2px; }
                .picker button:hover { background: #000; color: #f2f2ef; }
                .picker button:hover .swatch.W { border-color: #f2f2ef; }
                .picker .swatch { width: 24px; height: 24px; border-radius: 50%; flex-shrink: 0; }
                .picker .swatch.W { background: #fff; border: 1px solid #000; }
                .picker .swatch.B { background: #000; border: 1px solid #000; }
                .picker .swatch.R { background: linear-gradient(135deg, #fff 0%, #fff 49%, #000 51%, #000 100%); border: 1px solid #000; }
                .picker h3 { margin: 1.5rem 0 0.75rem; font-size: 0.9rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: #000; border-bottom: 1px solid #000; padding-bottom: 0.5rem; }
                .picker .algo-choices { display: grid; grid-template-columns: 1fr 1fr; gap: 0; }
                .picker .algo-choice { display: flex; flex-direction: row; align-items: center; gap: 0.5rem; background: #f2f2ef; border: 2px solid #000; border-right-width: 0; border-bottom-width: 0; padding: 0.6rem 0.75rem; border-radius: 0; cursor: pointer; font-size: 0.8rem; }
                .picker .algo-choice:nth-child(2n) { border-right-width: 2px; }
                .picker .algo-choice:nth-last-child(-n+2) { border-bottom-width: 2px; }
                .picker .algo-choice:hover { background: #e0e0dc; }
                .picker .algo-choice input { margin: 0; accent-color: #000; }
                .picker .algo-choice:has(input:checked) { background: #000; color: #f2f2ef; }
                .picker .algo-choice div { line-height: 1.2; }

                /* Modal shown briefly when a side has to pass its move. */
                .pass-modal { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; animation: pass-fade 3s ease-in-out forwards; }
                .pass-modal-content { background: #f2f2ef; border: 3px solid #000; padding: 1.5rem 2rem; border-radius: 0; font-size: 1rem; font-weight: 700; text-transform: uppercase; text-align: center; max-width: 400px; color: #000; }
                @keyframes pass-fade { 0% { opacity: 0 } 10% { opacity: 1 } 85% { opacity: 1 } 100% { opacity: 0 } }
                """
            }
        }
    }
    body {
        h1 { +"Othello World" }
        div {
            id = "board-container"
            // Initial #board content is the color picker. Choosing a color swaps in the
            // actual rendered board. The "New Game" button below brings the picker back.
            unsafe { +renderColorPickerFragment() }
        }
        div("controls") {
            button(classes = "action") {
                attributes["hx-get"] = "api/new-game-modal"
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
    status: GameStatus,
    algorithm: String,
    validMoves: List<Int>,
    lastMove: Int? = null,
    flippedSquares: List<Int> = emptyList(),
    playerPlaysNext: Boolean,
    playerJustPlayed: Boolean?,
): String = createHTML().div {
    id = "board"

    // playerJustPlayed == null means the game just started — no pass modal in that case.
    val computerPassing = playerJustPlayed == true && playerPlaysNext && !status.isTerminal()
    val playerPassing = playerJustPlayed == false && !playerPlaysNext && !status.isTerminal()

    if (computerPassing || playerPassing) {
        div("pass-modal") {
            div("pass-modal-content") {
                if (computerPassing) {
                    +"Computer has no legal moves — your turn again!"
                } else {
                    +"You have no legal moves — computer plays again."
                }
            }
        }
        // Auto-dismiss the modal after the fade-out animation. When the player is
        // passing, also fire the computerMove event so the engine takes its (second)
        // turn — Routing intentionally skips HX-Trigger-After-Settle for this case
        // so the player has time to read the message.
        script {
            unsafe {
                +if (playerPassing) {
                    "setTimeout(() => { document.querySelector('.pass-modal')?.remove(); htmx.trigger(document.body, 'computerMove'); }, 3000);"
                } else {
                    "setTimeout(() => { document.querySelector('.pass-modal')?.remove(); }, 3000);"
                }
            }
        }
    }

    val white = boardState.whitePositions
    val black = boardState.blackPositions
    val gameOver = status.isTerminal()
    val playerToMove = if (status.blackToMove()) "B" else "W"
    val playerIsBlack = if (playerPlaysNext) status.blackToMove() else !status.blackToMove()
    val playerColor = if (playerIsBlack) "B" else "W"

    // Hidden inputs that future HTMX requests will pick up via hx-include.
    input(type = InputType.hidden, name = "whitePos") { value = white.toString() }
    input(type = InputType.hidden, name = "blackPos") { value = black.toString() }
    input(type = InputType.hidden, name = "algorithm") { value = algorithm }
    input(type = InputType.hidden, name = "status") { value = status.name }
    input(type = InputType.hidden, name = "playerColor") { value = playerColor }

    div("meta") {
        +"TURN ${boardState.turnNumber} / W ${boardState.whitePieceCount} / B ${boardState.blackPieceCount} / "
        +algorithm.uppercase()
    }

    when {
        gameOver -> {
            val resultClass = when (status) {
                GameStatus.WHITE_WINS -> if (playerIsBlack) "lose" else "win"
                GameStatus.BLACK_WINS -> if (playerIsBlack) "win" else "lose"
                else -> "draw"
            }
            val resultText = when (status) {
                GameStatus.WHITE_WINS -> "GAME OVER / WHITE WINS"
                GameStatus.BLACK_WINS -> "GAME OVER / BLACK WINS"
                GameStatus.DRAW -> "GAME OVER / DRAW"
                else -> ""
            }
            div("turn-bar gameover $resultClass") { +resultText }
        }
        playerPlaysNext -> {
            div("turn-bar player") {
                div("turn-disc $playerColor") {}
                span("turn-label") { +"YOUR MOVE" }
            }
        }
        else -> {
            div("turn-bar engine") {
                span("turn-label") { +"ENGINE THINKING" }
                div("thinking-blocks") {
                    div("blk") {}
                    div("blk") {}
                    div("blk") {}
                }
            }
        }
    }

    // Hidden chain element. After a player move, the server sets HX-Trigger: computerMove,
    // which fires this element's request to make the engine respond.
    if (!gameOver) {
        val delayMs = when (algorithm) {
            "random", "greedy" -> 1000
            else -> 500
        }
        div {
            attributes["hx-post"] = "api/computer/move"
            attributes["hx-trigger"] = "computerMove from:body delay:${delayMs}ms"
            attributes["hx-include"] = "[name='whitePos'],[name='blackPos'],[name='algorithm'],[name='status']"
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
                val isValid = playerPlaysNext && !gameOver && sq in validMoves

                if (isValid) {
                    button(classes = "square valid") {
                        attributes["hx-post"] = "api/player/move"
                        attributes["hx-vals"] = """{"square": $sq}"""
                        attributes["hx-include"] = "[name='whitePos'],[name='blackPos'],[name='algorithm'],[name='status']"
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

/**
 * Renders the color picker as a `<div id="board">` fragment so it can be swapped
 * into the same target as the board. Each choice POSTs to /api/new-game with a
 * `color` parameter (B, W, or R for random).
 */
fun renderColorPickerFragment(): String = createHTML().div {
    id = "board"
    div("picker") {
        h2 { +"Choose your color" }
        div("choices") {
            listOf(
                Triple("B", "Black", "Plays first"),
                Triple("W", "White", "Plays second"),
                Triple("R", "Random", "Coin flip"),
            ).forEach { (code, colorLabel, sub) ->
                button {
                    attributes["hx-post"] = "api/new-game"
                    attributes["hx-vals"] = """{"color": "$code", "status": "BLACK_TO_MOVE"}"""
                    attributes["hx-include"] = "[name='algorithm']"
                    attributes["hx-target"] = "#board"
                    attributes["hx-swap"] = "outerHTML"
                    div("swatch $code") {}
                    div {
                        div { style = "font-weight: 700; text-transform: uppercase;"; +colorLabel }
                        div { style = "font-size: 0.75rem; opacity: 0.7;"; +sub }
                    }
                }
            }
        }
        h3 { +"Engine algorithm" }
        div("algo-choices") {
            val algos = listOf(
                Triple("negamax-alpha-beta", "NEGAMAX + αβ", "Pruned search"),
                Triple("negamax", "NEGAMAX", "Looks ahead"),
                Triple("greedy", "GREEDY", "Maximizes pieces"),
                Triple("random", "RANDOM", "Picks randomly"),
            )
            algos.forEach { (code, algoLabel, sub) ->
                label("algo-choice") {
                    input(type = InputType.radio, name = "algorithm") {
                        value = code
                        if (code == "negamax") checked = true
                    }
                    div {
                        div { style = "font-weight: 700;"; +algoLabel }
                        div { style = "font-size: 0.7rem; opacity: 0.7;"; +sub }
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
