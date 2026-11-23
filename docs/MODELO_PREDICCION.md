# Modelo de predicción (Poisson)

Estado: **terminado** el 23/11/2026 (predicción de la próxima jornada + backtesting).

## Idea

Los goles de un equipo en un partido se modelan con una **distribución de Poisson**, que cuenta
sucesos que ocurren de vez en cuando en un tiempo fijo. Solo necesita la media esperada (λ):

```
P(k goles) = e^(-λ) · λ^k / k!
```

Para cada partido se calcula λ de cada equipo con su fuerza de ataque y la defensa del rival,
separando casa y fuera (así se incluye la ventaja de jugar en casa):

```
λ local     = media liga en casa × ataque local en casa × defensa visitante fuera
λ visitante = media liga fuera   × ataque visitante fuera × defensa local en casa
```

- **Ataque** = lo que marca el equipo / lo que marca un equipo medio (1,2 = un 20 % más).
- **Defensa** = lo que recibe el equipo / lo que recibe un equipo medio (0,8 = un 20 % menos).
- **Suavizado**: a cada equipo se le suman 5 partidos de un equipo medio, para que un recién
  ascendido o un equipo con pocos partidos no tenga una fuerza exagerada.

Con las probabilidades de 0 a 10 goles de cada equipo se calculan las de todos los marcadores
(P(i - j) = P(i) · P(j), suponiendo independencia) y, sumándolas, las de 1, X, 2 y "más de 2,5 goles".

Los córners, amarillas, remates y faltas usan la misma fórmula con sus propias medias. Si la fuente
no tiene un dato (por ejemplo, córners antes de 2000), el resultado es **N/D**.

## Datos que usa

- Partidos de la **temporada actual y la anterior** guardados en MySQL.
- **Solo partidos anteriores a la fecha** del partido que se predice (sin fuga de información futura).
  Lo comprueba la prueba `ModeloPoissonTest.noUsaPartidosDelMismoDiaNiPosteriores`.
- Calendario de la próxima jornada: `fixtures.csv` de football-data.co.uk (mismos nombres de equipo).

## Clases

| Clase | Qué hace |
|---|---|
| `importer.CsvCalendarioParser` | Descarga `fixtures.csv` con `HttpClient` y lee los partidos SP1 |
| `model.PartidoPendiente` | Partido sin jugar (fecha, local, visitante) |
| `service.MediasEquipos` | Medias de una estadística por equipo, en casa y fuera, con suavizado |
| `service.ModeloPoisson` | Calcula λ y las probabilidades de cada partido |
| `service.Prediccion` | Resultado de la predicción, con textos para la tabla |
| `service.BacktestingService` | Predice partidos pasados y mide aciertos y Brier score |

## Backtesting (resultados reales, 23/11/2026)

Cada partido de la temporada se predice usando solo los partidos anteriores a su fecha (de esa
temporada y la anterior). Se compara con un **modelo base** que da a todos los partidos el porcentaje
de 1, X y 2 de los partidos anteriores y elige siempre el más frecuente (victoria local).

**Brier score**: media de (probabilidad dada − resultado real)² sumada en 1, X y 2. Cuanto más bajo, mejor
(0 = acertar siempre con un 100 %).

| Temporada | Partidos | Aciertos modelo | Aciertos base | Brier modelo | Brier base | Error goles totales |
|---|---|---|---|---|---|---|
| 2022/23 | 380 | 51,6 % | 47,9 % | 0,6023 | 0,6374 | 1,24 |
| 2023/24 | 380 | 53,4 % | 43,9 % | 0,5787 | 0,6529 | 1,37 |
| 2024/25 | 380 | 51,1 % | 44,5 % | 0,5929 | 0,6488 | 1,24 |
| 2025/26 | 380 | 51,8 % | 48,9 % | 0,5833 | 0,6326 | 1,19 |
| 2026/27 (hasta 08/11) | 119 | 52,1 % | 43,7 % | 0,5782 | 0,6543 | 1,47 |

El modelo mejora al modelo base en **todas** las temporadas, tanto en aciertos como en Brier score.
Acertar algo más de la mitad es lo normal en fútbol: el empate es muy difícil de predecir y el modelo
casi nunca lo elige como más probable.

### Elección del suavizado

Se probó con la temporada 2024/25 (no con la 2025/26, para no ajustar el modelo con la misma temporada
con la que luego se mide):

| Partidos de suavizado | Aciertos | Brier |
|---|---|---|
| 0 | 49,7 % | no se puede calcular (un ascendido sin partidos da 0/0) |
| 3 | 50,3 % | 0,5925 |
| **5** | **51,1 %** | 0,5929 |
| 10 | 50,0 % | 0,5953 |
| 20 | 50,3 % | 0,6009 |

## Limitaciones

- Supone que los goles de los dos equipos son independientes (en la realidad los empates 0-0 y 1-1
  son algo más frecuentes de lo que da el modelo).
- Todos los partidos pesan igual, sean de hace un año o de la semana pasada.
- No conoce lesiones, sanciones, cambios de entrenador ni fichajes.
- Un recién ascendido empieza como un equipo medio de Primera, aunque normalmente es más débil.
- El CSV de resultados de Football-Data se actualiza con unos días de retraso.
