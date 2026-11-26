# ПЗ-3. Статья в LaTeX

Репозиторий содержит статью и её код.

Репозиторий подготовлен на основе шаблона `sadekovlar/misis-article-latex`.

Статья: **«Восстановление ориентации и траектории кисти по данным акселерометра и гироскопа смартфона с коррекцией дрейфа по остановкам»**.

## Состав

| Файл | Назначение |
|---|---|
| `article.tex` | текст статьи: введение, анализ работ, постановка, метод, эксперименты, заключение, 16 источников |
| `article.pdf` | собранный PDF, 6 страниц |
| `misisarticle.cls` | класс сборника НИТУ «МИСиС» |
| `fonts/` | Liberation Serif и Mono для кириллицы |
| `figures/` | схема обработки и траектория до/после коррекции |
| `code/kotlin/` | классы приложения, реализующие метод |
| `experiments/rest_correction_demo.py` | минимальный пример коррекции по остановкам |
| `Makefile` | локальная сборка |

## Сборка

```bash
make article
# PDF: build/article.pdf
```

Вручную:

```bash
mkdir -p build
xelatex -output-directory=build article.tex
xelatex -output-directory=build article.tex
```

Нужен **XeLaTeX**. Для минимального TinyTeX доустановите `tex-gyre`:

```bash
tlmgr install tex-gyre
```

## GitHub Actions

Workflow лежит в корне репозитория: `.github/workflows/build-article.yml`. Он собирает `article.tex` и выкладывает `article.pdf` в артефакт `article-pdf`.

## Код

`code/kotlin/` — четыре класса из приложения:

| Файл | Роль |
|---|---|
| `OrientationEkf.kt` | кватернионный EKF и гейт акселерометра |
| `ZuptDetector.kt` | скользящая дисперсия и порог по угловой скорости |
| `TrajectoryIntegrator.kt` | интегрирование трапециями и обнуление скорости |
| `RestSegmentCorrection.kt` | оценка и вычитание постоянной ошибки между остановками |

`experiments/rest_correction_demo.py` не зависит от Android. Запуск:

```bash
python3 experiments/rest_correction_demo.py
```

Пример показывает, как постоянная ошибка ускорения 0,04 м/с² даёт 0,72 м ухода, а коррекция возвращает ошибку к уровню 10⁻⁴ м.

## Числа

- Ошибка ориентации: 1,48° в модели, 2,34° в приложении.
- RMSE положения: 0,59 м, после коррекции — 0,27 м.
- UCI HAR: 97,2 % отсчётов покоя, ложных остановок при ходьбе нет.
- Собственные записи: 1,423 → 0,071 м и 0,188 → 0,037 м.

## Оформление

- 6 страниц, A4, две колонки, 10 pt.
- 6 разделов, 16 источников, 3 таблицы, 2 рисунка, 11 формул.
- Поля, шрифты и подписи — по классу `misisarticle.cls`.

## Лицензия

`misisarticle.cls` распространяется по LPPL 1.3c. Шрифты Liberation — SIL Open Font License. Текст статьи — авторский.
