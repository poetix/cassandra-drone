package com.opencredo.cassandra.tester

import com.datastax.driver.core.utils.UUIDs
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.stereotype.Component
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.Reader
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import javax.annotation.PostConstruct
import javax.script.Bindings
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

@Component
open class TranspilerService() {

    val engine = ScriptEngineManager().getEngineByName("nashorn")
    val bindings = SimpleBindings()
    val cache: ConcurrentMap<String, String> = ConcurrentHashMap()

    @PostConstruct
    open fun initialise(): Unit {
        engine.eval(read("babel.js"), bindings)
    }

    open fun transpile(script: String): String =
        cache.computeIfAbsent(script) {
            bindings.put("oldJs", script)
            val result = engine.eval("Babel.transform(oldJs, { presets: ['es2015'] }).code", bindings) as String
            println(result)
            result
        }

    private fun read(path: String): Reader = InputStreamReader(
            javaClass.classLoader.getResourceAsStream(path),
            Charset.forName("UTF-8"))
}

@Component
open class ScriptEngineService @Autowired constructor(
        val template: CassandraTemplate,
        val queryRepository: QueryRepository,
        val scriptRepository: ScriptRepository,
        val transpiler: TranspilerService) {

    fun exec(script: String, console: PrintWriter): Unit {
        val engine = ScriptEngineManager().getEngineByName("nashorn")
        val bindings = createBindings(console, engine)

        try {
            execTranspiled(script, engine, bindings)
        } catch (e: Exception) {
            e.printStackTrace(console)
        }
    }

    private fun execTranspiled(script: String, engine: ScriptEngine, bindings: Bindings): Unit {
        engine.eval(transpiler.transpile(script), bindings)
    }

    private fun createBindings(console: PrintWriter, engine: ScriptEngine): Bindings {
        val bindings = SimpleBindings()

        bindings.put("cassandra", template)
        bindings.put("console", console)
        bindings.put("getQuery", Function<String, String> { queryRepository.getQuery(it) })
        bindings.put("parallel", Function<Int, ParallelRunner>(::ParallelRunner))
        bindings.put("uuid", Supplier<String> { UUID.randomUUID().toString() })
        bindings.put("timeuuid", Supplier<String> { UUIDs.timeBased().toString() })
        bindings.put("timer", Function<String, Timer> { Timer(it, console) })

        bindings.put("runQuery", runQueryWith(console))
        bindings.put("include", runScriptWith(engine, bindings))

        return bindings
    }

    private fun runQueryWith(console: PrintWriter): Consumer<String> {
        return Consumer<String> { queryName: String ->
            val query = queryRepository.getQuery(queryName)
                ?: throw IllegalArgumentException("Could not find query $queryName")

            val statements = query.split(";").map(String::trim).filter { it.length > 0 }

            console.println("Executing query $queryName")
            statements.forEach {
                console.println(it)
                template.execute(it)
            }
        }
    }

    private fun runScriptWith(engine: ScriptEngine, bindings: Bindings): Consumer<String> {
        var includesRun = emptySet<String>()
        return Consumer<String> { scriptName: String ->
            if (!includesRun.contains(scriptName)) {
                val script = scriptRepository.getScript(scriptName)
                    ?: throw IllegalArgumentException("Could not find script $scriptName")

                execTranspiled(script, engine, bindings)
                includesRun += scriptName
            }
        }
    }
}

class ParallelRunner(val poolSize: Int) {

    fun run(count: Int, task: Function<Int, *>): List<Any> {
        val executor = Executors.newFixedThreadPool(poolSize)
        try {
            val futures = IntRange(0, count).map { i -> executor.submit(Callable<Any> { task.apply(i) }) }
            return futures.map { it.get() }
        } finally {
            executor.shutdown()
            executor.awaitTermination(30, TimeUnit.SECONDS)
        }
    }

}

class Timer(val description: String, val console: PrintWriter) {
    fun run(task: Callable<*>): Any {
        val start = Instant.now()
        val result = task.call()
        val duration = Duration.between(start, Instant.now())
        console.println("$description in ${duration.toMillis()} milliseconds")
        return result
    }
}