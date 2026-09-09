package com.othelloworld.benchmark

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.Engine
import com.othelloworld.engine.GameStatus
import com.othelloworld.engine.GameStatus.BLACK_TO_MOVE
import com.othelloworld.engine.STARTING_STATE
import com.othelloworld.engine.algorithms.DecayingEpsilonGreedyWrapper
import com.othelloworld.engine.algorithms.NegamaxWithAlphaBetaSearch
import com.othelloworld.engine.algorithms.RandomSelection
import com.othelloworld.engine.evaluation.v3.V3BoardEvaluator
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

private const val TARGET_POSITION_COUNT = 1_000
private const val REFERENCE_SEARCH_DEPTH = 7
private const val CANDIDATE_SELF_PLAY_SEARCH_DEPTH = 3
private const val CANDIDATE_POOL_PER_MID_OR_LATE_STAGE = 1_500
private const val EARLY_CANDIDATES_PER_LARGE_DEPTH = 400
private const val SCORE_WORKER_COUNT = 8
private const val RANDOM_SEED = 20_260_908L
private const val REFERENCE_WEIGHTS_PATH = "benchmark/reference/v1/weights.json"
private const val DEFAULT_OUTPUT_PATH = "benchmark/positions-v1.csv"
private const val DEFAULT_AUDIT_OUTPUT_PATH = "benchmark/positions-v1-audit.csv"

private const val SELF_PLAY_INITIAL_EPSILON = 0.75
private const val SELF_PLAY_FLOOR_EPSILON = 0.05
private const val SELF_PLAY_EPSILON_DECAY_FACTOR = 0.90

fun main(args: Array<String>) = runBlocking {
    require(args.size <= 2) { "Expected: [benchmark CSV path] [audit CSV path]" }

    val outputFile = File(args.getOrNull(0) ?: DEFAULT_OUTPUT_PATH)
    val auditOutputFile = File(args.getOrNull(1) ?: DEFAULT_AUDIT_OUTPUT_PATH)
    val evaluator = V3BoardEvaluator(REFERENCE_WEIGHTS_PATH)

    println("Generating deterministic benchmark candidates")
    val earlyResult = collectEarlyCandidates()
    val selfPlayResult = collectSelfPlayCandidates(evaluator)
    val candidateResults = listOf(earlyResult, selfPlayResult)
    val candidates = candidateResults.flatMap { it.positions }

    candidateResults.forEach { result ->
        println(
            "${result.source}: ${result.positions.size} candidates, " +
                "${result.exactDuplicates} exact duplicates removed, " +
                "${result.symmetryDuplicates} symmetry duplicates removed"
        )
    }
    BenchmarkStage.entries.forEach { stage ->
        println("${stage.csvValue}: ${candidates.count { it.stage == stage }} candidates")
    }

    println(
        "Scoring ${candidates.size} candidates with frozen V3 weights and " +
            "depth-$REFERENCE_SEARCH_DEPTH search using $SCORE_WORKER_COUNT workers"
    )
    val scoredPositions = scoreCandidates(candidates, evaluator)
    val selectedPositions = selectBenchmarkPositions(scoredPositions)
    validateBenchmarkPositions(selectedPositions)

    writePositionsCsv(selectedPositions, outputFile)
    writeAuditCsv(selectedPositions, auditOutputFile)
    printSelectionReport(selectedPositions)

    println("Wrote ${selectedPositions.size} positions to ${outputFile.absolutePath}")
    println("Wrote selection metadata to ${auditOutputFile.absolutePath}")
    println("Benchmark CSV SHA-256: ${sha256(outputFile)}")
}

internal enum class BenchmarkStage(
    val csvValue: String,
    val pieceCounts: IntRange,
) {
    EARLY("early", 4..12),
    MID("mid", 24..32),
    LATE("late", 44..52),
    ;

    companion object {
        fun fromPieceCount(pieceCount: Int): BenchmarkStage? =
            entries.firstOrNull { pieceCount in it.pieceCounts }
    }
}

internal enum class StrengthCategory(val csvValue: String) {
    EVEN("even"),
    BLACK("black"),
    WHITE("white"),
    STRONG_BLACK("strong-black"),
    STRONG_WHITE("strong-white"),
}

