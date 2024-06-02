package Game

import kotlin.math.abs

val useRocket: (Lander) -> Double = { lander ->
    acceleration(lander.mass, lander.enginePower)
}

val calcNewVelocity: (Lander, Double, Double, Boolean) -> Pair<Double, Double> = { lander, time, gravitationalAcceleration, useEngine ->
    val enginesAcceleration = if (useEngine) useRocket(lander) else 0.0

    val decomposedEnginesAcceleration = decomposeAcceleration(enginesAcceleration, lander.rotation)

    Pair(applyDeltaVelocity(lander.velocity.first, decomposedEnginesAcceleration.first * time),
        applyDeltaVelocity(lander.velocity.second, gravitationalAcceleration * time + decomposedEnginesAcceleration.second * time))
}

val safeLand: (Lander) -> Boolean = {lander ->
    abs(lander.velocity.first) < 2.0f && abs(lander.velocity.second) < 2.0f && abs(lander.rotation) < 5.0f
}