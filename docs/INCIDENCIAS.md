# Registro de incidencias

Cada incidencia indica cómo se detectó, la evidencia y su estado. Solo se registran problemas reproducidos.

| ID | Fecha | Descripción | Cómo se reprodujo | Estado |
|---|---|---|---|---|
| INC-01 | 13/11/2026 | Ejecutar `App` directamente (botón Run de IntelliJ sobre `main`) falla: *JavaFX runtime components are missing*. | `java -cp target/classes;<dependencias> es.fraydiegotadeo.laliga.App` | Resuelta: clase `Launcher` + configuración `.run/LaLiga App.run.xml`. Arranque verificado. |
| INC-02 | 13/11/2026 | El parser rechaza el CSV real de 1995/96: *A header name is missing* (la cabecera termina en columnas vacías `,,,,`). | Lectura de `data/SP1_9596.csv` (football-data.co.uk) con `CsvPartidosParser`. | Pendiente |
| INC-03 | 13/11/2026 | El diálogo de importación rechaza años anteriores a 2000 (`App.java`), aunque Football-Data ofrece SP1 desde 1993/94. | Revisión de código: condición `inicio < 2000`. Pendiente de probar en la interfaz con MySQL. | Pendiente |
| INC-04 | 13/11/2026 | No hay servidor MySQL instalado (solo Workbench y la MariaDB 10.4 de XAMPP). | `Get-Service`, puerto 3306 sin escucha. | Resuelta: MySQL Community Server 26.10 como servicio `MySQL2610`. |
| INC-05 | 13/11/2026 | Maven no está en el PATH de Windows (solo el integrado en IntelliJ). | `mvn -version` no encontrado. | Resuelta: Maven Wrapper (`mvnw.cmd`). `clean test` verificado. |
| INC-06 | 13/11/2026 | *Access denied for user 'laliga_app'@'localhost'*: el usuario se creó copiando el ejemplo con la clave de marcador `TU_CLAVE`, distinta de la guardada en `LALIGA_DB_PASSWORD`. | Conexión con `ConexionMySql`. | Resuelta: `ALTER USER` con la clave real. Verificado: conecta con la variable de Windows y `TU_CLAVE` es rechazada. La aplicación abre sin errores y lee las 5 temporadas. Lección para el manual: avisar de sustituir el marcador. |
| INC-07 | 13/11/2026 | La clasificación calculada de 2024/25 coincide en puntos y goles con la oficial, pero ordena distinto 4 grupos de equipos empatados a puntos (Osasuna/Rayo, Valencia/Real Sociedad, Getafe/Alavés/Espanyol, Sevilla/Girona), porque no aplica el desempate oficial por enfrentamientos directos. | `ClasificacionService` sobre los 380 partidos importados de 2024/25. | Pendiente: implementar el desempate oficial. |

## Comprobaciones correctas (13/11/2026)

- `mvn clean test` en Windows 11 + JDK 21: 3 pruebas, 0 fallos. Tras comentar el código y añadir `PartidoHistoricoTest`: 5 pruebas, 0 fallos, y mismos resultados con los CSV reales.
- `mvn javafx:run`: la ventana se abre y, sin `LALIGA_DB_USER`, muestra un aviso de error comprensible.
- Parser con CSV reales: 2000/01, 2010/11, 2024/25 y 2025/26 → 380 partidos y 20 equipos cada una; 2026/27 → 119 partidos jugados (hasta el 08/11/2026).
- 2000/01 no contiene córners: los 380 partidos quedan con `NULL` (se mostrará N/D), no con 0.

## Prueba de integración con MySQL 26.10 (13/11/2026)

Realizada con el mismo código que usa la aplicación (`CsvPartidosParser` + `PartidosRepository`), conectando como `laliga_app`.

- Esquema: 4 tablas, 5 claves foráneas, 3 restricciones CHECK, índices únicos y `utf8mb4_unicode_ci`.
- Permisos de `laliga_app`: solo `SELECT, INSERT, UPDATE, DELETE` sobre `laliga_dam`.
- Primera importación:

| Temporada | Partidos | Equipos | Goles | Sin córners | Primera fecha | Última fecha |
|---|---:|---:|---:|---:|---|---|
| 2000/01 | 380 | 20 | 1095 | 380 | 09/09/2000 | 17/06/2001 |
| 2010/11 | 380 | 20 | 1042 | 0 | 28/08/2010 | 21/05/2011 |
| 2024/25 | 380 | 20 | 995 | 0 | 15/08/2024 | 25/05/2025 |
| 2025/26 | 380 | 20 | 1024 | 0 | 15/08/2025 | 24/05/2026 |
| 2026/27 | 119 | 20 | 344 | 0 | 15/08/2026 | 08/11/2026 |

- 1995/96 no se importa (INC-02).
- **Reimportación de los mismos archivos:** se mantienen 1.639 partidos y 31 equipos → no hay duplicados.
- Clasificación 2024/25: puntos, victorias, empates, derrotas y goles de los 20 equipos coinciden con la oficial
  (Barcelona 88, Real Madrid 84, Atlético 76…). Diferencias solo en el orden de empatados (INC-07).
- Los nombres de Football-Data son abreviados (`Ath Madrid`, `Espanol`, `Sociedad`, `Sp Gijon`…): conviene
  un nombre para mostrar en la interfaz.