internal data class SearchPosition(
    val board: BoardState,
    val gameStatus: GameStatus,
)

internal data class CandidatePosition(
    val position: SearchPosition,
    val stage: BenchmarkStage,
)

internal data class ScoredPosition(
    val position: SearchPosition,
    val stage: BenchmarkStage,
    val referenceScore: Double,
)

internal data class BenchmarkPosition(
    val position: SearchPosition,
    val stage: BenchmarkStage,
    val strength: StrengthCategory,
    val referenceScore: Double,
)

private data class CandidateResult(
    val source: String,
    val positions: List<CandidatePosition>,
    val exactDuplicates: Long,
    val symmetryDuplicates: Long,
)

private data class StageQuota(
    val even: Int,
    val black: Int,
    val white: Int,
    val strongBlack: Int,
    val strongWhite: Int,
) {
    val total = even + black + white + strongBlack + strongWhite
}

private val stageQuotas = mapOf(
    BenchmarkStage.EARLY to StageQuota(200, 34, 34, 33, 33),
    BenchmarkStage.MID to StageQuota(200, 33, 33, 34, 33),
    BenchmarkStage.LATE to StageQuota(200, 33, 33, 33, 34),
)

private fun collectEarlyCandidates(): CandidateResult {
    val engine = Engine(RandomSelection(Random(RANDOM_SEED)))
    val start = SearchPosition(STARTING_STATE, BLACK_TO_MOVE)
    val exactSeen = hashSetOf(start)
    val symmetrySeen = hashSetOf(canonicalize(start))
    val candidates = mutableListOf<CandidatePosition>()
    var frontier = listOf(start)
    var exactDuplicates = 0L
    var symmetryDuplicates = 0L

    while (frontier.isNotEmpty()) {
        val pieceCount = frontier.first().board.pieceCount()
        val candidateLimit = if (pieceCount >= 9) {
            EARLY_CANDIDATES_PER_LARGE_DEPTH
        } else {
            Int.MAX_VALUE
        }
        frontier.take(candidateLimit).forEach { position ->
            candidates.add(CandidatePosition(position, BenchmarkStage.EARLY))
        }
        println(
            "Early BFS: $pieceCount pieces, ${frontier.size} canonical positions, " +
                "${minOf(frontier.size, candidateLimit)} retained"
        )

        if (pieceCount == BenchmarkStage.EARLY.pieceCounts.last) break

        val nextFrontier = mutableListOf<SearchPosition>()
        for (position in frontier) {
            for (move in engine.getValidMoves(position.board, position.gameStatus)) {
                val (nextBoard, nextStatus) = engine.makePlayerMove(
                    position.board,
                    position.gameStatus,
                    move,
                )
                val exactPosition = SearchPosition(nextBoard, nextStatus)
                val exactIsNew = exactSeen.add(exactPosition)
                val symmetryKey = canonicalize(exactPosition)

                if (symmetrySeen.add(symmetryKey)) {
                    // Keep the first actually reached representative while using
                    // the canonical form only as the symmetry-deduplication key.
                    nextFrontier.add(exactPosition)
                } else if (exactIsNew) {
                    symmetryDuplicates++
                } else {
                    exactDuplicates++
                }
            }
        }
        frontier = nextFrontier
    }

    return CandidateResult(
        source = "early BFS",
        positions = candidates,
        exactDuplicates = exactDuplicates,
        symmetryDuplicates = symmetryDuplicates,
    )
}

