package Game

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.*
import kotlin.math.pow

@Composable
@Preview
fun App(game: Game) {
    Surface() {
        KtLander(game)
    }
}

class Game {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val lunar = Lunar(generateSurface(), 1737000.0, 7.34767309 * 10.0.pow(22))
    private var time = 1
    private var gravitationalAcceleration: Double = 0.0
    var engineTime = 0.0
    private val mutableState = MutableStateFlow(Pair(
        Lander(position = Pair(100.0, 50.0), rotation = 0.0, enginePower = 3500.0, mass = 14900.0, velocity = Pair(0.01, 0.0)),
        Pair(lunar.plane, engineTime))) // Maybe just the nearest points?

    val state: Flow<Pair<Lander, Pair<List<Pair<Double, Double>>, Double>>> = mutableState

    var rotationToApply: Double = 0.0


    init {
        coroutineScope.launch {
            while (true) {
                delay(10)
                if (collision(
                        mutableState.value.second.first.findNearestCoords(mutableState.value.first.position),
                        mutableState.value.first.position)
                ) break;

                gravitationalAcceleration = gravitationalAcceleration(lunar.mass, lunar.radius, mutableState.value.first.position.first)
                val newVelocity = calcNewVelocity(mutableState.value.first, engineTime, gravitationalAcceleration)
                mutableState.update {
                    Pair(
                        it.first.copy(
                            position = Pair(
                                it.first.position.first + (newVelocity.first * time),
                                it.first.position.second + (newVelocity.second * time)
                            ),
                            rotation = it.first.rotation + rotationToApply,
                            velocity = newVelocity),
                        Pair(
                            it.second.first,
                            engineTime
                        ))
                }
            }
        }
    }
}

@Composable
fun KtLander(game: Game) {
    val state = game.state.collectAsState(initial = null)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        state.value?.let {
            Board(it)
        }
    }
}

@Composable
fun Board(state: Pair<Lander, Pair<List<Pair<Double, Double>>, Double>>) {
    Canvas(Modifier.fillMaxSize()) {

        scale(20f, -20f) {
            translate(left = size.width/2 - state.first.position.first.toFloat(), top = size.height/2 - state.first.position.second.toFloat()) {
                rotate(
                    degrees = -state.first.rotation.toFloat(),
                    pivot = Offset(x = state.first.position.first.toFloat() + 0.25f, y = state.first.position.second.toFloat() + 0.35f)
                ) {
                    drawRect(
                        color = Color.Black,
                        size = Size(width = 0.5f, height = 0.7f),
                        topLeft = Offset(
                            x = state.first.position.first.toFloat(),
                            y = state.first.position.second.toFloat()
                        )
                    )
                    if (state.second.second > 0.0) {
                        drawCircle(
                            color = Color.Magenta,
                            radius = 0.2f,
                            center = Offset(
                                x = state.first.position.first.toFloat() + 0.25f,
                                y = state.first.position.second.toFloat() + 0.0f)
                        )
                    }
                }

                state.second.first.zipWithNext { a, b ->
                    drawLine(
                        color = Color.Black,
                        start = Offset(a.first.toFloat(), a.second.toFloat()),
                        end = Offset(b.first.toFloat(), b.second.toFloat()))
                }
            }
        }
    }
}


fun main() = application {
    val game = Game()
    Window(onCloseRequest = ::exitApplication,
        onKeyEvent = {
            when (it.key) {
                Key.A -> {
                    when (it.type) {
                        KeyEventType.KeyDown -> {
                            game.rotationToApply = -1.0
                        }
                        KeyEventType.KeyUp -> {
                            game.rotationToApply = 0.0
                        }
                    }
                    true
                }
                Key.D -> {
                    when (it.type) {
                        KeyEventType.KeyDown -> {
                            game.rotationToApply = 1.0
                        }
                        KeyEventType.KeyUp -> {
                            game.rotationToApply = 0.0
                        }
                    }
                    true
                }
                Key.W -> {
                    when (it.type) {
                        KeyEventType.KeyDown -> {
                            game.engineTime = 0.005
                        }
                        KeyEventType.KeyUp -> {
                            game.engineTime = 0.0
                        }
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }) {
        App(game)
    }
}
