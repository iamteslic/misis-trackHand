package ru.iamteslic.handtrack.core

/**
 * Детектор остановок по скользящему окну.
 *
 * В покое модуль ускорения почти постоянен, а модуль угловой скорости близок
 * к нулю. Пороги заданы в м/с^2 и рад/с — как в НИР-7.
 */
class ZuptDetector(
    private val window: Int = 25,
    private val accThreshold: Double = 0.12,
    private val gyroThreshold: Double = 0.10,
) {
    private val accNorms = ArrayDeque<Double>()
    private val gyroNorms = ArrayDeque<Double>()

    fun add(acc: Vec3, gyro: Vec3): Boolean {
        accNorms.addLast(acc.norm)
        gyroNorms.addLast(gyro.norm)
        while (accNorms.size > window) accNorms.removeFirst()
        while (gyroNorms.size > window) gyroNorms.removeFirst()

        val meanAcc = accNorms.average()
        var variance = 0.0
        for (value in accNorms) {
            val d = value - meanAcc
            variance += d * d
        }
        variance /= accNorms.size
        val meanGyro = gyroNorms.average()

        return variance < accThreshold * accThreshold && meanGyro < gyroThreshold
    }

    fun reset() {
        accNorms.clear()
        gyroNorms.clear()
    }
}