private fun collectSelfPlayCandidates(evaluator: V3BoardEvaluator): CandidateResult {
    val positionsByStage = mapOf(
        BenchmarkStage.MID to linkedSetOf<CandidatePosition>(),
        BenchmarkStage.LATE to linkedSetOf(),
    )
    val exactSeenByStage = mapOf(
        BenchmarkStage.MID to hashSetOf<SearchPosition>(),
        BenchmarkStage.LATE to hashSetOf(),
    )
    val symmetrySeenByStage = mapOf(
        BenchmarkStage.MID to hashSetOf<SearchPosition>(),
        BenchmarkStage.LATE to hashSetOf(),
    )
    var exactDuplicates = 0L
    var symmetryDuplicates = 0L
    var gameNumber = 0

    fun stageNeedsCandidates(stage: BenchmarkStage): Boolean =
        positionsByStage.getValue(stage).size < CANDIDATE_POOL_PER_MID_OR_LATE_STAGE

    while (stageNeedsCandidates(BenchmarkStage.MID) || stageNeedsCandidates(BenchmarkStage.LATE)) {
        val gameSeed = RANDOM_SEED + gameNumber * 987_654_327L
        val blackEngine = candidateSelfPlayEngine(evaluator, gameSeed)
        val whiteEngine = candidateSelfPlayEngine(evaluator, gameSeed + 1)
        var board = STARTING_STATE
        var status = BLACK_TO_MOVE

        while (!status.isTerminal() && board.pieceCount() <= BenchmarkStage.LATE.pieceCounts.last) {
            val movingEngine = if (status.blackToMove()) blackEngine else whiteEngine
            val result = movingEngine.makeEngineMove(board, status)
            board = result.first
            status = result.second

            val stage = BenchmarkStage.fromPieceCount(board.pieceCount())
            if (
                stage != null &&
                stage != BenchmarkStage.EARLY &&
                stageNeedsCandidates(stage) &&
                !status.isTerminal()
            ) {
                val exactPosition = SearchPosition(board, status)
                val exactIsNew = exactSeenByStage.getValue(stage).add(exactPosition)
                val symmetryKey = canonicalize(exactPosition)
                val candidate = CandidatePosition(exactPosition, stage)

                if (symmetrySeenByStage.getValue(stage).add(symmetryKey)) {
                    positionsByStage.getValue(stage).add(candidate)
                } else {
                    if (exactIsNew) symmetryDuplicates++ else exactDuplicates++
                }
            }
        }

        gameNumber++
        if (gameNumber % 25 == 0) {
            println(
                "Self-play games: $gameNumber, " +
                    "mid candidates: ${positionsByStage.getValue(BenchmarkStage.MID).size}, " +
                    "late candidates: ${positionsByStage.getValue(BenchmarkStage.LATE).size}"
            )
        }
    }

    println("Self-play candidate collection completed after $gameNumber games")
    return CandidateResult(
        source = "seeded V3 self-play",
        positions = BenchmarkStage.entries.flatMap { stage ->
            positionsByStage[stage]?.toList().orEmpty()
        },
        exactDuplicates = exactDuplicates,
        symmetryDuplicates = symmetryDuplicates,
    )
}

private fun candidateSelfPlayEngine(
    evaluator: V3BoardEvaluator,
    randomSeed: Long,
): Engine = Engine(
    DecayingEpsilonGreedyWrapper(
        internalSelectionAlgorithm = NegamaxWithAlphaBetaSearch(
            searchDepth = CANDIDATE_SELF_PLAY_SEARCH_DEPTH,
            boardEvaluator = evaluator,
            temperature = 0.7,
            randomSeed = randomSeed,
        ),
        initialEpsilon = SELF_PLAY_INITIAL_EPSILON,
        floorEpsilon = SELF_PLAY_FLOOR_EPSILON,
        epsilonDecayFactor = SELF_PLAY_EPSILON_DECAY_FACTOR,
        randomSeed = randomSeed,
    )
)

private suspend fun scoreCandidates(
    candidates: List<CandidatePosition>,
    evaluator: V3BoardEvaluator,
): List<ScoredPosition> = coroutineScope {
    val dispatcher = Dispatchers.Default.limitedParallelism(SCORE_WORKER_COUNT)
    candidates.chunked(100).map { chunk ->
        async(dispatcher) {
            val search = NegamaxWithAlphaBetaSearch(
                searchDepth = REFERENCE_SEARCH_DEPTH,
                boardEvaluator = evaluator,
                temperature = 0.0,
            )
            chunk.map { candidate ->
                ScoredPosition(
                    position = candidate.position,
                    stage = candidate.stage,
                    referenceScore = search.evaluatePosition(
                        candidate.position.board,
                        candidate.position.gameStatus,
                    ),
                )
            }
        }
    }.awaitAll().flatten()
}

