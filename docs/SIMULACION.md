# Simulación de la liga (Monte Carlo)

Estado: **terminado** el 25/11/2026.

## Idea

El método de Monte Carlo consiste en repetir muchas veces un experimento al azar y contar los resultados.
Aquí el experimento es jugar todos los partidos que faltan de la temporada:

1. Se parte de la clasificación real con los partidos ya jugados.
2. Los partidos que faltan son todas las parejas (local, visitante) que todavía no se han jugado
   (cada equipo juega contra los otros 19 en casa y fuera: 380 partidos).
3. Para cada partido que falta se calculan los goles esperados de cada equipo con el modelo de Poisson
   (`docs/MODELO_PREDICCION.md`), usando solo los partidos anteriores a la fecha de la simulación.
4. Se "juega" cada partido sorteando los goles con la distribución de Poisson (método de la ruleta:
   se tira un número entre 0 y 1 y se van sumando P(0), P(1), P(2)... hasta pasarlo).
5. Se ordena la clasificación final (puntos, diferencia de goles, goles a favor).
6. Se repite **10.000 veces** y se cuenta: si un equipo es campeón en 8.000 temporadas, su probabilidad es del 80 %.

Resultados por equipo: puntos, goles y diferencia de goles medios, posición media y probabilidad de ser
campeón, entrar en Champions (4 primeros) o descender (3 últimos).

Se usa una **semilla fija** en el generador de números aleatorios: con los mismos datos sale siempre el mismo
resultado, y así las pruebas se pueden repetir.

## Clases

| Clase | Qué hace |
|---|---|
| `service.SimulacionService` | Simulación completa (pasos 1 a 6) |
| `ui.VistaSimulacion` | Pantalla "Simulación" con la tabla de resultados |

## Validación con temporadas terminadas (resultados reales, 25/11/2026)

Se simula una temporada ya terminada desde distintas fechas, usando solo lo que se sabía ese día,
y se mira qué probabilidad daba al campeón real. 10.000 simulaciones, semilla 2026.

**2025/26** (campeón real: Barcelona, 94 puntos):

| Simulación desde | Partidos jugados | Favorito | Prob. del campeón real | Puntos esperados del campeón |
|---|---|---|---|---|
| 01/10/2025 | 70 | Barcelona | 80,9 % | 85,5 |
| 01/12/2025 | 139 | Barcelona | 64,0 % | 84,3 |
| 01/02/2026 | 214 | Barcelona | 74,2 % | 89,3 |
| 01/04/2026 | 290 | Barcelona | 82,2 % | 91,6 |
| 01/05/2026 | 330 | Barcelona | 99,9 % | 95,4 |

**2024/25** (campeón real: Barcelona, 88 puntos):

| Simulación desde | Partidos jugados | Favorito | Prob. del campeón real |
|---|---|---|---|
| 01/10/2024 | 80 | Real Madrid (63,6 %) | 32,1 % |
| 01/12/2024 | 142 | Real Madrid (65,5 %) | 28,0 % |
| 01/02/2025 | 211 | Real Madrid (86,7 %) | 8,2 % |
| 01/04/2025 | 289 | Barcelona | 69,9 % |
| 01/05/2025 | 330 | Barcelona | 77,4 % |

Conclusiones:
- Cuanto más avanza la liga, más seguras son las probabilidades (quedan menos partidos al azar).
- En 2025/26 el campeón real fue el favorito desde octubre.
- En 2024/25 el favorito fue el Real Madrid hasta febrero; el Barcelona lo pasó en la segunda vuelta,
  algo que no se podía saber con los datos de ese momento. Desde abril la simulación ya lo daba como favorito.
- Una temporada simulada tarda muy poco: 10.000 simulaciones en unos 0,2 segundos.

## Limitaciones

- Las mismas del modelo de Poisson (no conoce lesiones, fichajes ni cambios de entrenador).
- Los goles esperados de cada partido se calculan una vez al principio; dentro de una temporada simulada
  no cambian aunque un equipo vaya muy bien o muy mal.
- Usa la temporada anterior: un equipo que la terminó arriba puede salir más alto de lo que indica su
  clasificación actual (en noviembre de 2026 el Villarreal es 10.º con 15 puntos, pero la simulación lo
  pone 5.º porque quedó 3.º en 2025/26).
- La fuerza en casa y fuera se calcula por separado: si un equipo apenas ha jugado fuera, el modelo lo
  trata fuera de casa como un equipo medio. Lo detectó la prueba `elEquipoMuchoMejorSaleCampeonCasiSiempre`
  con una liga de 4 equipos en la que un equipo había jugado toda la ida en casa.
- El orden de la clasificación usa la diferencia de goles en los empates, no el criterio oficial (INC-07).
