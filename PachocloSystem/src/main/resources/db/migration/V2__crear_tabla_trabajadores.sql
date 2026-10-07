-- Trabajadores del hospital: doctores (con especialidad) y enfermeros (con nivel
-- de experiencia) en una sola tabla. Ids DOC-0001 y ENF-0001, cada uno con su
-- secuencia. El borrado es físico: los registros clínicos guardan su propia copia
-- del autor.

CREATE SEQUENCE doctores_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE enfermeros_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE trabajadores (
    id_trabajador     VARCHAR(20) NOT NULL,
    tipo              VARCHAR(10) NOT NULL,
    nombre_completo   VARCHAR(60) NOT NULL,
    especialidad      TEXT,
    nivel_experiencia VARCHAR(20),
    orden             BIGINT      GENERATED ALWAYS AS IDENTITY,

    CONSTRAINT pk_trabajadores PRIMARY KEY (id_trabajador),
    CONSTRAINT uq_trabajadores_orden UNIQUE (orden),
    CONSTRAINT ck_trabajadores_tipo CHECK (tipo IN ('DOCTOR', 'ENFERMERO')),
    CONSTRAINT ck_trabajadores_nivel CHECK (nivel_experiencia IN ('NOVATO', 'PRINCIPIANTE', 'AVANZADO')),
    CONSTRAINT ck_trabajadores_datos_de_su_tipo CHECK (
        (tipo = 'DOCTOR' AND especialidad IS NOT NULL AND nivel_experiencia IS NULL)
        OR (tipo = 'ENFERMERO' AND nivel_experiencia IS NOT NULL AND especialidad IS NULL))
);
