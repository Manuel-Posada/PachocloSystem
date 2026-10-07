package com.pachoclosystem.pachoclosystem.model;

/**
 * Autor de un registro clínico tal como era al firmarlo. Es una copia: editar o
 * eliminar después al trabajador no cambia lo ya firmado.
 *
 * @param rol "Doctor" o "Enfermero", como en la API de trabajadores
 * @param especialidad solo para doctores; {@code null} para enfermeros
 * @param nivelExperiencia solo para enfermeros; {@code null} para doctores
 */
public record AutorRegistro(String idTrabajador, String nombreCompleto, String rol, String especialidad,
                            NivelExperiencia nivelExperiencia) {

    public static final String DOCTOR = "Doctor";
    public static final String ENFERMERO = "Enfermero";

    /** Copia los datos del trabajador en este momento. */
    public static AutorRegistro de(TrabajadorHospital trabajador) {
        if (trabajador instanceof Doctor d) {
            return new AutorRegistro(d.getIdTrabajador(), d.getNombreCompleto(), DOCTOR, d.getEspecialidad(), null);
        }
        Enfermero e = (Enfermero) trabajador;
        return new AutorRegistro(e.getIdTrabajador(), e.getNombreCompleto(), ENFERMERO, null,
                e.getNivelExperiencia());
    }
}
