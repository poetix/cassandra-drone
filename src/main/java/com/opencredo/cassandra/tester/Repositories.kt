package com.opencredo.cassandra.tester

import org.mapdb.DB
import org.mapdb.Serializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentMap

@Component
open class ScriptRepository @Autowired constructor(db: DB) {

    val scripts: ConcurrentMap<String, String> =
            db.hashMap("scripts", Serializer.STRING, Serializer.STRING).createOrOpen()

    fun getScripts(): Map<String, String> = scripts

    fun getScript(scriptName: String): String? =
            scripts[scriptName]

    fun deleteScript(scriptName: String): String? =
            scripts.remove(scriptName)

    fun saveScript(scriptName: String, script: String): String? =
            scripts.put(scriptName, script)

}

@Component
open class QueryRepository @Autowired constructor(db: DB) {

    val queries: ConcurrentMap<String, String> =
            db.hashMap("queries", Serializer.STRING, Serializer.STRING).createOrOpen()

    fun getQueries(): Map<String, String> = queries
    fun getQuery(queryName: String): String? =
            queries[queryName]

    fun saveQuery(queryName: String, query: String): String? =
            queries.put(queryName, query)

    fun deleteQuery(queryName: String): String? =
            queries.remove(queryName)
}
