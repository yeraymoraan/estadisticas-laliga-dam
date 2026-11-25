# LaLiga Stats

Este es mi proyecto de 2º de DAM (IES Fray Diego Tadeo González, curso 2026-27). Es una aplicación de escritorio
para ver estadísticas de la Primera División española, predecir los partidos de la próxima jornada y simular cómo
puede terminar la liga.

Está hecha con **Java 21**, **JavaFX** para la interfaz y **MySQL** para guardar los datos.

## Qué se puede hacer con la aplicación

La aplicación tiene un menú a la izquierda con cuatro pantallas:

- **Próxima jornada**: sale una tarjeta por cada partido con la probabilidad de que gane el local, de empate y de
  que gane el visitante, el marcador más probable y los córners, tarjetas, remates y faltas que se esperan. Si el
  partido ya se ha jugado, también sale el resultado real y si he acertado o no.
- **Simulación**: juega 10.000 veces los partidos que quedan de la temporada y dice con cuántos puntos terminaría
  cada equipo y qué probabilidad tiene de quedar campeón, entrar en Champions o bajar a Segunda.
- **Clasificación**: la tabla de la temporada que elijas, con colores para los puestos de Champions, Europa League,
  Conference League y descenso.
- **Partidos**: todos los partidos de una temporada, separados por jornadas, con sus estadísticas. Si escribes un
  equipo en el buscador te salen todos sus partidos.

Arriba se puede cambiar de temporada. Hay datos desde la 1993/94 hasta la actual.

## Qué hace falta para usarla

- **Java 21** (el JDK).
- **MySQL** con **MySQL Workbench**. Yo la he probado con MySQL 26.10.
- Un IDE: IntelliJ IDEA, Eclipse o NetBeans. No hace falta instalar Maven porque el proyecto ya lo trae (`mvnw.cmd`).
- **Internet la primera vez**, porque es cuando se descargan los datos.

## Cómo ponerla en marcha

1. **Crear la base de datos.** Abre MySQL Workbench, abre el archivo `sql/schema.sql` y ejecútalo con el rayo.
   Esto crea la base de datos `laliga_dam` con sus tablas.
2. **Crear el usuario de la aplicación.** En Workbench ejecuta esto, cambiando `TU_CLAVE` por la contraseña que quieras:
   ```sql
   CREATE USER 'laliga_app'@'localhost' IDENTIFIED BY 'TU_CLAVE';
   GRANT SELECT, INSERT, UPDATE, DELETE ON laliga_dam.* TO 'laliga_app'@'localhost';
   ```
   Ojo con no dejar `TU_CLAVE` tal cual: a mí me pasó y luego la aplicación no podía entrar.
3. **Decirle a la aplicación la contraseña.** Haz una copia del archivo `laliga.properties.example`, llámala
   `laliga.properties` (en la misma carpeta) y cambia `CAMBIAR_POR_TU_CLAVE` por la contraseña del paso 2.
   Este archivo no se sube a GitHub para que no se vea la contraseña.
4. **Abrir el proyecto.** En el IDE abre la carpeta como proyecto Maven y espera a que descargue las librerías.
5. **Ejecutar la clase `Launcher`** (`src/main/java/es/fraydiegotadeo/laliga/Launcher.java`). En IntelliJ ya está
   preparada la configuración **LaLiga App**. Hay que ejecutar `Launcher` y no `App`, porque si no sale el error
   *"JavaFX runtime components are missing"*.
6. **Esperar unos segundos.** La primera vez la aplicación se descarga sola todas las temporadas de football-data.co.uk
   y las guarda en MySQL. Después ya se abre en la pantalla de Próxima jornada.

A partir de ahí solo hay que hacer el paso 5. Cada vez que se abre, la aplicación busca los partidos nuevos y los
añade sin repetir los que ya tenía. Si no hay internet, funciona con lo que ya tiene guardado.

En vez del archivo `laliga.properties` también se pueden usar las variables de entorno `LALIGA_DB_URL`,
`LALIGA_DB_USER` y `LALIGA_DB_PASSWORD` (en `.env.example` hay un ejemplo).

## Cómo funcionan las predicciones

Para cada partido calculo los goles que se espera que marque cada equipo, según lo que marca y lo que encaja en casa
y fuera comparado con la media de la liga. Con esos goles uso la **distribución de Poisson**, que da la probabilidad
de marcar 0, 1, 2... goles, y de ahí saco la probabilidad de 1, X y 2. Los córners, tarjetas, remates y faltas se
calculan con la misma idea. Cada partido se predice solo con los partidos jugados antes de su fecha.

Para comprobarlo he predicho todos los partidos de varias temporadas pasadas: acierta algo más de la mitad, y en
todas las temporadas mejora a decir siempre que gana el local.

La simulación usa el **método de Monte Carlo**: juega al azar los partidos que faltan con esas probabilidades, mira
cómo queda la clasificación y lo repite 10.000 veces.

Son estimaciones, no resultados seguros. El modelo no sabe nada de lesiones, fichajes ni cambios de entrenador.

## Cómo está organizado el código

El código está en `src/main/java/es/fraydiegotadeo/laliga` y lo he separado en paquetes:

| Paquete | Qué hay dentro |
|---|---|
| `model` | Las clases con los datos: partidos jugados, partidos pendientes y los nombres completos de los equipos |
| `importer` | Lo que descarga y lee los CSV de Football-Data |
| `repository` | La conexión con MySQL y las consultas (JDBC con `PreparedStatement`) |
| `service` | Los cálculos: clasificación, jornadas, modelo de Poisson, comprobación del modelo, simulación y actualización de datos |
| `ui` | Las pantallas de JavaFX. Los colores y el diseño están en `src/main/resources/.../estilos.css` |

`App.java` es la ventana principal y `Launcher.java` solo sirve para arrancarla.

## Pruebas

Hay 36 pruebas con JUnit en `src/test/java`: lector de CSV, modelo de Poisson, simulación, jornadas, etc. No
necesitan base de datos. Se pasan con botón derecho sobre `src/test/java` → Run tests, o desde la terminal con:

```
.\mvnw.cmd clean test
```

## Cosas que hay que saber

- Football-Data publica los resultados con unos días de retraso, así que la aplicación siempre va un poco por detrás.
- Los CSV no traen el número de jornada, así que lo calculo contando los partidos de cada equipo. Un partido aplazado
  sale en la jornada en la que se jugó.
- En la clasificación, cuando dos equipos empatan a puntos se ordenan por diferencia de goles, que no es exactamente
  lo que hace LaLiga (allí se mira primero el enfrentamiento directo).
- Las estadísticas que no existen en una temporada (por ejemplo, los córners de los años 90) salen como "N/D".

## Más información

El repositorio con todo el historial de cambios y la documentación está en
https://github.com/yeraymoraan/estadisticas-laliga-dam

Para hacer el proyecto me he apoyado en un asistente de inteligencia artificial. He revisado y probado cada parte,
y el código está comentado para poder explicarlo.