internal fun selectBenchmarkPositions(
    scoredPositions: List<ScoredPosition>,
): List<BenchmarkPosition> = BenchmarkStage.entries.flatMap { stage ->
    val stagePositions = scoredPositions.filter { it.stage == stage }
    val quota = stageQuotas.getValue(stage)
    require(stagePositions.size >= quota.total) {
        "${stage.csvValue} has only ${stagePositions.size} candidates for ${quota.total} positions"
    }

    val selected = linkedSetOf<ScoredPosition>()
    val positive = stagePositions.filter { it.referenceScore > 0.0 }
        .sortedWith(scoredPositionComparator(descendingScore = true))
    val negative = stagePositions.filter { it.referenceScore < 0.0 }
        .sortedWith(scoredPositionComparator(descendingScore = false))

    require(positive.size >= quota.strongBlack + quota.black) {
        "${stage.csvValue} has only ${positive.size} Black-favoured candidates"
    }
    require(negative.size >= quota.strongWhite + quota.white) {
        "${stage.csvValue} has only ${negative.size} White-favoured candidates"
    }

    fun take(
        positions: List<ScoredPosition>,
        count: Int,
        strength: StrengthCategory,
    ): List<BenchmarkPosition> = positions.asSequence()
        .filter { it !in selected }
        .take(count)
        .onEach { selected.add(it) }
        .map { it.toBenchmarkPosition(strength) }
        .toList()

    val strongBlack = take(positive, quota.strongBlack, StrengthCategory.STRONG_BLACK)
    val black = take(positive, quota.black, StrengthCategory.BLACK)
    val strongWhite = take(negative, quota.strongWhite, StrengthCategory.STRONG_WHITE)
    val white = take(negative, quota.white, StrengthCategory.WHITE)
    val evenCandidates = stagePositions.sortedWith(
        compareBy<ScoredPosition> { abs(it.referenceScore) }
            .then(scoredPositionComparator(descendingScore = false))
    )
    val even = take(evenCandidates, quota.even, StrengthCategory.EVEN)

    require(even.size == quota.even) {
        "${stage.csvValue} has insufficient remaining candidates for the even category"
    }

    even + black + white + strongBlack + strongWhite
}

private fun ScoredPosition.toBenchmarkPosition(strength: StrengthCategory) = BenchmarkPosition(
    position = position,
    stage = stage,
    strength = strength,
    referenceScore = referenceScore,
)

private fun scoredPositionComparator(descendingScore: Boolean): Comparator<ScoredPosition> {
    val scoreComparator = if (descendingScore) {
        compareByDescending<ScoredPosition> { it.referenceScore }
    } else {
        compareBy<ScoredPosition> { it.referenceScore }
    }
    return scoreComparator
        .thenComparator { left, right -> comparePositions(left.position, right.position) }
}

internal fun canonicalize(position: SearchPosition): SearchPosition =
    BOARD_SYMMETRIES
        .map { transform ->
            SearchPosition(
                board = BoardState(
                    whitePositions = transformBitboard(position.board.whitePositions, transform),
                    blackPositions = transformBitboard(position.board.blackPositions, transform),
                ),
                gameStatus = position.gameStatus,
            )
        }
        .minWith(::comparePositions)

private fun comparePositions(left: SearchPosition, right: SearchPosition): Int {
    val blackComparison = java.lang.Long.compareUnsigned(
        left.board.blackPositions,
        right.board.blackPositions,
    )
    if (blackComparison != 0) return blackComparison

    val whiteComparison = java.lang.Long.compareUnsigned(
        left.board.whitePositions,
        right.board.whitePositions,
    )
    if (whiteComparison != 0) return whiteComparison

    return left.gameStatus.ordinal.compareTo(right.gameStatus.ordinal)
}

private typealias SquareTransform = (row: Int, column: Int) -> Pair<Int, Int>

// Canonical keys use every rotation and reflection. The generator retains the
// first reached representative rather than writing a transformed position.
private val BOARD_SYMMETRIES: List<SquareTransform> = listOf<SquareTransform>(
    { row: Int, column: Int -> row to column },
    { row: Int, column: Int -> column to (7 - row) },
    { row: Int, column: Int -> (7 - row) to (7 - column) },
    { row: Int, column: Int -> (7 - column) to row },
    { row: Int, column: Int -> row to (7 - column) },
    { row: Int, column: Int -> (7 - row) to column },
    { row: Int, column: Int -> column to row },
    { row: Int, column: Int -> (7 - column) to (7 - row) },
)

