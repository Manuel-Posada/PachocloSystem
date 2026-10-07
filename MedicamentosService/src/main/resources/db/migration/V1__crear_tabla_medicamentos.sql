-- Medicamentos con su stock. Los ids tienen el formato MED-0001: el número sale
-- de la secuencia y el formato lo pone la aplicación.

CREATE SEQUENCE medicamentos_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE medicamentos (
    id_medicamento    VARCHAR(20)  NOT NULL,
    nombre            VARCHAR(80)  NOT NULL,
    principio_activo  VARCHAR(80)  NOT NULL,
    presentacion      VARCHAR(20)  NOT NULL,
    concentracion     VARCHAR(40)  NOT NULL,
    laboratorio       VARCHAR(80)  NOT NULL,
    lote              VARCHAR(40)  NOT NULL,
    stock_minimo      INTEGER      NOT NULL,
    -- Sin máximo propio: las entradas pueden superar 1.000.000 hasta el límite de int.
    cantidad_stock    INTEGER      NOT NULL,
    fecha_vencimiento DATE         NOT NULL,
    ubicacion         VARCHAR(80)  NOT NULL,
    -- Nombre + concentración + presentación + lote normalizados. Lo calcula la
    -- aplicación (DatosMedicamento.claveUnica) para que la normalización sea
    -- exactamente la misma que valida los duplicados.
    clave_unica       TEXT         NOT NULL,

    CONSTRAINT pk_medicamentos PRIMARY KEY (id_medicamento),
    CONSTRAINT uq_medicamentos_clave_unica UNIQUE (clave_unica),
    CONSTRAINT ck_medicamentos_presentacion CHECK (presentacion IN
        ('TABLETA', 'CAPSULA', 'JARABE', 'SUSPENSION', 'INYECTABLE', 'CREMA', 'GOTAS', 'OTRO')),
    CONSTRAINT ck_medicamentos_stock_minimo CHECK (stock_minimo BETWEEN 0 AND 1000000),
    CONSTRAINT ck_medicamentos_cantidad_stock CHECK (cantidad_stock >= 0)
);
