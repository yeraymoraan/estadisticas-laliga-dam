package es.fraydiegotadeo.laliga.repository;

import es.fraydiegotadeo.laliga.model.PartidoHistorico;
import es.fraydiegotadeo.laliga.model.PartidoVista;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la base de datos para temporadas, equipos y partidos (capa repository).
 *
 * Todas las consultas usan PreparedStatement con parámetros "?": así el valor nunca se
 * concatena dentro del SQL y se evita la inyección SQL.
 */
public final class PartidosRepository {

    private static final String SQL_LISTAR_TEMPORADAS =
            "SELECT inicio FROM temporadas ORDER BY inicio DESC";

    // Une partidos con temporadas y equipos (JOIN) para obtener los nombres en lugar de los id
    private static final String SQL_LISTAR_PARTIDOS = """
            SELECT p.fecha, el.nombre AS local, ev.nombre AS visitante,
                   p.goles_local, p.goles_visitante,
                   p.remates_local, p.remates_visitante, p.remates_puerta_local, p.remates_puerta_visitante,
                   p.corners_local, p.corners_visitante, p.amarillas_local, p.amarillas_visitante,
                   p.rojas_local, p.rojas_visitante, p.faltas_local, p.faltas_visitante
              FROM partidos p
              JOIN temporadas t ON t.id = p.temporada_id
              JOIN equipos el ON el.id = p.equipo_local_id
              JOIN equipos ev ON ev.id = p.equipo_visitante_id
             WHERE t.inicio = ? AND p.goles_local IS NOT NULL
             ORDER BY p.fecha, p.id
            """;

    // "ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)": si la temporada o el equipo ya existían
    // no se crea otro registro y, aun así, getGeneratedKeys() nos devuelve el id del existente.
    private static final String SQL_GUARDAR_TEMPORADA =
            "INSERT INTO temporadas (inicio, nombre) VALUES (?, ?) ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)";
    private static final String SQL_GUARDAR_EQUIPO =
            "INSERT INTO equipos (nombre) VALUES (?) ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)";
    private static final String SQL_GUARDAR_PARTICIPANTE =
            "INSERT IGNORE INTO participantes (temporada_id, equipo_id) VALUES (?, ?)";

    // Si el partido ya estaba guardado (mismo local y visitante en la temporada) se actualiza.
    // COALESCE(nuevo, actual) conserva el dato antiguo cuando el CSV nuevo no lo trae (null).
    private static final String SQL_GUARDAR_PARTIDO = """
            INSERT INTO partidos (
                temporada_id, fecha, equipo_local_id, equipo_visitante_id,
                goles_local, goles_visitante, remates_local, remates_visitante,
                remates_puerta_local, remates_puerta_visitante, corners_local, corners_visitante,
                amarillas_local, amarillas_visitante, rojas_local, rojas_visitante,
                faltas_local, faltas_visitante, fuente)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
                fecha = VALUES(fecha),
                goles_local = VALUES(goles_local),
                goles_visitante = VALUES(goles_visitante),
                remates_local = COALESCE(VALUES(remates_local), remates_local),
                remates_visitante = COALESCE(VALUES(remates_visitante), remates_visitante),
                remates_puerta_local = COALESCE(VALUES(remates_puerta_local), remates_puerta_local),
                remates_puerta_visitante = COALESCE(VALUES(remates_puerta_visitante), remates_puerta_visitante),
                corners_local = COALESCE(VALUES(corners_local), corners_local),
                corners_visitante = COALESCE(VALUES(corners_visitante), corners_visitante),
                amarillas_local = COALESCE(VALUES(amarillas_local), amarillas_local),
                amarillas_visitante = COALESCE(VALUES(amarillas_visitante), amarillas_visitante),
                rojas_local = COALESCE(VALUES(rojas_local), rojas_local),
                rojas_visitante = COALESCE(VALUES(rojas_visitante), rojas_visitante),
                faltas_local = COALESCE(VALUES(faltas_local), faltas_local),
                faltas_visitante = COALESCE(VALUES(faltas_visitante), faltas_visitante)
            """;

    /** Texto que se guarda en la columna "fuente" para saber de dónde viene cada partido. */
    private static final String FUENTE_CSV = "Football-Data CSV";

