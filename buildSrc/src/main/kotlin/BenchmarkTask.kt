import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class BenchmarkTask : DefaultTask() {
    private var p1WeightsValue = ""
    private var p2WeightsValue = ""
    private var p1SearchDepthValue = 4
    private var p2SearchDepthValue = 4
    private var numGamesValue = 10


    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Classpath
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:Input
    val p1Weights: String
        get() = p1WeightsValue

    @get:Input
    val p2Weights: String
        get() = p2WeightsValue

    @get:Input
    val p1SearchDepth: Int
        get() = p1SearchDepthValue

    @get:Input
    val p2SearchDepth: Int
        get() = p2SearchDepthValue

    @get:Input
    val numGames: Int
        get() = numGamesValue

    @Option(option = "p1-weights", description = "Path to player 1 weights file")
    fun setP1Weights(value: String) {
        p1WeightsValue = value
    }

    @Option(option = "p2-weights", description = "Path to player 2 weights file")
    fun setP2Weights(value: String) {
        p2WeightsValue = value
    }

    @Option(option = "p1-search-depth", description = "P1 search depth")
    fun setP1SearchDepth(value: String) {
        p1SearchDepthValue = value.toIntOrNull()
            ?: throw IllegalArgumentException("--p1-search-depth must be an integer")
    }

    @Option(option = "p2-search-depth", description = "P2 search depth")
    fun setP2SearchDepth(value: String) {
        p2SearchDepthValue = value.toIntOrNull()
            ?: throw IllegalArgumentException("--p2-search-depth must be an integer")
    }

    @Option(option = "num-games", description = "Number of games to run")
    fun setNumGames(value: String) {
        numGamesValue = value.toIntOrNull()
            ?: throw IllegalArgumentException("--num-games must be an integer")
    }

    @TaskAction
    fun runBenchmark() {
        require(p1Weights.isNotBlank()) { "Provide --p1-weights=<path>" }
        require(p2Weights.isNotBlank()) { "Provide --p2-weights=<path>" }

        execOperations.javaexec {
            classpath = runtimeClasspath
            mainClass.set("com.othelloworld.benchmark.BenchmarkMainKt")
            args(p1Weights, p2Weights, p1SearchDepthValue.toString(), p2SearchDepthValue.toString(), numGamesValue.toString())
        }
    }
}
