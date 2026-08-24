package model;
 
import java.util.List;
 
/**
 * Interfaz que define el contrato de persistencia para los objetos TrabajadorHospital.
 * Es implementada por UsuarioRepositoryImpl.
 */
public interface IUsuarioRepository {
 
    boolean guardarUsuario(TrabajadorHospital u);
 
    TrabajadorHospital buscarPorId(String id);
 
    boolean eliminarUsuario(String id);
 
    List<TrabajadorHospital> obtenerTodos();
}
 