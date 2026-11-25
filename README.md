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

1. Instalar **JDK 21** y **MySQL Community Server** (probado con 26.10). No hace falta instalar Maven: el proyecto
   incluye Maven Wrapper (`mvnw.cmd`). Se puede abrir con IntelliJ IDEA, Eclipse o NetBeans como proyecto Maven.
2. Crear la base de datos ejecutando `sql/schema.sql` en MySQL (por ejemplo con MySQL Workbench y el usuario `root`).
3. Crear un usuario solo para la aplicación (cambiar `TU_CLAVE` por una contraseña inventada):
   ```sql
   CREATE USER 'laliga_app'@'localhost' IDENTIFIED BY 'TU_CLAVE';
   GRANT SELECT, INSERT, UPDATE, DELETE ON laliga_dam.* TO 'laliga_app'@'localhost';
   ```
4. Configurar la conexión de una de estas dos formas (**no** subir contraseñas a GitHub):
   - Copiar `laliga.properties.example` como `laliga.properties` en la carpeta del proyecto y poner la contraseña del paso 3.
   - O crear las variables de entorno `LALIGA_DB_URL`, `LALIGA_DB_USER` y `LALIGA_DB_PASSWORD` (ver `.env.example`).
5. Ejecutar: en IntelliJ, configuración **LaLiga App**; en cualquier IDE, la clase `es.fraydiegotadeo.laliga.Launcher`;
   desde terminal, `.\mvnw.cmd javafx:run`. Pruebas: `.\mvnw.cmd clean test`.
6. **No hay que importar nada**: al arrancar, la aplicación descarga de football-data.co.uk las temporadas que falten
   (la primera vez, todo el histórico desde 1993/94, unos segundos) y los partidos nuevos de la temporada en curso.
   Hace falta internet la primera vez; después funciona también sin conexión con lo ya guardado.
7. La aplicación se abre en **Próxima jornada**, con una tarjeta de predicción por partido. En el menú lateral están
   también **Simulación** (cómo puede terminar la temporada), **Clasificación** (con las zonas europeas y de descenso)
   y **Partidos** (organizados por jornadas, con buscador por equipo). El selector de arriba cambia de temporada.

## Estado actual

- **Terminado:** histórico 1993/94–2026/27 en MySQL, actualización automática, interfaz con menú lateral (`ui/estilos.css`), clasificación con zonas, partidos por jornadas, predicción de la próxima jornada con backtesting (`docs/MODELO_PREDICCION.md`), simulación Monte Carlo del final de la liga (`docs/SIMULACION.md`).
- **Pendiente:** goleadores y asistentes, instalador, memoria y manuales.

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
