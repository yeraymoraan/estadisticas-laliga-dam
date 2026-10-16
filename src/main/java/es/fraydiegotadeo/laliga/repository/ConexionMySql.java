package es.fraydiegotadeo.laliga.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Obtiene credenciales de variables de entorno. Nunca incorporar claves al repositorio. */
public final class ConexionMySql {
    private ConexionMySql() {}

    public static Connection abrir() throws SQLException {
        String usuario = System.getenv("LALIGA_DB_USER");
        if (usuario == null || usuario.isBlank()) {
            throw new SQLException("Configura LALIGA_DB_USER antes de iniciar la aplicación.");
        }
        String url = System.getenv().getOrDefault("LALIGA_DB_URL",
                "jdbc:mysql://localhost:3306/laliga_dam?serverTimezone=Europe/Madrid");
        String password = System.getenv().getOrDefault("LALIGA_DB_PASSWORD", "");
        return DriverManager.getConnection(url, usuario, password);
    }
}
