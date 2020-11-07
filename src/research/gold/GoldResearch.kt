package com.github.nmicra.research.gold

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.nmicra.round
import org.slf4j.LoggerFactory
import yahoofinance.YahooFinance
import yahoofinance.histquotes.Interval
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs


/**
 * delta have to be at least 10% to switch between min <--> max points.
 */
const val MIN_MAX_PERCENTAGE_DELTA = 10

var lastUpdateFromYahooFinance : LocalDate = LocalDate.now().minusDays(1)

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

private val logger = LoggerFactory.getLogger("com.github.nmicra.goldpattern.GoldResearch")

class GoldResearch {
    private val records = mutableListOf<GoldFuturesRecord>()

    constructor(){
        val goldFuturesRawData: List<List<String>> = this::class.java.classLoader.getResource("Gold_Futures_Historical_Data_2020.csv").readText()
            .let {csvReader().readAll(it)}


        goldFuturesRawData.drop(1).forEach {
            val r = GoldFuturesRecord(
                LocalDate.parse(it.get(0), formatter),
                it.get(1).replace(",", "").toDouble(),
                it.get(2).replace(",", "").toDouble(),
                it.get(3).replace(",", "").toDouble(),
                it.get(4).replace(",", "").toDouble(),
                it.get(5),
                it.get(6).dropLast(1).toDouble()
            )
            records.add(r)
        }
        records.sortBy { it.date }
    }


    fun predictPricesSummery() : String {
        var predictionStr = ""

        val predictPrices = predictPrices()
        if (predictPrices.isEmpty()) return "no appropriate pattern was found today"
        predictPrices.forEach {
            predictionStr += """
                predictionType = [${it.type}]
                predictionStrength = [${it.matchedPattern.length}]
                3DaysPrediction = [${it.predictDay1}, ${it.predictDay2}, ${it.predictDay3}]
                
            """.trimIndent()
        }
        val d1 = predictPrices.map { it.predictDay1 }.average()
        val d2 = predictPrices.map { it.predictDay2 }.average()
        val d3 = predictPrices.map { it.predictDay3 }.average()
        predictionStr += """
                ----------------------------------------------------------------------
                AveragePrediction = [$d1, $d2, $d3]
            """.trimIndent()
        return predictionStr
    }
    fun predictPrices() : List<Prediction> {
        val encodedChanges = encodePriceDifference(records.map { it.changePercent })
        val exactPredictionLst = mutableListOf<Prediction>()
        val fuzzyPredictionLst = mutableListOf<Prediction>()
        println("encodes str: $encodedChanges")

        var i = 2
        while (i < encodedChanges.length){
            val patternToVerify = encodePriceDifference(records.takeLast(i).map { it.changePercent })
            val encodedStrForSearch = encodedChanges.substring(0, encodedChanges.length - i)
            val matchedIndex = encodedStrForSearch.indexOf(patternToVerify)
            if (matchedIndex + patternToVerify.length + 3 > records.size - patternToVerify.length) {
                i++
                continue
            }
            if (matchedIndex > 0) {
                val prediction = records.subList(
                    matchedIndex + patternToVerify.length,
                    matchedIndex + patternToVerify.length + 3
                )
                val match = Prediction(
                    patternToVerify,
                    prediction[0].changePercent,
                    prediction[1].changePercent,
                    prediction[2].changePercent
                )
                exactPredictionLst.removeAll { it.matchedPattern.isNotEmpty() }
                exactPredictionLst.add(match)
            } else {
                val listFuzzyMatch = fuzzyStrings(patternToVerify).filter { encodedStrForSearch.indexOf(it) >= 0 }
                if (listFuzzyMatch.isEmpty()) break

                fuzzyPredictionLst.removeAll { it.matchedPattern.isNotEmpty() }
                listFuzzyMatch.forEach {
                    val idx = encodedStrForSearch.indexOf(it)
                    val prediction = records.subList(idx + it.length, idx + it.length + 3)
                    val match = Prediction(
                        it,
                        prediction[0].changePercent,
                        prediction[1].changePercent,
                        prediction[2].changePercent,
                        type = "fuzzy"
                    )
                    fuzzyPredictionLst.add(match)
//                    conclusionStr = "$conclusionStr \n-----> original substring: $patternToVerify, found fuzzyStr: $it at index: $idx"
//                    records.subList(idx, idx+ it.length + 2).forEach { conclusionStr = "$conclusionStr \n $it" }
                }
            }

            i++
        }
//        val maxFuzzyPatternLength = fuzzyPredictionLst.maxByOrNull { it.matchedPattern.length }!!.matchedPattern.length
//        return exactPredictionLst + fuzzyPredictionLst.filter { it.matchedPattern.length == maxFuzzyPatternLength }
        return exactPredictionLst + fuzzyPredictionLst
    }


    private fun getCriticalPoints(list: List<GoldFuturesRecord>): List<GoldFuturesRecord> {
        fun priceWithDelts(price: Double): Double = (price * (100 + MIN_MAX_PERCENTAGE_DELTA)) / 100
        val stack = Stack<GoldFuturesRecord>()
        stack.push(list[0])
        list.drop(1).forEach {
            when (stack.peek().minMax) {
                "min" -> {
                    when {
                        stack.peek().price > it.price -> {
                            stack.pop()
                            stack.push(it.copy(minMax = "min"))
                        }
                        priceWithDelts(stack.peek().price) <= it.price -> stack.push(it.copy(minMax = "max"))
                    }
                }
                "max" -> {
                    when {
                        stack.peek().price < it.price -> {
                            stack.pop()
                            stack.push(it.copy(minMax = "max"))
                        }
                        stack.peek().price >= priceWithDelts(it.price) -> stack.push(it.copy(minMax = "min"))
                    }
                }
                else -> error("NOT SUPPORTED")
            }
        }
        return stack.toList()
    }

