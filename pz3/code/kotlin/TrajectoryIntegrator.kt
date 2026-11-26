package ru.iamteslic.handtrack.core

/** Результат одного шага интегрирования. */
data class IntegrationStep(val velocity: Vec3, val position: Vec3)

/**
 * Интегрирование ускорения методом трапеций с обнулением скорости в покое.
 */
class TrajectoryIntegrator {
    var velocity: Vec3 = Vec3.ZERO
        private set
    var position: Vec3 = Vec3.ZERO
        private set

    private var previousAcceleration: Vec3? = null

    fun reset() {
        velocity = Vec3.ZERO
        position = Vec3.ZERO
        previousAcceleration = null
    }

    fun step(acceleration: Vec3, dt: Double, rest: Boolean): IntegrationStep {
        if (dt <= 0.0 || !dt.isFinite()) return IntegrationStep(velocity, position)

        if (rest) {
            velocity = Vec3.ZERO
            previousAcceleration = acceleration
            return IntegrationStep(velocity, position)
        }

        val previous = previousAcceleration ?: acceleration
        val newVelocity = velocity + (previous + acceleration) * (0.5 * dt)
        val newPosition = position + (velocity + newVelocity) * (0.5 * dt)

        velocity = newVelocity
        position = newPosition
        previousAcceleration = acceleration
        return IntegrationStep(velocity, position)
    }
}
