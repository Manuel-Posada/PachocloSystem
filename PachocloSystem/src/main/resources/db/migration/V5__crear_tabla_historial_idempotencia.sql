-- Claves Idempotency-Key del alta de registros clínicos. Repetir la petición con
-- la misma clave devuelve el registro guardado sin crear otro ni volver a
-- descontar stock.
--
-- respuesta NULL: el intento quedó con resultado incierto (la salida de stock pudo
-- hacerse sin que se guardara el registro); solo se puede repetir la misma petición.

CREATE TABLE historial_idempotencia (
    clave       VARCHAR(100) NOT NULL,
    id_usuario  VARCHAR(20)  NOT NULL,
    id_paciente VARCHAR(20)  NOT NULL,
    -- SHA-256 del cuerpo de la petición, en hexadecimal.
    huella      CHAR(64)     NOT NULL,
    -- El registro creado (RegistroResponse en JSON), o NULL si el resultado fue incierto.
    respuesta   JSONB,
    -- Último uso según el reloj de la aplicación: de él depende la caducidad.
    usada_en    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_historial_idempotencia PRIMARY KEY (clave)
);

-- Para borrar las claves caducadas.
CREATE INDEX idx_historial_idempotencia_usada_en ON historial_idempotencia (usada_en);
