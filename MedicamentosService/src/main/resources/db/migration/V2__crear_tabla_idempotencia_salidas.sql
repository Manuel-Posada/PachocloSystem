-- Salidas de stock ya hechas con una clave de idempotencia (cabecera
-- Idempotency-Key). Repetir la petición con la misma clave devuelve la
-- respuesta guardada sin volver a descontar.
--
-- Sin clave foránea a medicamentos: borrar un medicamento no se bloquea por
-- sus salidas, y la repetición sigue devolviendo la respuesta de entonces.

CREATE TABLE idempotencia_salidas (
    clave          VARCHAR(100) NOT NULL,
    id_medicamento VARCHAR(20)  NOT NULL,
    cantidad       INTEGER      NOT NULL,
    -- La respuesta tal como se devolvió (MedicamentoResponse en JSON).
    respuesta      JSONB        NOT NULL,
    -- Momento de la salida según el reloj de la aplicación: de él depende la caducidad.
    hecha_en       TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_idempotencia_salidas PRIMARY KEY (clave),
    CONSTRAINT ck_idempotencia_salidas_cantidad CHECK (cantidad BETWEEN 1 AND 1000000)
);

-- Para borrar las claves caducadas.
CREATE INDEX idx_idempotencia_salidas_hecha_en ON idempotencia_salidas (hecha_en);
