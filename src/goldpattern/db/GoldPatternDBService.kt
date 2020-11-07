package com.github.nmicra.goldpattern.db

import io.ktor.http.*
import java.sql.DriverManager
import java.time.LocalDate

lateinit var DB_FILE_LOCATION : String

object GoldPatternDBService {


    // create a database connection
    val connection = DriverManager.getConnection("jdbc:sqlite:$DB_FILE_LOCATION")
    val statement = connection.createStatement().apply { queryTimeout = 30 } // 30 sec timeout

    init {
        statement.executeUpdate(CREATE_CONTACTS_TABLE)
        statement.executeUpdate(CREATE_CONTACTS_INDEX)
        statement.executeUpdate(CREATE_PREDICTION_HISTORY_TABLE)
        statement.executeUpdate(CREATE_PREDICTION_HISTORY_INDEX)
    }

    fun addNewContact(chatId: String) {
        statement.executeUpdate(INSERT_INTO_CONTACTS_TABLE.replace(":chatId", chatId))
    }

    fun addNewPrediction(predictionStr: String) {
        statement.executeUpdate(INSERT_INTO_PREDICTION_HISTORY_TABLE.replace(":prediction_str", predictionStr)
            .replace(":prediction_date", "${LocalDate.now()}"))
    }

    fun getAllChatIds(): Set<String> {
        val chatIdsRs = statement.executeQuery(SELECT_ALL_CHAT_ID)
        val chatIds = mutableSetOf<String>()
        while (chatIdsRs.next()) {
            chatIds.add(chatIdsRs.getString("chatId"))
        }
        return chatIds.toSet()
    }
}