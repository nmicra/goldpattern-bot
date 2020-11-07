package com.github.nmicra

import com.github.nmicra.goldpattern.db.DB_FILE_LOCATION
import com.github.nmicra.goldpattern.db.GoldPatternDBService
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.server.engine.*
import kotlin.test.*
import io.ktor.server.testing.*
import java.io.File
import java.nio.file.FileSystem
import java.time.LocalDate

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }


    @Test
    fun testDb() {
        DB_FILE_LOCATION = "./goldpatternTest.db"
        try{
            File(DB_FILE_LOCATION).delete()
            GoldPatternDBService.addNewContact("c1")
            GoldPatternDBService.addNewContact("c2")
            GoldPatternDBService.addNewPrediction("bla bla bla")
            assert(GoldPatternDBService.getAllChatIds().contains("c1"))
            assert(GoldPatternDBService.getAllChatIds().contains("c2"))
        } finally {
            File(DB_FILE_LOCATION).delete()
        }
        println("Done!")
    }
}
