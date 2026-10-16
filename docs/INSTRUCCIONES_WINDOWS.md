# Instalación y primera prueba en Windows

1. Instalar Java JDK 21, Apache Maven y MySQL 8+; comprobar con java -version, mvn -version.
2. Crear la base de datos ejecutando sql/schema.sql con MySQL Workbench.
3. Crear un usuario MySQL **local** con permisos SELECT/INSERT/UPDATE en laliga_dam (no usar credenciales en código).
4. Configurar variables de entorno LALIGA_DB_URL, LALIGA_DB_USER, LALIGA_DB_PASSWORD. En PowerShell para esta sesión:
   $env:LALIGA_DB_USER = "usuario_local"
   $env:LALIGA_DB_PASSWORD = "tu_clave_local"
   $env:LALIGA_DB_URL = "jdbc:mysql://localhost:3306/laliga_dam?serverTimezone=Europe/Madrid"
5. En el directorio con pom.xml, ejecutar: mvn clean test
6. Iniciar la interfaz: mvn javafx:run
7. Descargar manualmente un CSV SP1 desde https://www.football-data.co.uk/spainm.php
8. Pulsar Importar CSV, introducir el año de inicio y seleccionar el CSV descargado.
9. Verificar en las pestañas Partidos y Clasificación que se muestran los registros esperados.

No subir el CSV original ni contraseñas al repositorio.

Estado de comprobación: pendiente de ejecutar end-to-end en Windows y MySQL real; el workflow CI comprobará la compilación y JUnit en GitHub Actions.
