package com.github.nmicra.scheduled

import com.github.nmicra.bot
import com.github.nmicra.goldResearch
import com.github.nmicra.goldpattern.db.GoldPatternDBService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import java.time.LocalDateTime

private val logger = LoggerFactory.getLogger("com.github.nmicra.ScheduleSendPredictions")

fun scheduleSendPredictions() {
    GlobalScope.launch {
        while (true) {
            delay(1000 * 60 * 60) // 1 hour
            if (LocalDateTime.now().dayOfWeek.value in 1..5){
                if (LocalDateTime.now().hour == 14){
                    logger.info("time to make predictions: ${LocalDateTime.now()}")
                    goldResearch.updateDataFromYahooFinance()
                    val summary = goldResearch.predictPricesSummery()
                    GoldPatternDBService.addNewPrediction(summary)
                    GoldPatternDBService.getAllChatIds().forEach { bot.execute(SendMessage().setChatId(it).setText(summary)) }

                }
                else{
                    logger.info("just skipping, ${LocalDateTime.now()}")
                }
            }

        }
    }
}
