package com.pachoclosystem.pachoclosystem.repository;
 
import com.pachoclosystem.pachoclosystem.model.TrabajadorHospital;

import java.util.List;
 

public interface ITrabajadoresRepository {
    String generarNuevoId(String prefijo);
 
    boolean guardarTrabajador(TrabajadorHospital u);
 
    TrabajadorHospital buscarPorId(String id);
 
    boolean eliminarTrabajador(String id);
 
    List<TrabajadorHospital> obtenerTodos();
}   
 