package Game

import kotlin.math.pow
import kotlin.random.Random

class PerlinNoiseGenerator(seed: Int, private val boundary: Int = 10) {
    private var random = Random(seed)
    private val noise = DoubleArray(boundary) {
        random.nextDouble()
    }

    fun perlin(x: Double, persistence: Double = 0.5, numberOfOctaves: Int = 8): Double {
        var total = 0.0
        var amplitudeSum = 0.0
        for (i in 0 until numberOfOctaves) {
            val amplitude = persistence.pow(i)
            val frequency = 2.0.pow(i)
            val octave = amplitude * noise(x * frequency)
            total += octave
            amplitudeSum += amplitude
        }
        return total / amplitudeSum
    }

    private fun noise(t: Double): Double {
        val x = t.toInt()
        val x0 = x % boundary
        val x1 = if (x0 == boundary - 1) 0 else x0 + 1
        val between = t - x

        val y0 = noise[x0]
        val y1 = noise[x1]
        return lerp(y0, y1, between)
    }

    private fun lerp(a: Double, b: Double, alpha: Double): Double {
        return a + alpha * (b - a)
    }
}
