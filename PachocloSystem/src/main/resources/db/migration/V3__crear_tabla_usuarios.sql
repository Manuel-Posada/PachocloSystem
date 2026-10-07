-- Usuarios de acceso. Solo se guarda el hash BCrypt de la contraseña. Ids USR-0001.
--
-- id_trabajador es único aunque el usuario esté desactivado, y no tiene clave
-- foránea: el vínculo se conserva aunque el trabajador se elimine (la cuenta
-- queda desactivada y no se puede reactivar ni vincular a otro usuario).

CREATE SEQUENCE usuarios_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE usuarios (
    id_usuario            VARCHAR(20)  NOT NULL,
    -- Ya normalizado por la aplicación (minúsculas, sin espacios alrededor).
    username              VARCHAR(30)  NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    -- Va en el claim "ver" del JWT: sube al cambiar la contraseña y al desactivar.
    version_token         BIGINT       NOT NULL DEFAULT 0,
    debe_cambiar_password BOOLEAN      NOT NULL DEFAULT FALSE,
    rol                   VARCHAR(10)  NOT NULL,
    id_trabajador         VARCHAR(20),
    activo                BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_usuarios PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT uq_usuarios_id_trabajador UNIQUE (id_trabajador),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMIN', 'DOCTOR', 'ENFERMERO')),
    CONSTRAINT ck_usuarios_version_token CHECK (version_token >= 0),
    -- El ADMIN no tiene trabajador; doctores y enfermeros, sí.
    CONSTRAINT ck_usuarios_vinculo CHECK ((rol = 'ADMIN') = (id_trabajador IS NULL))
);
