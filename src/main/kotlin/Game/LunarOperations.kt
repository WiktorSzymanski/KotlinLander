package Game

import java.io.File
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

val generateSurface: () -> List<Pair<Double, Double>> = {
    val cords = mutableListOf<Pair<Double, Double>>()
    val gen = PerlinNoiseGenerator(42, 100)
    var i = 0.0
    while (i < 100.0) {
        val num = gen.perlin(i, .5,16)
        cords.add(Pair(i*10, num*10))
        i += ((1..2).random()).toDouble()/10
        cords.add(Pair(i*10, num*10))
        i += 0.1
    }
    cords
}

val printSurface: (Map<Double, Double>) -> Unit = { coords ->
    File("data/surface.dat").writeText("")
    for (x in coords) {
        File("data/surface.dat").appendText("${x.key}\t${x.value}\n")
    }
}

val collision: (List<Pair<Double, Double>>, Pair<Double, Double>) -> Boolean = {
    line, point ->
    val lineDistance = sqrt((line[0].first - line[1].first).pow(2.0) + (line[0].second - line[1].second).pow(2.0))
    val pointFirstDistance = sqrt((line[0].first - point.first).pow(2.0) + (line[0].second - point.second).pow(2.0))
    val pointSecondDistance = sqrt((line[1].first - point.first).pow(2.0) + (line[1].second - point.second).pow(2.0))

    abs(pointFirstDistance + pointSecondDistance - lineDistance) < 0.01 || ( line[0].first > point.first && line[1].first > point.first )
}

fun List<Pair<Double, Double>>.findNearestCoords(point: Pair<Double, Double>): List<Pair<Double, Double>> {
    var left = 0
    var right = this.size - 1
    while (left <= right) {
        val mid = left + (right - left) / 2
        if (this[mid].first < point.first) left = mid + 1
        else right = mid - 1
    }

    return listOf(this[left], this[right])
}