package me.ghostbear.koguma.data.mediaQueryMatch

import me.ghostbear.koguma.domain.mediaQueryMatch.MediaQueryResults
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3)
@Measurement(
    iterations = 5,
    time = 1,
    timeUnit = TimeUnit.SECONDS
)
class InterpreterMediaQueryMatcherBenchmark {
    private lateinit var matcher: InterpreterMediaQueryMatcher

    @Setup
    fun setUp() {
        matcher = InterpreterMediaQueryMatcher()
    }

    @Benchmark
    fun withoutMatch(): MediaQueryResults {
        return matcher.match("Boku no Hero Academia 123")
    }


    @Benchmark
    fun withMatch(): MediaQueryResults {
        return matcher.match("<<Boku no Hero Academia>>")
    }

}