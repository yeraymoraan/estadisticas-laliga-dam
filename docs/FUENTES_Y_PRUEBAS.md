# Fuentes y plan de pruebas

## Datos reales

- Football-Data, Primera División: https://www.football-data.co.uk/spainm.php. Uso de CSV descargados manualmente, respetando condiciones y límites del proveedor.
- Columnas comunes: Date, HomeTeam, AwayTeam, FTHG, FTAG, HS, AS, HST, AST, HC, AC, HY, AY, HR, AR; algunas temporadas no tienen todas.
- No publicar una copia completa de los archivos originales sin verificar permisos.
- Nunca inventar una estadística ausente: almacenarla como NULL.
- Para jornadas futuras se analizará una API REST legítima. El importador de esta fase se destina **solo a partidos terminados**.

## Casos de prueba previstos

1. Importar un CSV válido de temporada real y contar equipos/partidos.
2. Reimportar el mismo CSV y comprobar que no genera duplicados.
3. Introducir una temporada con columnas estadísticas ausentes y comprobar NULL.
4. Un partido con mismo local/visitante o goles negativos se rechaza.
5. Una temporada inexistente muestra listado vacío sin bloquear la aplicación.
6. Si MySQL no está activo, la aplicación muestra un error comprensible.
7. Las medias omiten NULL y muestran el tamaño de muestra.
8. Backtest del modelo: ocultar partidos futuros en cada fecha y medir aciertos/calibración, sin utilizar información posterior.

**Evidencias pendientes:** capturas de UI, salida de Maven/JUnit, exportación SQL e informe de prueba en Windows.