private fun transformBitboard(bitboard: Long, transform: SquareTransform): Long {
    var transformed = 0L
    for (square in 0 until 64) {
        val mask = 1L shl square
        if (bitboard and mask == 0L) continue

        val row = square / 8
        val column = square % 8
        val (transformedRow, transformedColumn) = transform(row, column)
        transformed = transformed or (1L shl (transformedRow * 8 + transformedColumn))
    }
    return transformed
}

internal fun validateBenchmarkPositions(positions: List<BenchmarkPosition>) {
    require(positions.size == TARGET_POSITION_COUNT) {
        "Expected $TARGET_POSITION_COUNT positions, got ${positions.size}"
    }
    require(positions.map { canonicalize(it.position) }.distinct().size == positions.size) {
        "Benchmark contains exact or symmetry-equivalent duplicate positions"
    }
    require(positions.none { it.position.gameStatus.isTerminal() }) {
        "Benchmark contains terminal positions"
    }
    val engine = Engine(RandomSelection(Random(RANDOM_SEED)))
    require(positions.all {
        engine.getValidMoves(it.position.board, it.position.gameStatus).isNotEmpty()
    }) {
        "Benchmark contains a non-terminal position without a legal move"
    }
    require(positions.all { it.position.board.pieceCount() in it.stage.pieceCounts }) {
        "Benchmark contains a position outside its stage's piece range"
    }

    stageQuotas.forEach { (stage, quota) ->
        val stagePositions = positions.filter { it.stage == stage }
        require(stagePositions.size == quota.total)
        require(stagePositions.count { it.strength == StrengthCategory.EVEN } == quota.even)
        require(stagePositions.count { it.strength == StrengthCategory.BLACK } == quota.black)
        require(stagePositions.count { it.strength == StrengthCategory.WHITE } == quota.white)
        require(stagePositions.count { it.strength == StrengthCategory.STRONG_BLACK } == quota.strongBlack)
        require(stagePositions.count { it.strength == StrengthCategory.STRONG_WHITE } == quota.strongWhite)
    }
}

internal fun writePositionsCsv(positions: List<BenchmarkPosition>, outputFile: File) {
    outputFile.parentFile?.mkdirs()
    outputFile.bufferedWriter().use { writer ->
        writer.write("Black positions, White positions, game status\n")
        positions.forEach { position ->
            writer.write(position.position.toCsvPrefix() + "\n")
        }
    }
}

private fun writeAuditCsv(positions: List<BenchmarkPosition>, outputFile: File) {
    outputFile.parentFile?.mkdirs()
    outputFile.bufferedWriter().use { writer ->
        writer.write(
            "Black positions, White positions, game status, stage, " +
                "strength, reference score\n"
        )
        positions.forEach { position ->
            writer.write(
                "${position.position.toCsvPrefix()}, ${position.stage.csvValue}, " +
                    "${position.strength.csvValue}, ${position.referenceScore}\n"
            )
        }
    }
}

private fun SearchPosition.toCsvPrefix(): String =
    "${board.blackPositions}L, ${board.whitePositions}L, $gameStatus"

private fun printSelectionReport(positions: List<BenchmarkPosition>) {
    println("Final benchmark selection:")
    BenchmarkStage.entries.forEach { stage ->
        StrengthCategory.entries.forEach { strength ->
            val categoryPositions = positions.filter {
                it.stage == stage && it.strength == strength
            }
            val minimum = categoryPositions.minOf { it.referenceScore }
            val maximum = categoryPositions.maxOf { it.referenceScore }
            println(
                "  ${stage.csvValue}/${strength.csvValue}: ${categoryPositions.size}, " +
                    "score range ${formatScore(minimum)} to ${formatScore(maximum)}"
            )
        }
    }
}

private fun formatScore(score: Double): String = String.format(Locale.US, "%.6f", score)

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

private fun BoardState.pieceCount(): Int = blackPieceCount + whitePieceCount
