package model;
 
import java.util.List;
 

public interface ITrabajadoresRepository {
 
    boolean guardarTrabajador(TrabajadorHospital u);
 
    TrabajadorHospital buscarPorId(String id);
 
    boolean eliminarTrabajador(String id);
 
    List<TrabajadorHospital> obtenerTodos();
}
 