    /**
     * @return años de inicio de las temporadas guardadas (2025 = temporada 2025/26), de la más reciente a la más antigua
     */
    public List<Integer> listarTemporadas() throws SQLException {
        List<Integer> anios = new ArrayList<>();
        try (Connection conexion = ConexionMySql.abrir();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR_TEMPORADAS);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                anios.add(resultado.getInt("inicio"));
            }
        }
        return anios;
    }

    /**
     * @param inicio año de inicio de la temporada
     * @return partidos terminados de esa temporada ordenados por fecha
     */
    public List<PartidoVista> listarPartidos(int inicio) throws SQLException {
        List<PartidoVista> partidos = new ArrayList<>();
        try (Connection conexion = ConexionMySql.abrir();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR_PARTIDOS)) {
            sentencia.setInt(1, inicio);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    partidos.add(leerPartido(resultado));
                }
            }
        }
        return partidos;
    }

    /**
     * Guarda en MySQL todos los partidos de una temporada.
     *
     * Se hace en una TRANSACCIÓN: o se guardan todos los partidos o, si falla uno, se deshace
     * todo (rollback) y la base de datos queda como estaba. Reimportar el mismo CSV no duplica
     * partidos porque la tabla tiene una clave única (temporada, local, visitante).
     *
     * @param inicio   año de inicio de la temporada
     * @param partidos partidos leídos del CSV
     * @return número de partidos procesados
     */
    public int importar(int inicio, List<PartidoHistorico> partidos) throws SQLException {
        if (inicio < 1900 || inicio > 2100 || partidos.isEmpty()) {
            throw new IllegalArgumentException("Temporada o listado de partidos no válido");
        }

        try (Connection conexion = ConexionMySql.abrir()) {
            // Desactivar el autocommit inicia la transacción: nada se confirma hasta commit()
            conexion.setAutoCommit(false);
            try {
                long temporadaId = obtenerId(conexion, SQL_GUARDAR_TEMPORADA, inicio, nombreTemporada(inicio));

                try (PreparedStatement sentencia = conexion.prepareStatement(SQL_GUARDAR_PARTIDO)) {
                    for (PartidoHistorico partido : partidos) {
                        long localId = obtenerId(conexion, SQL_GUARDAR_EQUIPO, partido.local());
                        long visitanteId = obtenerId(conexion, SQL_GUARDAR_EQUIPO, partido.visitante());
                        guardarParticipante(conexion, temporadaId, localId);
                        guardarParticipante(conexion, temporadaId, visitanteId);

                        rellenarPartido(sentencia, temporadaId, localId, visitanteId, partido);
                        // addBatch acumula la sentencia y executeBatch las envía todas juntas (más rápido)
                        sentencia.addBatch();
                    }
                    sentencia.executeBatch();
                }

                conexion.commit();
                return partidos.size();
            } catch (SQLException | RuntimeException error) {
                conexion.rollback();
                if (error instanceof SQLException) {
                    throw (SQLException) error;
                }
                throw new SQLException("No se pudo importar la temporada: " + error.getMessage(), error);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    /** Ejemplo: 2025 → "2025/26". */
    private static String nombreTemporada(int inicio) {
        return inicio + "/" + String.format("%02d", (inicio + 1) % 100);
    }

    /** Asigna los 19 parámetros "?" de SQL_GUARDAR_PARTIDO. */
    private static void rellenarPartido(PreparedStatement sentencia, long temporadaId, long localId,
                                        long visitanteId, PartidoHistorico p) throws SQLException {
        sentencia.setLong(1, temporadaId);
        sentencia.setDate(2, Date.valueOf(p.fecha()));
        sentencia.setLong(3, localId);
        sentencia.setLong(4, visitanteId);
        sentencia.setInt(5, p.golesLocal());
        sentencia.setInt(6, p.golesVisitante());
        asignarEstadistica(sentencia, 7, p.rematesLocal());
        asignarEstadistica(sentencia, 8, p.rematesVisitante());
        asignarEstadistica(sentencia, 9, p.rematesPuertaLocal());
        asignarEstadistica(sentencia, 10, p.rematesPuertaVisitante());
        asignarEstadistica(sentencia, 11, p.cornersLocal());
        asignarEstadistica(sentencia, 12, p.cornersVisitante());
        asignarEstadistica(sentencia, 13, p.amarillasLocal());
        asignarEstadistica(sentencia, 14, p.amarillasVisitante());
        asignarEstadistica(sentencia, 15, p.rojasLocal());
        asignarEstadistica(sentencia, 16, p.rojasVisitante());
        asignarEstadistica(sentencia, 17, p.faltasLocal());
        asignarEstadistica(sentencia, 18, p.faltasVisitante());
        sentencia.setString(19, FUENTE_CSV);
    }

    /** Si el dato es null se guarda NULL en MySQL (no disponible); setInt no admite null. */
    private static void asignarEstadistica(PreparedStatement sentencia, int posicion, Integer valor)
            throws SQLException {
        if (valor == null) {
            sentencia.setNull(posicion, Types.INTEGER);
        } else {
            sentencia.setInt(posicion, valor);
        }
    }

    /** Convierte la fila actual del ResultSet en un PartidoVista. */
    private static PartidoVista leerPartido(ResultSet rs) throws SQLException {
        // getObject(..., Integer.class) devuelve null si la columna es NULL.
        // getInt devolvería 0, y eso convertiría un dato ausente en un 0 falso.
        return new PartidoVista(
                rs.getDate("fecha").toLocalDate(),
                rs.getString("local"),
                rs.getString("visitante"),
                rs.getInt("goles_local"),
                rs.getInt("goles_visitante"),
                rs.getObject("remates_local", Integer.class),
                rs.getObject("remates_visitante", Integer.class),
                rs.getObject("remates_puerta_local", Integer.class),
                rs.getObject("remates_puerta_visitante", Integer.class),
                rs.getObject("corners_local", Integer.class),
                rs.getObject("corners_visitante", Integer.class),
                rs.getObject("amarillas_local", Integer.class),
                rs.getObject("amarillas_visitante", Integer.class),
                rs.getObject("rojas_local", Integer.class),
                rs.getObject("rojas_visitante", Integer.class),
                rs.getObject("faltas_local", Integer.class),
                rs.getObject("faltas_visitante", Integer.class));
    }

    /**
     * Ejecuta un INSERT y devuelve el id generado (o el id del registro que ya existía).
     *
     * @param valores valores para los "?" de la sentencia, en orden
     */
    private static long obtenerId(Connection conexion, String sql, Object... valores) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < valores.length; i++) {
                sentencia.setObject(i + 1, valores[i]);
            }
            sentencia.executeUpdate();

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getLong(1);
                }
                throw new SQLException("No se ha recuperado el ID generado");
            }
        }
    }

    /** Registra que un equipo participa en una temporada. INSERT IGNORE evita el error si ya estaba. */
    private static void guardarParticipante(Connection conexion, long temporadaId, long equipoId)
            throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_GUARDAR_PARTICIPANTE)) {
            sentencia.setLong(1, temporadaId);
            sentencia.setLong(2, equipoId);
            sentencia.executeUpdate();
        }
    }
}
