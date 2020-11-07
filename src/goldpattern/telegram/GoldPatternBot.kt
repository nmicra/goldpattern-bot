package com.github.nmicra.goldpattern.telegram

import com.github.nmicra.goldResearch
import com.github.nmicra.goldpattern.db.GoldPatternDBService
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.LocalDateTime


lateinit var BOT_NAME: String
lateinit var BOT_API_KEY: String


class GoldPatternBot : TelegramLongPollingBot() {
    private val logger = LoggerFactory.getLogger("com.github.nmicra.goldpattern.GoldPatternBot")

    override fun getBotToken(): String {
        return BOT_API_KEY
    }

    override fun onUpdateReceived(update: Update) {
        if (update.message.text == "/start") {
            logger.info("new subscriber: ${update.message.chatId}")
            GoldPatternDBService.addNewContact(update.message.chatId.toString())
            execute(
                SendMessage().setChatId(update.message.chatId)
                    .setText("GoldPattern bot will provide you with 3 days price prediction.")
            )
        } else if (update.message.text == "/time") {
            execute(SendMessage().setChatId(update.message.chatId).setText(LocalDateTime.now().toString()))
        } else if (update.message.text == "/pattern") {
            logger.info("pattern requested by subscriber: ${update.message.chatId}")
            goldResearch.updateDataFromYahooFinance()
            val summary = goldResearch.predictPricesSummery()
            execute(SendMessage().setChatId(update.message.chatId).setText(summary))
        }
    }

    override fun getBotUsername(): String {
        return BOT_NAME
    }
}