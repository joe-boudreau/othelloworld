package com.othelloworld.benchmark

import com.othelloworld.engine.BLACK_STARTING_POSITION
import com.othelloworld.engine.BoardState
import com.othelloworld.engine.GameStatus.BLACK_TO_MOVE
import com.othelloworld.engine.GameStatus.WHITE_TO_MOVE
import com.othelloworld.engine.WHITE_STARTING_POSITION
import java.io.File
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GenerateSeedPositionsTest {

    @Test
    fun `canonical form removes rotations and reflections`() {
        val position = SearchPosition(
            board = BoardState(
                blackPositions = BLACK_STARTING_POSITION or (1L shl 19),
                whitePositions = WHITE_STARTING_POSITION or (1L shl 18),
            ),
            gameStatus = WHITE_TO_MOVE,
        )
        val rotation = SearchPosition(
            board = BoardState(
                blackPositions = WHITE_STARTING_POSITION or (1L shl 29),
                whitePositions = BLACK_STARTING_POSITION or (1L shl 21),
            ),
            gameStatus = WHITE_TO_MOVE,
        )

        assertNotEquals(position, rotation)
        assertEquals(canonicalize(position), canonicalize(rotation))
    }

    @Test
    fun `canonical form retains game status`() {
        val blackToMove = SearchPosition(
            board = BoardState(WHITE_STARTING_POSITION, BLACK_STARTING_POSITION),
            gameStatus = BLACK_TO_MOVE,
        )
        val whiteToMove = blackToMove.copy(gameStatus = WHITE_TO_MOVE)

        assertNotEquals(canonicalize(blackToMove), canonicalize(whiteToMove))
    }

    @Test
    fun `writes the requested CSV format`() {
        val outputFile = File(createTempDirectory().toFile(), "positions.csv")
        val position = BenchmarkPosition(
            position = SearchPosition(
                board = BoardState(WHITE_STARTING_POSITION, BLACK_STARTING_POSITION),
                gameStatus = BLACK_TO_MOVE,
            ),
            stage = BenchmarkStage.EARLY,
            strength = StrengthCategory.EVEN,
            referenceScore = 0.0,
        )

        writePositionsCsv(listOf(position), outputFile)

        assertEquals(
            listOf(
                "Black positions, White positions, game status",
                "${BLACK_STARTING_POSITION}L, ${WHITE_STARTING_POSITION}L, BLACK_TO_MOVE",
            ),
            outputFile.readLines(),
        )
    }

    @Test
    fun `committed benchmark is valid and matches its audit file`() {
        val benchmarkFile = File("benchmark/positions-v1.csv")
        val auditFile = File("benchmark/positions-v1-audit.csv")
        val benchmarkLines = benchmarkFile.readLines()
        val auditLines = auditFile.readLines()
        val positions = auditLines.drop(1).map { line ->
            val columns = line.split(", ")
            BenchmarkPosition(
                position = SearchPosition(
                    board = BoardState(
                        blackPositions = columns[0].removeSuffix("L").toLong(),
                        whitePositions = columns[1].removeSuffix("L").toLong(),
                    ),
                    gameStatus = com.othelloworld.engine.GameStatus.valueOf(columns[2]),
                ),
                stage = BenchmarkStage.entries.single { it.csvValue == columns[3] },
                strength = StrengthCategory.entries.single { it.csvValue == columns[4] },
                referenceScore = columns[5].toDouble(),
            )
        }

        validateBenchmarkPositions(positions)
        assertEquals(
            benchmarkLines.drop(1),
            auditLines.drop(1).map { it.split(", ").take(3).joinToString(", ") },
        )
        assertEquals(
            "453061cec749c36f5422566f5b7ac5236a50ba931b504298c17d36116d640b55",
            sha256(benchmarkFile),
        )
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
