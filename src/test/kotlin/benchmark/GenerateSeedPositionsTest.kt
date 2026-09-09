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
    fun `repository round trips the complete CSV schema`() {
        val outputFile = File(createTempDirectory().toFile(), "positions.csv")
        val position = SeedPosition(
            board = BoardState(WHITE_STARTING_POSITION, BLACK_STARTING_POSITION),
            gameStatus = BLACK_TO_MOVE,
            stage = BenchmarkStage.EARLY,
            strength = StrengthCategory.EVEN,
            referenceScore = -0.125,
        )
        val repository = SeedPositionsRepository(outputFile)

        repository.write(listOf(position))

        assertEquals(
            listOf(
                "Black positions, White positions, game status, stage, strength, reference score",
                "${BLACK_STARTING_POSITION}L, ${WHITE_STARTING_POSITION}L, " +
                    "BLACK_TO_MOVE, early, even, -0.125",
            ),
            outputFile.readLines(),
        )
        assertEquals(listOf(position), repository.read())
    }

    @Test
    fun `committed seed positions benchmark is readable and valid`() {
        val benchmarkFile = File("benchmark/seed-positions.csv")
        val positions = SeedPositionsRepository(benchmarkFile).read()

        validateBenchmarkPositions(positions)
        assertEquals(
            "42d3b7bfe1132c7baacc02ef4665339beeab91bb395268255ecf7e44890a7cf5",
            sha256(benchmarkFile),
        )
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
