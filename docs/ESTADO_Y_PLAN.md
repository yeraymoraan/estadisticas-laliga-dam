# Estado, decisiones y planificación

Fecha de inicio del repositorio: 16/10/2026. Entrega final: 04/12/2026. Defensa: 10 u 11/12/2026.

## Primer avance (16 de octubre)

- Alcance fijado: solo Primera División española, muchas temporadas históricas.
- Fuente histórica propuesta: CSV Football-Data; los CSV se descargan manualmente.
- Modelo SQL diseñado para temporadas, equipos, participantes, partidos y estadísticas opcionales.
- Desarrollo inicial de lector CSV, repositorio JDBC y vista JavaFX.
- **Sin prometer pruebas en Windows/MySQL** hasta realizarlas y guardar capturas/evidencias.
- Todo el código creado con asistencia IA debe revisarse y poder explicarse.

## Diagnóstico del entorno (13 de noviembre)

- Repositorio clonado en Windows 11; `main` limpia, 7 commits reales del 16/10.
- IDE: se pasa de Eclipse a **IntelliJ IDEA 2026.3** (mismo lenguaje, mismo `pom.xml`).
- JDK 21 instalado. Maven no estaba en el PATH: se añade **Maven Wrapper** (`mvnw.cmd`).
- `mvn clean test`: 3/3 pruebas correctas. La interfaz arranca con `javafx:run` y con la nueva clase `Launcher`.
- Base de datos: no había servidor MySQL; se instala **MySQL Community Server 26.10**. Esquema y permisos verificados;
  importación real de 5 temporadas (1.639 partidos) sin duplicados al reimportar.
- Parser probado con CSV reales: falla con 1995/96 (INC-02). Detalle en `docs/INCIDENCIAS.md`.
- Temporada actual 2026/27: 119 partidos jugados a 08/11/2026. Los partidos pendientes se pueden deducir
  (cada pareja juega una vez en cada campo: 20 × 19 = 380), sin depender de una API de calendario.

## Plan revisado (13 nov – 4 dic)

| Semana | Objetivo | Resultado verificable |
|---|---|---|
| 13–19 nov | MySQL funcionando, corregir INC-02/INC-03, importar 1993/94–2026/27, CRUD de equipos y partidos (RF-01/02), filtros (RF-03), estadísticas de equipo con tamaño de muestra (RF-04), JDBC fuera del hilo de la interfaz | Conteos por temporada; pruebas JUnit; capturas |
| 20–26 nov | Modelo de Poisson (fuerza ataque/defensa + ventaja local), 1-X-2, goles, córners y tarjetas; backtesting sin fuga de datos; simulación Monte Carlo de 2026/27 (RF-05/06); desempates oficiales | Métricas de backtesting frente a una referencia simple; pruebas |
| 27 nov–4 dic | Interfaz final (navegación, CSS, gráficos), ejecutable Windows, memoria con la plantilla, manuales, evidencias | Entrega el 4/12 |
| 5–10 dic | Presentación, guion de demostración y ensayo de preguntas | Defensa |

Los hitos originales de octubre se conservan abajo como referencia histórica.

## Próximos hitos (planificación del 16 de octubre)

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
