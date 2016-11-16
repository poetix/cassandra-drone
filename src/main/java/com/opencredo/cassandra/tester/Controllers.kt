package com.opencredo.cassandra.tester

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.io.PrintWriter
import java.time.Duration
import java.time.Instant
import javax.servlet.http.HttpServletResponse

@ResponseStatus(HttpStatus.NOT_FOUND)
class ScriptNotFoundException(scriptName: String): RuntimeException("Unable to find script $scriptName")

@ResponseStatus(HttpStatus.NOT_FOUND)
class QueryNotFoundException(queryName: String): RuntimeException("Unable to find query $queryName")

@RestController
@RequestMapping("/scripts")
class ScriptsController @Autowired constructor(
        val scriptRepository: ScriptRepository,
        val scriptEngineService: ScriptEngineService) {

    @RequestMapping("/exec", method = arrayOf(RequestMethod.POST))
    @ResponseBody fun runTest(@RequestBody request: String, response: HttpServletResponse): Unit {
        response.run {
            contentType = "text/plain"
            bufferSize = 0
            status = 200
        }

        val console = PrintWriter(response.outputStream)
        console.println("Runnung script")
        console.println("==============")
        console.flush()

        val start = Instant.now()
        scriptEngineService.exec(request, console)
        console.println("Execution completed in ${Duration.between(start, Instant.now()).toMillis()} milliseconds")

        console.flush()
    }

    @RequestMapping("/", method = arrayOf(RequestMethod.GET))
    @ResponseBody fun getScripts(): Map<String, String> = scriptRepository.getScripts()

    @RequestMapping("/{name}", method = arrayOf(RequestMethod.GET))
    @ResponseBody fun getScript(@PathVariable("name") scriptName: String): String =
            scriptRepository.getScript(scriptName) ?: throw ScriptNotFoundException(scriptName)

    /*
    @RequestMapping("/{name}/exec", method = arrayOf(RequestMethod.POST))
    @ResponseBody fun runScript(@PathVariable("name") scriptName: String): String = runTest(getScript(scriptName))
    */

    @RequestMapping("/{name}", method = arrayOf(RequestMethod.DELETE))
    @ResponseBody fun deleteScript(@PathVariable("name") scriptName: String): String? =
            scriptRepository.deleteScript(scriptName)

    @RequestMapping("/{name}", method = arrayOf(RequestMethod.PUT))
    @ResponseBody fun saveScript(@PathVariable("name") scriptName: String, @RequestBody script: String): String? =
            scriptRepository.saveScript(scriptName, script)
}

@RestController
@RequestMapping("/queries")
class QueriesController @Autowired constructor(
        val queryRepository: QueryRepository) {

    @RequestMapping("/", method = arrayOf(RequestMethod.GET))
    @ResponseBody fun getQueries(): Map<String, String> = queryRepository.getQueries()

    @RequestMapping("/{name}", method = arrayOf(RequestMethod.GET))
    @ResponseBody fun getQuery(@PathVariable("name") queryName: String): String =
            queryRepository.getQuery(queryName) ?: throw QueryNotFoundException(queryName)

    @RequestMapping("/{name}", method = arrayOf(RequestMethod.PUT))
    @ResponseBody fun saveQuery(@PathVariable("name") queryName: String, @RequestBody query: String): String? =
            queryRepository.saveQuery(queryName, query)

    @RequestMapping("/{name}", method = arrayOf(RequestMethod.DELETE))
    @ResponseBody fun deleteQuery(@PathVariable("name") queryName: String): String? =
            queryRepository.deleteQuery(queryName)

}
