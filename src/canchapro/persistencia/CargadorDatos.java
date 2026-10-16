package canchapro.persistencia;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta la carga de todos los archivos al iniciar el sistema.
 *
 * Los repositorios se cargan en el orden en que se registran, que debe
 * respetar las dependencias entre archivos:
 * usuarios → clientes → canchas → tarifas → servicios → reservas →
 * reserva_servicios → pagos → productos → ventas → detalle_ventas.
 * Así, cuando se lee una reserva, su cliente y su cancha ya existen en memoria.
 */
public class CargadorDatos {

    public static final String CARPETA_RESPALDOS = "respaldos";

    private final Path carpetaDatos;
    private final List<RepositorioCsv<?>> repositorios = new ArrayList<>();

    public CargadorDatos(Path carpetaDatos) {
        this.carpetaDatos = carpetaDatos;
    }

    public void registrar(RepositorioCsv<?> repositorio) {
        repositorios.add(repositorio);
    }

    /**
     * Carga todos los repositorios registrados.
     * Si un archivo tiene líneas inválidas, antes de continuar se guarda una
     * copia del archivo original en la carpeta de respaldos: la próxima vez
     * que se guarde ese archivo, las líneas inválidas desaparecerán, pero
     * seguirán disponibles en la copia para corregirlas.
     */
    public void cargarTodo(RegistroErroresCarga registro) {
        for (RepositorioCsv<?> repositorio : repositorios) {
            repositorio.cargar();
            List<ErrorCarga> errores = repositorio.getErroresCarga();
            if (!errores.isEmpty()) {
                registro.agregarTodos(errores);
                Path respaldo = ArchivoCsv.respaldar(repositorio.getArchivo(),
                        carpetaDatos.resolve(CARPETA_RESPALDOS));
                registro.agregarRespaldo(respaldo);
            }
        }
    }
}
