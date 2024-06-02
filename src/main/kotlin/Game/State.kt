package Game

data class State(
    val lander: Lander,
    val plane: List<Pair<Double, Double>>,
    val engineInUse: Boolean,
    val landerStatus: LanderStatus
)