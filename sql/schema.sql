-- LaLiga Stats · Esquema inicial · MySQL 8+
-- Ejecutar con un usuario con permisos de creación.
CREATE DATABASE IF NOT EXISTS laliga_dam CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE laliga_dam;

CREATE TABLE IF NOT EXISTS temporadas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inicio SMALLINT NOT NULL UNIQUE,
    nombre VARCHAR(9) NOT NULL UNIQUE,
    CHECK (inicio BETWEEN 1900 AND 2100)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS equipos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS participantes (
    temporada_id BIGINT NOT NULL,
    equipo_id BIGINT NOT NULL,
    PRIMARY KEY (temporada_id, equipo_id),
    FOREIGN KEY (temporada_id) REFERENCES temporadas(id),
    FOREIGN KEY (equipo_id) REFERENCES equipos(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS partidos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    temporada_id BIGINT NOT NULL,
    jornada TINYINT UNSIGNED NULL,
    fecha DATE NOT NULL,
    equipo_local_id BIGINT NOT NULL,
    equipo_visitante_id BIGINT NOT NULL,
    goles_local TINYINT UNSIGNED NULL,
    goles_visitante TINYINT UNSIGNED NULL,
    -- Estadísticas opcionales: NULL = dato NO disponible, nunca 0 inventado
    remates_local SMALLINT UNSIGNED NULL,
    remates_visitante SMALLINT UNSIGNED NULL,
    remates_puerta_local SMALLINT UNSIGNED NULL,
    remates_puerta_visitante SMALLINT UNSIGNED NULL,
    corners_local SMALLINT UNSIGNED NULL,
    corners_visitante SMALLINT UNSIGNED NULL,
    amarillas_local SMALLINT UNSIGNED NULL,
    amarillas_visitante SMALLINT UNSIGNED NULL,
    rojas_local SMALLINT UNSIGNED NULL,
    rojas_visitante SMALLINT UNSIGNED NULL,
    faltas_local SMALLINT UNSIGNED NULL,
    faltas_visitante SMALLINT UNSIGNED NULL,
    fuente VARCHAR(120) NULL,
    FOREIGN KEY (temporada_id) REFERENCES temporadas(id),
    FOREIGN KEY (equipo_local_id) REFERENCES equipos(id),
    FOREIGN KEY (equipo_visitante_id) REFERENCES equipos(id),
    UNIQUE (temporada_id, equipo_local_id, equipo_visitante_id),
    INDEX idx_partidos_fecha (temporada_id, fecha),
    CHECK (equipo_local_id <> equipo_visitante_id),
    CHECK ((goles_local IS NULL AND goles_visitante IS NULL) OR
           (goles_local IS NOT NULL AND goles_visitante IS NOT NULL))
) ENGINE=InnoDB;
