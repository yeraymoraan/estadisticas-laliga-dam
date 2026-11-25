# LaLiga Stats — Proyecto DAM

Aplicación de escritorio en **Java 21 + JavaFX + MySQL** para consultar varias temporadas reales de la Primera División española. Proyecto intermodular del CFGS Desarrollo de Aplicaciones Multiplataforma.

## Alcance acordado (16/10/2026)

- Historial de temporadas, equipos y partidos reales (resultados y estadísticas **solo cuando existan en la fuente**).
- Clasificación y búsquedas por temporada, equipo y fecha.
- Más adelante: predicciones de 1X2, goles, córners y tarjetas por partido futuro, y simulación de la clasificación final.
- Fuente histórica inicial: archivos CSV de https://www.football-data.co.uk/spainm.php (descarga manual, condiciones de uso de la fuente).
- Todas las estadísticas importadas se guardan en MySQL y la primera tabla ofrece goles, remates, remates a puerta, córners, tarjetas y faltas si están disponibles.

**Importante:** los resultados futuros nunca se tratan como si fueran hechos; las predicciones se identificarán como estimaciones y tendrán pruebas retrospectivas. No se incluyen claves ni credenciales reales.

## Primeros pasos (Windows)

1. Instalar **JDK 21**, **MySQL Community Server** (probado con 26.10) e **IntelliJ IDEA** (no hace falta instalar Maven: el proyecto incluye Maven Wrapper).
2. Ejecutar el script `sql/schema.sql` en MySQL.
3. Configurar variables de entorno `LALIGA_DB_URL`, `LALIGA_DB_USER`, `LALIGA_DB_PASSWORD` (ver `.env.example`; **no** subir contraseñas).
4. En IntelliJ: configuración **LaLiga App** (botón Run/Debug) o **Tests (mvn clean test)**. Desde terminal: `.\mvnw.cmd clean test` y `.\mvnw.cmd javafx:run`.
5. Abrir la aplicación. **No hay que importar nada**: al arrancar descarga de football-data.co.uk las temporadas que falten (la primera vez, todo el histórico desde 1993/94) y los partidos nuevos de la temporada en curso. El botón **Actualizar datos** vuelve a buscarlos. Sin internet funciona con lo ya guardado.
6. Seleccionar la temporada para consultar partidos y clasificación, o abrir **Próxima jornada** para ver la predicción.
7. Opcional: **Importar CSV** permite cargar un archivo propio indicando el año de inicio de la temporada (ej. 2025 para 2025/26).

## Estado actual

- **Terminado:** histórico 1993/94–2026/27 en MySQL, actualización automática, consulta de partidos y clasificación, predicción de la próxima jornada con backtesting (`docs/MODELO_PREDICCION.md`).
- **En desarrollo:** simulación Monte Carlo del final de la liga.
- **Pendiente:** goleadores y asistentes, diseño de la interfaz, instalador, memoria y manuales.

Probado en Windows 11 con JDK 21 y MySQL 26.10. Consulta `docs/ESTADO_Y_PLAN.md` y `docs/INCIDENCIAS.md`.

## Arquitectura

- `model`: datos del dominio.
- `repository`: JDBC y SQL parametrizado.
- `importer`: lectura/validación de archivos históricos CSV.
- `ui`: ventana JavaFX.
- `sql`: esquema normalizado con estadísticas opcionales.
- `docs`: decisiones, pruebas y evidencias.

El desarrollo se registra en Git con ramas y commits descriptivos. Entrega final: **4 de diciembre de 2026**; defensa: **10 u 11 de diciembre**.

## Ética académica

El código es una base de trabajo asistida por IA y debe revisarse, ejecutarse y comprenderse antes de la entrega y la defensa. Documentar las herramientas utilizadas siguiendo las indicaciones del centro.
