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

1. Instalar **JDK 21**, **Maven**, **MySQL 8** y opcionalmente Eclipse.
2. Ejecutar el script `sql/schema.sql` en MySQL.
3. Configurar variables de entorno `LALIGA_DB_URL`, `LALIGA_DB_USER`, `LALIGA_DB_PASSWORD` (ver `.env.example`; **no** subir contraseñas).
4. En la carpeta del proyecto: `mvn clean test` y `mvn javafx:run`.
5. Descargar manualmente uno o varios CSV de Primera División (`SP1`). Dentro de la aplicación, seleccionar **Importar CSV**, indicar el año de inicio de la temporada (ej. 2025 para 2025/26) y escoger el archivo.
6. Seleccionar la temporada para consultar partidos y datos disponibles.

## Estado actual

**Primer avance**: estructura técnica y funcionalidad de importación/consulta inicial. Las predicciones y simulaciones siguen **pendientes**. Consulta `docs/ESTADO_Y_PLAN.md` para el progreso, pruebas y próximos pasos. **No afirmar que funciona en Windows antes de ejecutarlo allí**.

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
