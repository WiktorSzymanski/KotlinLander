package Game

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.pow

enum class ViewState {
    INTRO,
    GAME
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    var viewState by remember { mutableStateOf(ViewState.INTRO) }

    when (viewState) {
        ViewState.INTRO -> IntroView(onStartGame = { viewState = ViewState.GAME })
        ViewState.GAME -> GameView(onBackToIntro = { viewState = ViewState.INTRO })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IntroView(onStartGame: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Press SPACE to Start",
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxSize().focusRequester(focusRequester).focusable().onKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Spacebar) {
                    onStartGame()
                    true
                } else {
                    false
                }
            }
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun endText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Safely Landed!",
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun GameView(onBackToIntro: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val game = Game(onBackToIntro)
    Surface(modifier = Modifier.focusRequester(focusRequester).focusable().onKeyEvent {
        when (it.key) {
            Key.A -> {
                when (it.type) {
                    KeyEventType.KeyDown -> {
                        game.rotationToApply = -35.0
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
                        game.rotationToApply = 35.0
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
                        game.engineTime = 1.0
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
        KtLander(game)
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

class Game(onBackToIntro: () -> Unit) {
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

    var frameCount = MutableStateFlow(0)
    var fps = MutableStateFlow(30)
    var lastTime = MutableStateFlow(System.currentTimeMillis())

    init {
        coroutineScope.launch {

            while (true) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - lastTime.value

                if (elapsedTime >= 1000) {
                    fps.value = frameCount.value * 1000 / max(elapsedTime.toInt(), 1)
                    frameCount.value = 0
                    lastTime.value = currentTime
                }
                delay(1000)
            }
        }
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
                val newVelocity = calcNewVelocity(mutableState.value.first, (engineTime / fps.value), gravitationalAcceleration)
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
                            rotation = (it.first.rotation + (rotationToApply / fps.value)) % 360.0,
                            velocity = newVelocity),
                        Pair(
                            it.second.first,
                            engineTime
                        ))
                }
            }
            delay(2000)
            onBackToIntro()
        }
    }
}

@Composable
fun KtLander(game: Game) {
    val state = game.state.collectAsState(initial = null)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        state.value?.let {
            Board(it) { game.frameCount.value++ }
        }
    }
}

@Preview
@Composable
fun Board(state: Pair<Lander, Pair<List<Pair<Double, Double>>, Double>>, onFrame: () -> Unit) {
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
        onFrame()
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
                val extendedPoints = mutableListOf<Pair<Double, Double>>()
                val points = state.second.first

                extendedPoints.addAll(points.takeLast(80).map {pair -> Pair(pair.first - points.last().first, pair.second) })
                extendedPoints.addAll(points)
                extendedPoints.addAll(points.take(80).map {pair -> Pair(pair.first + points.last().first, pair.second) })

                extendedPoints.zipWithNext { a, b ->
                    drawLine(
                        color = Color.Gray,
                        start = Offset(a.first.toFloat(), a.second.toFloat() - 0.3f),
                        end = Offset(b.first.toFloat(), b.second.toFloat() - 0.3f),
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
    Window(onCloseRequest = ::exitApplication,
    ) {
        App()
    }
}
