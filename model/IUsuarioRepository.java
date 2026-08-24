package model;
 
import java.util.List;
 

public interface IUsuarioRepository {
 
    boolean guardarUsuario(TrabajadorHospital u);
 
    TrabajadorHospital buscarPorId(String id);
 
    boolean eliminarUsuario(String id);
 
    List<TrabajadorHospital> obtenerTodos();
}
 