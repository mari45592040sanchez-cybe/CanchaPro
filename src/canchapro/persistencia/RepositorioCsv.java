package canchapro.persistencia;

import canchapro.util.Identificable;
import canchapro.util.excepciones.CanchaProException;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Base genérica para guardar una colección de entidades en un archivo CSV.
 * Mantiene los objetos en memoria (indexados por código) y los escribe al
 * archivo cada vez que se llama a {@link #guardar()}.
 *
 * Cada repositorio concreto indica el formato de su archivo implementando
 * {@link #encabezado()}, {@link #cantidadCampos()}, {@link #desdeCsv(String[])}
 * y {@link #aCsv(Identificable)}.
 *
 * @param <T> tipo de entidad
 */
public abstract class RepositorioCsv<T extends Identificable> {

    private final Path archivo;
    private final Map<String, T> datos = new LinkedHashMap<>();
    private final List<ErrorCarga> erroresCarga = new ArrayList<>();

    protected RepositorioCsv(Path archivo) {
        this.archivo = archivo;
    }

    // --- Formato del archivo (lo define cada repositorio) ---

    /** Encabezado del archivo, por ejemplo "codigo;nombre;telefono". */
    protected abstract String encabezado();

    /** Cantidad exacta de campos que debe tener cada línea. */
    protected abstract int cantidadCampos();

    /** Reconstruye el objeto a partir de los campos de una línea. Lanza una excepción si algún campo es inválido. */
    protected abstract T desdeCsv(String[] campos);

    /** Convierte el objeto a una línea del archivo (sin salto de línea). */
    protected abstract String aCsv(T objeto);

    /**
     * Lee el archivo y reconstruye los objetos. Las líneas con errores se
     * omiten y quedan registradas en {@link #getErroresCarga()}; el resto
     * de la información se carga normalmente.
     */
    public void cargar() {
        datos.clear();
        erroresCarga.clear();
        String nombre = archivo.getFileName().toString();
        ContenidoCsv contenido = ArchivoCsv.leer(archivo, encabezado());

        if (!contenido.encabezado().isEmpty() && !contenido.encabezado().equals(encabezado())) {
            erroresCarga.add(new ErrorCarga(nombre, 1, contenido.encabezado(),
                    "encabezado distinto al esperado (" + encabezado() + ")"));
        }

        for (LineaCsv linea : contenido.lineas()) {
            try {
                String[] campos = linea.campos();
                if (campos.length != cantidadCampos()) {
                    throw new IllegalArgumentException("se esperaban " + cantidadCampos()
                            + " campos y se encontraron " + campos.length);
                }
                T objeto = desdeCsv(campos);
                if (datos.containsKey(objeto.getCodigo())) {
                    throw new IllegalArgumentException("código duplicado " + objeto.getCodigo());
                }
                datos.put(objeto.getCodigo(), objeto);
            } catch (RuntimeException e) {
                // Cubre campos inválidos, fechas/números mal escritos, enums
                // desconocidos y referencias a registros inexistentes.
                erroresCarga.add(new ErrorCarga(nombre, linea.numero(), linea.texto(), describir(e)));
            }
        }
        despuesDeCargar();
    }

    /**
     * Punto de extensión: se ejecuta cada vez que termina una carga (también
     * al recargar tras un error de escritura). Las subclases lo usan para
     * reconstruir relaciones con otros repositorios.
     */
    protected void despuesDeCargar() {
    }

    /**
     * Escribe todos los objetos en el archivo. Si la escritura falla, se
     * vuelve a cargar el archivo para que la memoria no quede con cambios
     * que no se guardaron, y se relanza el error.
     */
    public void guardar() {
        List<String> lineas = new ArrayList<>(datos.size());
        for (T objeto : datos.values()) {
            lineas.add(aCsv(objeto));
        }
        try {
            ArchivoCsv.escribir(archivo, encabezado(), lineas);
        } catch (CanchaProException e) {
            try {
                cargar();
            } catch (CanchaProException ignorada) {
                // Si tampoco se puede leer, se conserva el estado en memoria.
            }
            throw e;
        }
    }

    public void agregar(T objeto) {
        if (datos.containsKey(objeto.getCodigo())) {
            throw new ReglaNegocioException("Ya existe un registro con el código " + objeto.getCodigo());
        }
        datos.put(objeto.getCodigo(), objeto);
    }

    public void actualizar(T objeto) {
        if (!datos.containsKey(objeto.getCodigo())) {
            throw new EntidadNoEncontradaException("No existe un registro con el código " + objeto.getCodigo());
        }
        datos.put(objeto.getCodigo(), objeto);
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(datos.get(codigo));
    }

    public boolean existe(String codigo) {
        return datos.containsKey(codigo);
    }

    public List<T> listar() {
        return List.copyOf(datos.values());
    }

    public boolean estaVacio() {
        return datos.isEmpty();
    }

    public Set<String> codigos() {
        return Collections.unmodifiableSet(datos.keySet());
    }

    public List<ErrorCarga> getErroresCarga() {
        return List.copyOf(erroresCarga);
    }

    public Path getArchivo() {
        return archivo;
    }

    private static String describir(RuntimeException e) {
        String mensaje = e.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            return e.getClass().getSimpleName();
        }
        // Los mensajes de validación terminan en punto; en la bitácora se ven mejor sin él.
        return mensaje.endsWith(".") ? mensaje.substring(0, mensaje.length() - 1) : mensaje;
    }
}
