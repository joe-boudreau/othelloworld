package com.othelloworld.benchmark

import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus
import java.io.File

const val DEFAULT_SEED_POSITIONS_PATH = "benchmark/seed-positions.csv"

const val SEED_POSITIONS_CSV_HEADER =
    "Black positions, White positions, game status, stage, strength, reference score"

enum class BenchmarkStage(
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

        fun fromCsvValue(value: String): BenchmarkStage =
            entries.firstOrNull { it.csvValue == value }
                ?: throw IllegalArgumentException("Unknown benchmark stage: $value")
    }
}

enum class StrengthCategory(val csvValue: String) {
    EVEN("even"),
    BLACK("black"),
    WHITE("white"),
    STRONG_BLACK("strong-black"),
    STRONG_WHITE("strong-white"),
    ;

    companion object {
        fun fromCsvValue(value: String): StrengthCategory =
            entries.firstOrNull { it.csvValue == value }
                ?: throw IllegalArgumentException("Unknown strength category: $value")
    }
}

data class SeedPosition(
    val board: BoardState,
    val gameStatus: GameStatus,
    val stage: BenchmarkStage,
    val strength: StrengthCategory,
    val referenceScore: Double,
)

class SeedPositionsRepository(
    private val file: File = File(DEFAULT_SEED_POSITIONS_PATH),
) {
    constructor(path: String) : this(File(path))

    fun read(): List<SeedPosition> {
        require(file.isFile) { "Seed positions file does not exist: ${file.absolutePath}" }

        val lines = file.readLines()
        require(lines.isNotEmpty()) { "Seed positions file is empty: ${file.absolutePath}" }
        require(lines.first() == SEED_POSITIONS_CSV_HEADER) {
            "Unexpected seed positions CSV header in ${file.absolutePath}: ${lines.first()}"
        }

        return lines.drop(1).mapIndexed { index, line ->
            require(line.isNotBlank()) { "Blank CSV row at line ${index + 2}" }
            parseRow(line, index + 2)
        }
    }

    fun write(positions: List<SeedPosition>) {
        file.parentFile?.mkdirs()
        file.bufferedWriter().use { writer ->
            writer.write(SEED_POSITIONS_CSV_HEADER)
            writer.write("\n")
            positions.forEach { position ->
                writer.write(position.toCsvRow())
                writer.write("\n")
            }
        }
    }

    private fun parseRow(row: String, lineNumber: Int): SeedPosition {
        val columns = row.split(',').map(String::trim)
        require(columns.size == 6) {
            "Expected 6 columns at line $lineNumber, got ${columns.size}"
        }

        return try {
            val blackPositions = columns[0].parseLongLiteral("Black positions")
            val whitePositions = columns[1].parseLongLiteral("White positions")
            val referenceScore = columns[5].toDouble()
            require(referenceScore.isFinite()) { "reference score must be finite" }

            SeedPosition(
                board = BoardState(
                    whitePositions = whitePositions,
                    blackPositions = blackPositions,
                ),
                gameStatus = GameStatus.valueOf(columns[2]),
                stage = BenchmarkStage.fromCsvValue(columns[3]),
                strength = StrengthCategory.fromCsvValue(columns[4]),
                referenceScore = referenceScore,
            )
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid seed position at line $lineNumber: ${exception.message}",
                exception,
            )
        }
    }

    private fun String.parseLongLiteral(columnName: String): Long {
        require(endsWith('L')) { "$columnName must use the Kotlin Long suffix L" }
        return dropLast(1).toLong()
    }

    private fun SeedPosition.toCsvRow(): String =
        "${board.blackPositions}L, ${board.whitePositions}L, $gameStatus, " +
            "${stage.csvValue}, ${strength.csvValue}, $referenceScore"
}
