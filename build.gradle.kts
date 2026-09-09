plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    kotlin("plugin.serialization") version "2.2.20"

}

group = "com.othelloworld"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.htmlBuilder)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.htmx)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}

tasks.register<BenchmarkTask>("benchmark") {
    group = "application"
    description = "Runs an Othello benchmark."
    runtimeClasspath.from(sourceSets["main"].runtimeClasspath)
}

tasks.register<JavaExec>("generateBenchmarkPositions") {
    group = "application"
    description = "Generates the fixed, curated Othello benchmark position set."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.othelloworld.benchmark.GenerateSeedPositionsKt")
}
