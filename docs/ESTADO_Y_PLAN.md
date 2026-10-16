# Estado, decisiones y planificación

Fecha de inicio del repositorio: 16/10/2026. Entrega final: 04/12/2026. Defensa: 10 u 11/12/2026.

## Primer avance (16 de octubre)

- Alcance fijado: solo Primera División española, muchas temporadas históricas.
- Fuente histórica propuesta: CSV Football-Data; los CSV se descargan manualmente.
- Modelo SQL diseñado para temporadas, equipos, participantes, partidos y estadísticas opcionales.
- Desarrollo inicial de lector CSV, repositorio JDBC y vista JavaFX.
- **Sin prometer pruebas en Windows/MySQL** hasta realizarlas y guardar capturas/evidencias.
- Todo el código creado con asistencia IA debe revisarse y poder explicarse.

## Próximos hitos

1. 16–25 oct: configurar Eclipse, MySQL, importar temporadas históricas, comprobar conteos por temporada (380 partidos completos en una temporada estándar de 20 clubes).
2. 26 oct–8 nov: filtros de partidos, clasificación, vistas de equipos, cálculos sobre remates/córners/tarjetas, pruebas y capturas.
3. 9–20 nov: modelos estadísticos por partido (1X2, goles, córners, tarjetas), pruebas retrospectivas para evitar fuga de datos.
4. 21–27 nov: simulación Monte Carlo de clasificación final, integración del calendario futuro con API REST si existe acceso adecuado.
5. 28 nov–4 dic: instalador/ejecutable, prueba con otra máquina, memoria, manual y preparación de defensa.

## Criterios para pasar a siguiente fase

- Datos verificables y sin duplicados al reimportar.
- Valores nulos no convertidos en cero.
- Validaciones de datos y JDBC con sentencias preparadas.
- Tests automáticos y pruebas manuales documentadas.
- Historial Git con ramas y PR, sin inventar fechas de desarrollo.

## Riesgos

- Cambios en formatos CSV de temporadas antiguas; validar por temporada antes de importar en bloque.
- Equipos con nombres históricos o variantes requieren normalización controlada.
- La API de calendarios puede tener límites de uso.
- Los pronósticos son estimaciones, no garantías; separar datos de entrenamiento y evaluación.
- Si falta tiempo, preferir una predicción estadística simple y bien probada a una IA opaca.

## Para consultar al tutor

La propuesta inicial contemplaba introducción manual y excluía predicciones. Confirmar por escrito la **ampliación** (histórico de LaLiga, importador, predicciones y simulación final), manteniendo Java/JavaFX/MySQL.
