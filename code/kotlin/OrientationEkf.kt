package ru.iamteslic.handtrack.core

import kotlin.math.abs

/**
 * Расширенный фильтр Калмана для ориентации.
 *
 * Вектор состояния ошибки — малый поворот dtheta (3). Кватернион уточняется
 * по схеме q_true = q_est * exp(dtheta). Акселерометр используется как датчик
 * направления силы тяжести. В покое измерение акселерометра считается более
 * надёжным (ZARU).
 */
class OrientationEkf(
    initialBias: Vec3 = Vec3.ZERO,
    private val sigmaAcc: Double = 0.3,
    private val sigmaGyro: Double = 0.005,
    private val sigmaAccRest: Double = 0.02,
    private val accelGate: Double = 0.35,
    private val gyroGate: Double = 0.6,
) {
    var bias: Vec3 = initialBias
        private set

    var q: Quaternion = Quaternion.IDENTITY
        private set

    var lastAccelUpdateSkipped: Boolean = false
        private set

    private var covariance: Mat3 = Mat3.identity() * 1e-3
    private var initialized = false

    fun reset() {
        q = Quaternion.IDENTITY
        covariance = Mat3.identity() * 1e-3
        initialized = false
    }

    fun resetFromAccel(acc: Vec3): Quaternion {
        val a = acc.normalized()
        q = if (a.norm < 1e-9) Quaternion.IDENTITY else Quaternion.fromTwoVectors(a, Vec3.UP)
        covariance = Mat3.identity() * 1e-3
        initialized = true
        return q
    }

    fun updateBias(value: Vec3) {
        bias = value
    }

    fun update(acc: Vec3, gyro: Vec3, dt: Double, rest: Boolean): Quaternion {
        if (!initialized) resetFromAccel(acc)
        if (dt <= 0.0 || !dt.isFinite()) return q

        val omega = gyro - bias
        q = (q * Quaternion.fromRotationVector(omega * dt)).normalized()

        val f = Mat3.identity() - Mat3.skew(omega * dt)
        val processNoise = Mat3.identity() * (sigmaGyro * sigmaGyro * dt)
        covariance = f * covariance * f.transpose() + processNoise

        val accNorm = acc.norm
        if (accNorm < 1e-9) return q

        val deviation = abs(accNorm - GRAVITY)

        // Во время быстрого вращения/линейного ускорения акселерометр
        // измеряет не только гравитацию (центростремительное и тангенциальное
        // ускорение). Доверять ему в этот момент нельзя: ошибка наклона
        // затем проецирует g в горизонтальные оси и уводит положение.
        if (!rest && (deviation > accelGate || omega.norm > gyroGate)) {
            lastAccelUpdateSkipped = true
            return q
        }
        lastAccelUpdateSkipped = false

        val measurementNoise = if (rest) {
            sigmaAccRest * sigmaAccRest
        } else {
            sigmaAcc * sigmaAcc * (1.0 + (deviation / 0.75) * (deviation / 0.75))
        }

        val gravityBody = q.toMatrix().transpose() * Vec3.UP
        val residual = acc / accNorm - gravityBody
        val h = Mat3.skew(gravityBody)
        val s = h * covariance * h.transpose() + Mat3.identity() * measurementNoise
        val k = covariance * h.transpose() * s.inverse()

        val dtheta = k * residual
        q = (q * Quaternion.fromRotationVector(dtheta)).normalized()
        covariance = (Mat3.identity() - k * h) * covariance
        return q
    }

    companion object {
        const val GRAVITY = 9.80665
    }
}
