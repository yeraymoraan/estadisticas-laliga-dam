package es.fraydiegotadeo.laliga.repository;

import es.fraydiegotadeo.laliga.model.PartidoHistorico;
import es.fraydiegotadeo.laliga.model.PartidoVista;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Operaciones JDBC parametrizadas. Una importación completa usa una transacción. */
public final class PartidosRepository {
    public List<Integer> listarTemporadas() throws SQLException {
        String sql = "SELECT inicio FROM temporadas ORDER BY inicio DESC";
        try (Connection c = ConexionMySql.abrir();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Integer> anios = new ArrayList<>();
            while (rs.next()) anios.add(rs.getInt(1));
            return anios;
        }
    }

    public List<PartidoVista> listarPartidos(int inicio) throws SQLException {
        String sql = """
            SELECT p.fecha, el.nombre AS local, ev.nombre AS visitante,
                   p.goles_local, p.goles_visitante,
                   p.remates_local, p.remates_visitante, p.corners_local, p.corners_visitante,
                   p.amarillas_local, p.amarillas_visitante
              FROM partidos p
              JOIN temporadas t ON t.id = p.temporada_id
              JOIN equipos el ON el.id = p.equipo_local_id
              JOIN equipos ev ON ev.id = p.equipo_visitante_id
             WHERE t.inicio = ? AND p.goles_local IS NOT NULL
             ORDER BY p.fecha, p.id
            """;
        try (Connection c = ConexionMySql.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, inicio);
            try (ResultSet rs = ps.executeQuery()) {
                List<PartidoVista> partidos = new ArrayList<>();
                while (rs.next()) partidos.add(new PartidoVista(
                        rs.getDate("fecha").toLocalDate(), rs.getString("local"),
                        rs.getString("visitante"), rs.getInt("goles_local"),
                        rs.getInt("goles_visitante"), rs.getObject("remates_local", Integer.class),
                        rs.getObject("remates_visitante", Integer.class),
                        rs.getObject("corners_local", Integer.class),
                        rs.getObject("corners_visitante", Integer.class),
                        rs.getObject("amarillas_local", Integer.class),
                        rs.getObject("amarillas_visitante", Integer.class)));
                return partidos;
            }
        }
    }

    public int importar(int inicio, List<PartidoHistorico> partidos) throws SQLException {
        if (inicio < 1900 || inicio > 2100 || partidos.isEmpty())
            throw new IllegalArgumentException("Temporada o listado de partidos no válido");
        try (Connection c = ConexionMySql.abrir()) {
            c.setAutoCommit(false);
            try {
                long temporadaId = obtenerId(c,
                        "INSERT INTO temporadas (inicio, nombre) VALUES (?, ?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)",
                        inicio, inicio + "/" + String.format("%02d", (inicio + 1) % 100));
                String guardar = """
                    INSERT INTO partidos (
                        temporada_id, fecha, equipo_local_id, equipo_visitante_id,
                        goles_local, goles_visitante, remates_local, remates_visitante,
                        remates_puerta_local, remates_puerta_visitante, corners_local, corners_visitante,
                        amarillas_local, amarillas_visitante, rojas_local, rojas_visitante,
                        faltas_local, faltas_visitante, fuente)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON DUPLICATE KEY UPDATE
                        fecha=VALUES(fecha), goles_local=VALUES(goles_local),
                        goles_visitante=VALUES(goles_visitante),
                        remates_local=COALESCE(VALUES(remates_local),remates_local),
                        remates_visitante=COALESCE(VALUES(remates_visitante),remates_visitante),
                        remates_puerta_local=COALESCE(VALUES(remates_puerta_local),remates_puerta_local),
                        remates_puerta_visitante=COALESCE(VALUES(remates_puerta_visitante),remates_puerta_visitante),
                        corners_local=COALESCE(VALUES(corners_local),corners_local),
                        corners_visitante=COALESCE(VALUES(corners_visitante),corners_visitante),
                        amarillas_local=COALESCE(VALUES(amarillas_local),amarillas_local),
                        amarillas_visitante=COALESCE(VALUES(amarillas_visitante),amarillas_visitante),
                        rojas_local=COALESCE(VALUES(rojas_local),rojas_local),
                        rojas_visitante=COALESCE(VALUES(rojas_visitante),rojas_visitante),
                        faltas_local=COALESCE(VALUES(faltas_local),faltas_local),
                        faltas_visitante=COALESCE(VALUES(faltas_visitante),faltas_visitante)
                    """;
                try (PreparedStatement ps = c.prepareStatement(guardar)) {
                    for (PartidoHistorico p : partidos) {
                        long local = obtenerId(c,
                                "INSERT INTO equipos (nombre) VALUES (?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)",
                                p.local());
                        long visitante = obtenerId(c,
                                "INSERT INTO equipos (nombre) VALUES (?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)",
                                p.visitante());
                        altaParticipante(c, temporadaId, local);
                        altaParticipante(c, temporadaId, visitante);
                        ps.setLong(1, temporadaId);
                        ps.setDate(2, Date.valueOf(p.fecha()));
                        ps.setLong(3, local);
                        ps.setLong(4, visitante);
                        ps.setInt(5, p.golesLocal());
                        ps.setInt(6, p.golesVisitante());
                        Integer[] extras = {p.rematesLocal(), p.rematesVisitante(),
                                p.rematesPuertaLocal(), p.rematesPuertaVisitante(),
                                p.cornersLocal(), p.cornersVisitante(),
                                p.amarillasLocal(), p.amarillasVisitante(),
                                p.rojasLocal(), p.rojasVisitante(),
                                p.faltasLocal(), p.faltasVisitante()};
                        for (int i = 0; i < extras.length; i++) {
                            if (extras[i] == null) ps.setNull(i + 7, Types.INTEGER);
                            else ps.setInt(i + 7, extras[i]);
                        }
                        ps.setString(19, "Football-Data CSV");
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return partidos.size();
            } catch (Exception ex) {
                c.rollback();
                if (ex instanceof SQLException sql) throw sql;
                throw new SQLException("No se pudo importar la temporada: " + ex.getMessage(), ex);
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private static long obtenerId(Connection c, String sql, Object... valores) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < valores.length; i++) ps.setObject(i + 1, valores[i]);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No se ha recuperado el ID generado");
            }
        }
    }

    private static void altaParticipante(Connection c, long temporada, long equipo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT IGNORE INTO participantes (temporada_id, equipo_id) VALUES (?, ?)")) {
            ps.setLong(1, temporada);
            ps.setLong(2, equipo);
            ps.executeUpdate();
        }
    }
}
