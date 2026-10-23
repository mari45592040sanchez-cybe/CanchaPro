package canchapro.persistencia;

import java.nio.file.Path;

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
    // POO - Colecciones / Polimorfismo: arreglo del tipo base RepositorioCsv; cada posición guarda
    // un repositorio distinto (usuarios, clientes...) y todos se tratan igual al llamar cargar().
    private RepositorioCsv<?>[] repositorios = new RepositorioCsv<?>[15];
    private int cantidad;

    public CargadorDatos(Path carpetaDatos) {
        this.carpetaDatos = carpetaDatos;
    }

    public void registrar(RepositorioCsv<?> repositorio) {
        if (cantidad == repositorios.length) {
            RepositorioCsv<?>[] mayor = new RepositorioCsv<?>[repositorios.length * 2];
            for (int i = 0; i < cantidad; i++) {
                mayor[i] = repositorios[i];
            }
            repositorios = mayor;
        }
        repositorios[cantidad] = repositorio;
        cantidad++;
    }

    /**
     * Carga todos los repositorios registrados.
     * Si un archivo tiene líneas inválidas, antes de continuar se guarda una
     * copia del archivo original en la carpeta de respaldos: la próxima vez
     * que se guarde ese archivo, las líneas inválidas desaparecerán, pero
     * seguirán disponibles en la copia para corregirlas.
     */
    public void cargarTodo(RegistroErroresCarga registro) {
        for (int i = 0; i < cantidad; i++) {
            RepositorioCsv<?> repositorio = repositorios[i];
            repositorio.cargar();
            ErrorCarga[] errores = repositorio.getErroresCarga();
            if (errores.length > 0) {
                registro.agregarTodos(errores);
                Path respaldo = ArchivoCsv.respaldar(repositorio.getArchivo(),
                        carpetaDatos.resolve(CARPETA_RESPALDOS));
                registro.agregarRespaldo(respaldo);
            }
        }
    }
}
