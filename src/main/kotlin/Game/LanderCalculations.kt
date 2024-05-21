package Game

val useRocket: (Lander, Double) -> Pair<Double, Double> = { lander, time ->
    val acceleration = acceleration(lander.mass, lander.enginePower)
    val deltaVelocity = accelerationToDeltaVelocity(acceleration, time)

    decomposeAcceleration(deltaVelocity, lander.rotation) /* change name of this function */
}

val calcNewVelocity: (Lander, Double, Double) -> Pair<Double, Double> = { lander, time, gravitationalAcceleration ->
    val enginesDeltaVelocity = useRocket(lander, time)

    Pair(applyDeltaVelocity(lander.velocity.first, enginesDeltaVelocity.second),
        applyDeltaVelocity(lander.velocity.second, gravitationalAcceleration + enginesDeltaVelocity.first))
}