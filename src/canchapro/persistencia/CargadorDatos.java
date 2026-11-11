package canchapro.persistencia;

import java.nio.file.Path;

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
