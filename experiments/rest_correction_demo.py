"""
Демонстрация коррекции постоянной ошибки ускорения на участке «покой–покой».

Сценарий: кисть проходит отрезок и останавливается. Истинное движение
начинается и заканчивается в покое, поэтому интеграл истинного ускорения
по участку равен нулю. Акселерометр добавляет постоянное смещение b.
Без коррекции интеграл равен b*T: скорость в конце не нулевая, а положение
уезжает примерно на b*T^2/2. Коррекция оценивает b по остатку интеграла
и вычитает его, после чего конечная скорость возвращается к нулю.

Запуск:  python3 rest_correction_demo.py
"""

import math


def true_position(t, duration, amplitude=0.4):
    """Плавное движение из покоя в покой: sin^2(pi*t/T)."""
    s = math.sin(math.pi * t / duration)
    return amplitude * s * s


def true_acceleration(t, duration, amplitude=0.4):
    w = math.pi / duration
    return 2.0 * amplitude * w * w * math.cos(2.0 * math.pi * t / duration)


def integrate(acc, dt, bias):
    """Дважды интегрирует ускорение методом трапеций, начиная из покоя."""
    n = len(acc)
    v = [0.0] * n
    p = [0.0] * n
    a = [x + bias for x in acc]
    for k in range(1, n):
        v[k] = v[k - 1] + 0.5 * dt * (a[k] + a[k - 1])
        p[k] = p[k - 1] + 0.5 * dt * (v[k] + v[k - 1])
    return a, v, p


def main():
    duration = 6.0
    dt = 0.01
    bias = 0.04                      # м/с^2, постоянная ошибка акселерометра
    n = int(round(duration / dt)) + 1
    t = [k * dt for k in range(n)]

    acc_true = [true_acceleration(x, duration) for x in t]
    p_true = [true_position(x, duration) for x in t]

    acc, v, p = integrate(acc_true, dt, bias)
    drift_before = abs(p[-1] - p_true[-1])

    # Коррекция: интеграл ускорения по участку должен быть нулевым.
    delta_v = 0.0
    for k in range(1, n):
        delta_v += 0.5 * dt * (acc[k] + acc[k - 1])
    correction = delta_v / (duration - 0.5 * dt)

    acc_corr = [x - correction for x in acc]
    _, v_corr, p_corr = integrate(acc_corr, dt, 0.0)
    drift_after = abs(p_corr[-1] - p_true[-1])

    def num(x, nd=4):
        return f"{x:.{nd}f}".replace(".", ",")

    print(f"Истинное смещение за участок:      {num(p_true[-1], 3)} м")
    print(f"Оценка постоянной ошибки:          {num(correction)} м/с^2 (истина {num(bias)})")
    print(f"Скорость в конце без коррекции:    {num(v[-1])} м/с")
    print(f"Ошибка положения без коррекции:    {num(drift_before)} м")
    print(f"Ошибка положения после коррекции:  {num(drift_after)} м")


if __name__ == "__main__":
    main()
