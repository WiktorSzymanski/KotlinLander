package Game

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
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
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
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
    private val maxX = lunar.plane.last().first
    private val mutableState = MutableStateFlow(Pair(
        Lander(position = Pair(100.0, 100.0), rotation = 0.0, enginePower = 3500.0, mass = 14900.0, velocity = Pair(0.01, 0.0)),
        Pair(lunar.plane, engineTime))) // Maybe just the nearest points?

    val state: Flow<Pair<Lander, Pair<List<Pair<Double, Double>>, Double>>> = mutableState

    var rotationToApply: Double = 0.0


    init {
        coroutineScope.launch {
            while (true) {
                delay(20)
                if (collision(
                        mutableState.value.second.first.findNearestCoords(mutableState.value.first.position),
                        mutableState.value.first.position)
                ) {
                    println(mutableState.value.first.velocity)
                    println(if (safeLand(mutableState.value.first)) "Safely landed!" else "Crashed!")
                    break
                };

//                gravitationalAcceleration = gravitationalAcceleration(lunar.mass, lunar.radius, mutableState.value.first.position.second)
                gravitationalAcceleration = -0.00167
                println(gravitationalAcceleration)
                val newVelocity = calcNewVelocity(mutableState.value.first, engineTime, gravitationalAcceleration)
                var newXPosition = (mutableState.value.first.position.first + (newVelocity.first * time)) % maxX
                if (newXPosition < 0) {
                    newXPosition += maxX
                }
                mutableState.update {
                    Pair(
                        it.first.copy(
                            position = Pair(
                                newXPosition,
                                it.first.position.second + (newVelocity.second * time)
                            ),
                            rotation = (it.first.rotation + rotationToApply) % 360.0,
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

@Preview
@Composable
fun Board(state: Pair<Lander, Pair<List<Pair<Double, Double>>, Double>>) {
    val textMeasurer = rememberTextMeasurer()
    val textVelocityX = "vx: %.2f".format(state.first.velocity.first)
    val textVelocityY = "vx: %.2f".format(state.first.velocity.second)

    val style = TextStyle(
        fontSize = 16.sp,
        color = Color.White
    )

    val textLayoutVX = remember(textVelocityX, style) {
        textMeasurer.measure(textVelocityX, style)
    }

    val textLayoutVY = remember(textVelocityY, style) {
        textMeasurer.measure(textVelocityY, style)
    }

    Canvas(Modifier.fillMaxSize().background(Color.hsl(237F, 1F, 0.08F))) {
        val landerOffset = Offset(
            x = state.first.position.first.toFloat(),
            y = state.first.position.second.toFloat())

        drawText(
            topLeft = Offset(size.width - 100f, 10f),
            textLayoutResult = textLayoutVX
        )

        drawText(
            topLeft = Offset(size.width - 100f, 30f),
            textLayoutResult = textLayoutVY
        )

        scale(20f, -20f) {
            translate(left = size.width/2 - state.first.position.first.toFloat(), top = size.height/2 - state.first.position.second.toFloat()) {
                state.second.first.zipWithNext { a, b ->
                    drawLine(
                        color = Color.Gray,
                        start = Offset(a.first.toFloat(), a.second.toFloat()),
                        end = Offset(b.first.toFloat(), b.second.toFloat()),
                        strokeWidth = 0.5f,
                        cap = StrokeCap.Round)
                }

                rotate(
                    degrees = -state.first.rotation.toFloat(),
                    pivot = Offset(x = state.first.position.first.toFloat(), y = state.first.position.second.toFloat() + 0.3f)
                ) {
                    drawCircle(
                        color = Color.White,
                        radius = 0.3f,
                        style = Stroke(width = 0.1f),
                        center = Offset(
                            x = state.first.position.first.toFloat(),
                            y = state.first.position.second.toFloat() + 0.3f)
                    )
                    drawRect(
                        color = Color.White,
                        size = Size(width = 0.6f, height = 0.2f),
                        style = Stroke(width = 0.1f),
                        topLeft = Offset(
                            x = state.first.position.first.toFloat() - 0.3f,
                            y = state.first.position.second.toFloat()
                        )
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(
                            x = state.first.position.first.toFloat() - 0.25f,
                            y = state.first.position.second.toFloat() + 0.2f
                        ),
                        end = Offset(
                            x = state.first.position.first.toFloat() - 0.4f,
                            y = state.first.position.second.toFloat() - 0.3f
                        )
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(
                            x = state.first.position.first.toFloat() + 0.25f,
                            y = state.first.position.second.toFloat() + 0.2f
                        ),
                        end = Offset(
                            x = state.first.position.first.toFloat() + 0.4f,
                            y = state.first.position.second.toFloat() - 0.3f
                        )
                    )
                    if (state.second.second > 0.0) {
                        drawCircle(
                            radius = 0.3f,
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Cyan, Color.Transparent),
                                end = landerOffset - Offset(0f, 0.8f),
                                start = landerOffset - Offset(0f, 0f),
                                tileMode = TileMode.Decal
                            ),
                            center = landerOffset - Offset(0f, 0.5f)
                        )
                        drawCircle(
                            radius = 0.15f,
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Cyan, Color.Transparent),
                                end = landerOffset - Offset(0f, 1f),
                                start = landerOffset - Offset(0f, 0.2f),
                                tileMode = TileMode.Decal
                            ),
                            center = landerOffset - Offset(0f, 0.7f)
                        )
                    }
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
                            game.engineTime = 0.05
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
