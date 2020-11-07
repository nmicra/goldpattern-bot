package com.github.nmicra.goldpattern.db

const val CREATE_CONTACTS_TABLE = """
    CREATE TABLE IF NOT EXISTS contacts (
	contactId INTEGER PRIMARY KEY AUTOINCREMENT,
	chatId TEXT NOT NULL UNIQUE
    );
"""

const val CREATE_CONTACTS_INDEX = """
    CREATE UNIQUE INDEX IF NOT EXISTS idx_contacts_chatId 
    ON contacts (chatId);
"""

const val INSERT_INTO_CONTACTS_TABLE = """
    insert into contacts(chatId) values(':chatId')
"""

const val SELECT_ALL_CHAT_ID = """
    select chatId from contacts
"""

const val CREATE_PREDICTION_HISTORY_TABLE = """
    CREATE TABLE IF NOT EXISTS prediction_history (
	predictionId INTEGER PRIMARY KEY AUTOINCREMENT,
	prediction_date TEXT NOT NULL,
	prediction_str TEXT NOT NULL
    );
"""

const val CREATE_PREDICTION_HISTORY_INDEX = """
    CREATE UNIQUE INDEX IF NOT EXISTS idx_prediction_history 
    ON prediction_history (prediction_date);
"""

const val INSERT_INTO_PREDICTION_HISTORY_TABLE = """
    insert into prediction_history(prediction_date,prediction_str) values(':prediction_date',':prediction_str')
"""

