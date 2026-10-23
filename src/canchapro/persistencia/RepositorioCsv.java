package canchapro.persistencia;

import canchapro.util.Identificable;
import canchapro.util.excepciones.CanchaProException;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Base genérica para guardar una colección de entidades en un archivo CSV.
 * Mantiene los objetos en memoria en un arreglo y los escribe al archivo
 * cada vez que se llama a {@link #guardar()}.
 *
 * Cada repositorio concreto indica el formato de su archivo implementando
 * {@link #encabezado()}, {@link #cantidadCampos()}, {@link #desdeCsv(String[])},
 * {@link #aCsv(Identificable)} y {@link #crearArreglo(int)}.
 *
 * @param <T> tipo de entidad
 */
// POO - Herencia / Reutilización: clase abstracta que heredan ClienteRepositorio y UsuarioRepositorio;
// la lógica de cargar, guardar, agregar y buscar se escribe una sola vez y la reutilizan todos.
// POO - Polimorfismo: cada subclase implementa a su manera los métodos abstractos (formato de su archivo).
// POO - Responsabilidad: esta clase solo guarda y recupera datos; no valida reglas de negocio.
public abstract class RepositorioCsv<T extends Identificable> {

    private static final int CAPACIDAD_INICIAL = 10;

    // POO - Encapsulamiento: los atributos son privados; solo se modifican mediante los métodos públicos.
    private final Path archivo;
    // POO - Colecciones: arreglo de objetos + contador de posiciones ocupadas.
    // Si el arreglo se llena, se crea uno del doble de tamaño y se copian los elementos.
    private T[] datos;
    private int cantidad;
    private ErrorCarga[] erroresCarga = new ErrorCarga[CAPACIDAD_INICIAL];
    private int cantidadErrores;

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
     * Crea un arreglo vacío del tipo concreto, por ejemplo {@code new Cliente[tamanio]}.
     * Es necesario porque en Java no se puede escribir {@code new T[tamanio]}.
     */
    // POO - Polimorfismo: cada repositorio crea el arreglo de su propio tipo de entidad.
    protected abstract T[] crearArreglo(int tamanio);

    /**
     * Lee el archivo y reconstruye los objetos. Las líneas con errores se
     * omiten y quedan registradas en {@link #getErroresCarga()}; el resto
     * de la información se carga normalmente.
     */
    public void cargar() {
        datos = crearArreglo(CAPACIDAD_INICIAL);
        cantidad = 0;
        erroresCarga = new ErrorCarga[CAPACIDAD_INICIAL];
        cantidadErrores = 0;
        String nombre = archivo.getFileName().toString();
        ContenidoCsv contenido = ArchivoCsv.leer(archivo, encabezado());

        if (!contenido.encabezado().isEmpty() && !contenido.encabezado().equals(encabezado())) {
            agregarError(new ErrorCarga(nombre, 1, contenido.encabezado(),
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
                if (existe(objeto.getCodigo())) {
                    throw new IllegalArgumentException("código duplicado " + objeto.getCodigo());
                }
                agregarAlFinal(objeto);
            } catch (RuntimeException e) {
                // Cubre campos inválidos, fechas/números mal escritos, enums
                // desconocidos y referencias a registros inexistentes.
                agregarError(new ErrorCarga(nombre, linea.numero(), linea.texto(), describir(e)));
            }
        }
        despuesDeCargar();
    }

    /**
     * Punto de extensión: se ejecuta cada vez que termina una carga (también
     * al recargar tras un error de escritura). Las subclases lo usan para
     * reconstruir relaciones con otros repositorios.
     */
    // POO - Polimorfismo: las subclases pueden sobrescribir este método (@Override) si lo necesitan.
    protected void despuesDeCargar() {
    }

    /**
     * Escribe todos los objetos en el archivo. Si la escritura falla, se
     * vuelve a cargar el archivo para que la memoria no quede con cambios
     * que no se guardaron, y se relanza el error.
     */
    public void guardar() {
        String[] lineas = new String[cantidad];
        for (int i = 0; i < cantidad; i++) {
            lineas[i] = aCsv(datos[i]);
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
        if (existe(objeto.getCodigo())) {
            throw new ReglaNegocioException("Ya existe un registro con el código " + objeto.getCodigo());
        }
        agregarAlFinal(objeto);
    }

    public void actualizar(T objeto) {
        int indice = indiceDe(objeto.getCodigo());
        if (indice < 0) {
            throw new EntidadNoEncontradaException("No existe un registro con el código " + objeto.getCodigo());
        }
        datos[indice] = objeto;
    }

    public Optional<T> buscar(String codigo) {
        int indice = indiceDe(codigo);
        return indice < 0 ? Optional.empty() : Optional.of(datos[indice]);
    }

    public boolean existe(String codigo) {
        return indiceDe(codigo) >= 0;
    }

    /**
     * Copia de los objetos guardados, del tamaño exacto. Se devuelve una
     * copia para que nadie pueda modificar el arreglo interno desde afuera.
     */
    // POO - Encapsulamiento: se entrega una copia, no el arreglo interno.
    public T[] listar() {
        T[] copia = crearArreglo(cantidad);
        for (int i = 0; i < cantidad; i++) {
            copia[i] = datos[i];
        }
        return copia;
    }

    public int getCantidad() {
        return cantidad;
    }

    public boolean estaVacio() {
        return cantidad == 0;
    }

    /** Códigos de todos los registros (se usa para generar el siguiente código). */
    public String[] codigos() {
        String[] codigos = new String[cantidad];
        for (int i = 0; i < cantidad; i++) {
            codigos[i] = datos[i].getCodigo();
        }
        return codigos;
    }

    public ErrorCarga[] getErroresCarga() {
        ErrorCarga[] copia = new ErrorCarga[cantidadErrores];
        for (int i = 0; i < cantidadErrores; i++) {
            copia[i] = erroresCarga[i];
        }
        return copia;
    }

    public Path getArchivo() {
        return archivo;
    }

    // --- Manejo del arreglo ---

    /** Posición del objeto con ese código, o -1 si no existe (búsqueda secuencial). */
    private int indiceDe(String codigo) {
        for (int i = 0; i < cantidad; i++) {
            if (datos[i].getCodigo().equals(codigo)) {
                return i;
            }
        }
        return -1;
    }

    private void agregarAlFinal(T objeto) {
        if (datos == null) {
            datos = crearArreglo(CAPACIDAD_INICIAL);
        }
        if (cantidad == datos.length) {
            // Arreglo lleno: se crea uno del doble de tamaño y se copian los elementos.
            T[] mayor = crearArreglo(datos.length * 2);
            for (int i = 0; i < cantidad; i++) {
                mayor[i] = datos[i];
            }
            datos = mayor;
        }
        datos[cantidad] = objeto;
        cantidad++;
    }

    private void agregarError(ErrorCarga error) {
        if (cantidadErrores == erroresCarga.length) {
            ErrorCarga[] mayor = new ErrorCarga[erroresCarga.length * 2];
            for (int i = 0; i < cantidadErrores; i++) {
                mayor[i] = erroresCarga[i];
            }
            erroresCarga = mayor;
        }
        erroresCarga[cantidadErrores] = error;
        cantidadErrores++;
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
