package Game

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.pow

val GRAVITATIONAL_CONSTANT: Double = 6.67430 * 10.0.pow(-11)

val acceleration: (Double, Double) -> Double = { mass, force -> force / mass }

val toRadians: (Double) -> Double = { deg -> deg / 180.0 * PI }

val decomposeAcceleration: (Double, Double) -> Pair<Double, Double> =
    { acceleration, rotation ->
        Pair(acceleration * cos(toRadians(rotation)), acceleration * sin(toRadians(rotation))) }

val accelerationToDeltaVelocity: (Double, Double) -> Double =
    { acceleration, time -> acceleration * time}

val gravitationalAcceleration: (Double, Double, Double) -> Double = /* Nie powinno też być zależne od czasu? */
    { mass, radius, height -> - GRAVITATIONAL_CONSTANT * mass / (radius * height).pow(2) }

val applyDeltaVelocity: (Double, Double) -> Double =
    { deltaVelocity, velocity -> deltaVelocity + velocity }

val calculatePosition: (Int, Double, Double) -> Double =
    { singleCoordinate, velocity, time -> singleCoordinate + (velocity * time) }