-- Pacientes. Los ids tienen el formato PAC-0001: el número sale de la secuencia y
-- el formato lo pone la aplicación. La baja es lógica (activo = false): el
-- paciente y su historial se conservan.

CREATE SEQUENCE pacientes_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE pacientes (
    id_paciente VARCHAR(20) NOT NULL,
    nombre      VARCHAR(60) NOT NULL,
    edad        INTEGER     NOT NULL,
    habitacion  INTEGER     NOT NULL,
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    -- Orden de alta: los listados salen en este orden.
    orden       BIGINT      GENERATED ALWAYS AS IDENTITY,

    CONSTRAINT pk_pacientes PRIMARY KEY (id_paciente),
    CONSTRAINT uq_pacientes_orden UNIQUE (orden),
    CONSTRAINT ck_pacientes_edad CHECK (edad BETWEEN 0 AND 120),
    CONSTRAINT ck_pacientes_habitacion CHECK (habitacion BETWEEN 1 AND 999)
);
