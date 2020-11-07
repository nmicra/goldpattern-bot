package com.github.nmicra

import com.github.nmicra.goldpattern.db.DB_FILE_LOCATION
import com.github.nmicra.goldpattern.telegram.BOT_API_KEY
import com.github.nmicra.goldpattern.telegram.BOT_NAME
import com.github.nmicra.goldpattern.telegram.GoldPatternBot
import com.github.nmicra.research.gold.GoldResearch
import com.github.nmicra.scheduled.scheduleSendPredictions
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import java.time.LocalDateTime


val goldResearch = GoldResearch()

lateinit var bot: GoldPatternBot

fun Double.round(decimals: Int = 2): Double = "%.${decimals}f".format(this).toDouble()

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    println("=========== Start: ${LocalDateTime.now()}")
    BOT_NAME = environment.config.property("ktor.application.telegram_bot_name").getString()
    BOT_API_KEY = environment.config.property("ktor.application.telegram_bot_api_key").getString()
    DB_FILE_LOCATION = environment.config.property("ktor.db.file").getString()
    scheduleSendPredictions()
    ApiContextInitializer.init()
    val telegramBotsApi = TelegramBotsApi()
    bot = GoldPatternBot()
    telegramBotsApi.registerBot(bot)

    install(ShutDownUrl.ApplicationCallFeature) {
        // The URL that will be intercepted (you can also use the application.conf's ktor.deployment.shutdown.url key)
        shutDownUrl = "/ktor/application/shutdown"
        // A function that will be executed to get the exit code of the process
        exitCodeSupplier = { 0 } // ApplicationCall.() -> Int
    }

    routing {
        get("/") {
            call.respondText(goldResearch.predictPricesSummery(), contentType = ContentType.Text.Plain)
        }
        get("/pattern") {
            goldResearch.updateDataFromYahooFinance()
            val summary = goldResearch.predictPricesSummery()
            call.respondText(summary, contentType = ContentType.Text.Plain)
        }
    }
}

