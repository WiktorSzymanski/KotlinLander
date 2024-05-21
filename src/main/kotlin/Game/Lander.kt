package Game

data class Lander(
    val position: Pair<Double, Double>,
    val rotation: Double,
    val enginePower: Double,
    val mass: Double,
    val velocity: Pair<Double, Double>)