    private fun encodePriceDifference(prices: List<Double>): String {
        fun encode(pr: Double): Char = when {
            pr >=0 && pr < 0.5 -> 'k'
            pr >=0.5 && pr < 1 -> 'j'
            pr >=1 && pr < 1.5 -> 'i'
            pr >=1.5 && pr < 2 -> 'h'
            pr >=2 && pr < 2.5 -> 'g'
            pr >=2.5 && pr < 3 -> 'f'
            pr >=3 && pr < 4 -> 'e'
            pr >=4 && pr < 5 -> 'd'
            pr >=5 && pr < 6 -> 'c'
            pr >=6 && pr < 7 -> 'b'
            pr >=7 -> 'a'
            // ------------------------
            pr < 0 && pr > -0.5 -> 'p'
            pr <=-0.5 && pr > -1 -> 'q'
            pr <=-1 && pr > -1.5 -> 'r'
            pr <=-1.5 && pr > -2 -> 's'
            pr <=-2 && pr > -2.5 -> 't'
            pr <=-2.5 && pr > -3 -> 'u'
            pr <=-3 && pr > -4 -> 'v'
            pr <=-4 && pr > -5 -> 'w'
            pr <=-5 && pr > -6 -> 'x'
            pr <=-6 && pr > -7 -> 'y'
            pr <=-7 -> 'z'
            else -> error("not supported value -> $pr") }

        return prices.map { encode(it) }.joinToString("")
    }

    private fun fuzzyStrings(encodedStr: String) : List<String>{
        val resultLst = mutableListOf<String>()
        fun replaceCharAtIndex(givenStr: String, indexToReplace: Int, chr: Char) = givenStr.substring(0, indexToReplace) + chr + givenStr.substring(
            indexToReplace + 1
        )
        fun similarChars(chr: Char) : List<Char> = when(chr){
            'a' -> listOf('b')
            'b' -> listOf('a', 'c')
            'c' -> listOf('b', 'd')
            'd' -> listOf('c', 'e')
            'e' -> listOf('d', 'f')
            'f' -> listOf('e', 'g')
            'g' -> listOf('f', 'h')
            'h' -> listOf('g', 'i')
            'i' -> listOf('h', 'j')
            'j' -> listOf('i', 'k')
            'k' -> listOf('j', 'p')
            'p' -> listOf('k', 'q')
            'q' -> listOf('p', 'r')
            'r' -> listOf('q', 's')
            's' -> listOf('r', 't')
            't' -> listOf('s', 'u')
            'u' -> listOf('t', 'v')
            'v' -> listOf('u', 'w')
            'w' -> listOf('v', 'x')
            'x' -> listOf('w', 'y')
            'y' -> listOf('x', 'z')
            'z' -> listOf('y')
            else -> error("$chr not supported")
        }


        for (i in encodedStr.indices) {
            similarChars(encodedStr[i]).forEach { resultLst.add(replaceCharAtIndex(encodedStr, i, it)) }
        }

        return resultLst
    }

    fun updateDataFromYahooFinance(){
        if (lastUpdateFromYahooFinance.isEqual(LocalDate.now())) {
            logger.debug("skipping.. updateDataFromYahooFinance was already executed.")
            return
        }
        logger.debug("executing.. updateDataFromYahooFinance.")
        lastUpdateFromYahooFinance = LocalDate.now()
        val stock = YahooFinance.get("GC=F")
        val maxDate = records.maxOf { it.date }.plusDays(1)// one day after the last we've already got
        val lastKnowDay : Calendar = Calendar.getInstance().also { it.set(
            maxDate.year,
            maxDate.monthValue - 1,
            maxDate.dayOfMonth
        ) }
        val now : Calendar = Calendar.getInstance()
        val history = stock.getHistory(lastKnowDay, now, Interval.DAILY).toList().sortedBy { it.date }.dropLast(1)

        for (j in history.indices) {
            val date = LocalDateTime.ofInstant(history[j].date.toInstant(), history[j].date.timeZone.toZoneId()).toLocalDate()
            val newRecord = GoldFuturesRecord(date,history[j].close.toDouble().round(2), history[j].open.toDouble().round(2),
                history[j].high.toDouble().round(2), history[j].low.toDouble().round(2), history[j].volume.toString(), directedPercentageDifference(records.last().price,history[j].close.toDouble()))
            records.add(newRecord)
        }
        /*history.forEach {
            val date = LocalDateTime.ofInstant(it.date.toInstant(), it.date.timeZone.toZoneId()).toLocalDate()
            if (date != LocalDate.now()){ // want to skip current date, because the data is not final
                val newRecord = GoldFuturesRecord(date,it.close.toDouble().round(2), it.open.toDouble().round(2),
                    it.high.toDouble().round(2), it.low.toDouble().round(2), it.volume.toString(), directedPercentageDifference(records.last().price,it.close.toDouble()))
                records.add(newRecord)
            }

        }*/
        records.sortBy { it.date }
    }

}

/**
 * returns percentage delta of SECOND argument, compared to FIRST
 * always positive
 */
fun percentageDifference(first: Double, second: Double): Double = ((abs(second - first) * 100) / first).round(2)

fun directedPercentageDifference(first: Double, second: Double): Double{
    if (first > second) return -1 * percentageDifference(first, second)

    return  percentageDifference(first, second)
}


data class Prediction(
    val matchedPattern: String,
    val predictDay1: Double,
    val predictDay2: Double,
    val predictDay3: Double,
    val type: String = "exact"
)
data class GoldFuturesRecord(
    val date: LocalDate, val price: Double, val open: Double,
    val high: Double, val low: Double, val volume: String, val changePercent: Double, var minMax: String = "none"
)
