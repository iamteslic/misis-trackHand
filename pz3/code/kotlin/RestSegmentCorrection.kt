package ru.iamteslic.handtrack.core

import kotlin.math.max

/** Один участок движения между остановками, для которого убрана постоянная ошибка. */
data class RestSegmentCorrection(
    val startIndex: Int,
    val endIndex: Int,
    val durationS: Double,
    val correction: Vec3,
    val positionShift: Vec3,
)

data class RestCorrectionResult(
    val positions: List<Vec3>,
    val velocities: List<Vec3>,
    val segments: List<RestSegmentCorrection>,
) {
    val maxCorrection: Double
        get() = segments.maxOfOrNull { it.correction.norm } ?: 0.0

    val maxPositionShift: Double
        get() = segments.maxOfOrNull { it.positionShift.norm } ?: 0.0
}

/**
 * Убирает постоянную ошибку ускорения на участках между остановками.
 *
 * Физическое ограничение: если движение начинается и заканчивается в покое,
 * интеграл линейного ускорения по участку обязан быть нулевым. Любой остаток —
 * это смещение акселерометра, ошибка масштаба или утечка гравитации из-за
 * неточной ориентации. Вычитание постоянной составляющей не меняет форму
 * реального движения, но убирает ложный дрейф, который при двойном
 * интегрировании превращается в уезжающую линию.
 *
 * Коррекция применяется только к участкам, ограниченным двумя достаточно
 * длинными остановками. Ведущие/завершающие участки без опоры не трогаются.
 */
object RestSegmentCorrector {

    fun correct(
        timesS: List<Double>,
        accelerations: List<Vec3>,
        restFlags: List<Boolean>,
        positions: List<Vec3>,
        velocities: List<Vec3>,
        minRestSamples: Int = 20,
    ): RestCorrectionResult {
        val n = minOf(timesS.size, accelerations.size, restFlags.size, positions.size, velocities.size)
        if (n < 3) return RestCorrectionResult(positions, velocities, emptyList())

        val correctedPositions = positions.toMutableList()
        val correctedVelocities = velocities.toMutableList()

        // Непрерывные остановки достаточной длины — якоря с нулевой скоростью.
        val anchors = ArrayList<IntRange>()
        var index = 0
        while (index < n) {
            if (restFlags[index]) {
                val start = index
                while (index < n && restFlags[index]) index++
                if (index - start >= max(2, minRestSamples)) anchors.add(start until index)
            } else {
                index++
            }
        }
        if (anchors.size < 2) return RestCorrectionResult(positions, velocities, emptyList())

        val segments = ArrayList<RestSegmentCorrection>()

        for (anchorIndex in 0 until anchors.lastIndex) {
            val firstRest = anchors[anchorIndex]
            val secondRest = anchors[anchorIndex + 1]
            val start = firstRest.last + 1
            val end = secondRest.first - 1
            if (end - start < 2) continue

            val previous = start - 1
            val duration = timesS[end] - timesS[previous]
            val firstStep = timesS[start] - timesS[previous]
            val weight = duration - 0.5 * firstStep
            if (duration <= 0.0 || weight <= 1e-9) continue

            // Приращение скорости по исходному ускорению (трапеции).
            var deltaV = Vec3.ZERO
            for (i in start..end) {
                val dt = timesS[i] - timesS[i - 1]
                deltaV += (accelerations[i] + accelerations[i - 1]) * (0.5 * dt)
            }
            val correction = deltaV / weight
            if (!correction.isFinite()) continue

            // Переинтегрируем участок с вычтенной постоянной составляющей.
            var position = correctedPositions[previous]
            var velocity = Vec3.ZERO
            correctedVelocities[previous] = Vec3.ZERO

            for (i in start..end) {
                val dt = timesS[i] - timesS[i - 1]
                val previousAcceleration = if (i == start) {
                    accelerations[i - 1]
                } else {
                    accelerations[i - 1] - correction
                }
                val acceleration = accelerations[i] - correction
                val nextVelocity = velocity + (previousAcceleration + acceleration) * (0.5 * dt)
                val nextPosition = position + (velocity + nextVelocity) * (0.5 * dt)

                velocity = nextVelocity
                position = nextPosition
                correctedVelocities[i] = velocity
                correctedPositions[i] = position
            }

            // На остановке скорость строго нулевая, положение не меняется.
            correctedVelocities[end] = Vec3.ZERO
            for (i in secondRest) {
                correctedVelocities[i] = Vec3.ZERO
                correctedPositions[i] = position
            }

            val shift = position - positions[end]
            segments.add(
                RestSegmentCorrection(
                    startIndex = start,
                    endIndex = end,
                    durationS = duration,
                    correction = correction,
                    positionShift = shift,
                ),
            )
        }

        // На ведущей остановке скорость тоже должна быть нулевой.
        for (i in anchors.first()) {
            correctedVelocities[i] = Vec3.ZERO
        }

        return RestCorrectionResult(correctedPositions, correctedVelocities, segments)
    }
}
