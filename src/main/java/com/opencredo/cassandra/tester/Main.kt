package com.opencredo.cassandra.tester

import org.mapdb.DB
import org.mapdb.DBMaker
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
open class Application {

    @Bean
    open fun db():DB = DBMaker.fileDB("storage").closeOnJvmShutdown().make();

}

fun main(args: Array<String>): Unit {
    SpringApplication.run(Application::class.java, *args);
}
