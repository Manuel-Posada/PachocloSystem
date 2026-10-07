-- Historial clínico. Cada registro guarda una copia del autor tal como era al
-- firmarlo: editar o eliminar después al trabajador no cambia lo ya firmado.

CREATE TABLE registros_clinicos (
    id_registro        UUID        NOT NULL,
    id_paciente        VARCHAR(20) NOT NULL,
    -- Hora local sin zona (LocalDateTime), al microsegundo.
    fecha              TIMESTAMP   NOT NULL,
    tipo               VARCHAR(20) NOT NULL,
    contenido          TEXT        NOT NULL,
    autor_id           VARCHAR(20) NOT NULL,
    autor_nombre       VARCHAR(60) NOT NULL,
    autor_tipo         VARCHAR(10) NOT NULL,
    autor_especialidad TEXT,
    autor_nivel        VARCHAR(20),
    -- Solo en registros de MEDICACION que descontaron stock.
    id_medicamento     VARCHAR(20),
    cantidad           INTEGER,
    -- Orden de alta: desempata registros con la misma fecha.
    orden              BIGINT      GENERATED ALWAYS AS IDENTITY,

    CONSTRAINT pk_registros_clinicos PRIMARY KEY (id_registro),
    CONSTRAINT uq_registros_clinicos_orden UNIQUE (orden),
    -- Los pacientes no se borran físicamente (baja lógica).
    CONSTRAINT fk_registros_clinicos_paciente FOREIGN KEY (id_paciente) REFERENCES pacientes (id_paciente),
    CONSTRAINT ck_registros_clinicos_tipo CHECK (tipo IN ('DIAGNOSTICO', 'EVOLUCION', 'MEDICACION', 'SIGNOS_VITALES')),
    CONSTRAINT ck_registros_clinicos_autor_tipo CHECK (autor_tipo IN ('DOCTOR', 'ENFERMERO')),
    CONSTRAINT ck_registros_clinicos_autor_nivel CHECK (autor_nivel IN ('NOVATO', 'PRINCIPIANTE', 'AVANZADO')),
    CONSTRAINT ck_registros_clinicos_medicacion CHECK (
        (id_medicamento IS NULL AND cantidad IS NULL)
        OR (id_medicamento IS NOT NULL AND cantidad BETWEEN 1 AND 1000000))
);

CREATE INDEX idx_registros_clinicos_paciente ON registros_clinicos (id_paciente, orden);
CREATE INDEX idx_registros_clinicos_fecha ON registros_clinicos (fecha, orden);
