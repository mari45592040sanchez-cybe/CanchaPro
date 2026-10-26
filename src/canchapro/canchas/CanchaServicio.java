package canchapro.canchas;

import canchapro.util.GeneradorCodigo;
import canchapro.util.Texto;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Reglas de negocio de las canchas.
 */
// POO - Responsabilidad: reglas de negocio de canchas (nombre único, datos iniciales, estados).
public class CanchaServicio {

    private static final String PREFIJO_CODIGO = "C";
    private static final LocalTime APERTURA_DEFECTO = LocalTime.of(8, 0);
    private static final LocalTime CIERRE_DEFECTO = LocalTime.of(23, 0);

    // POO - Relaciones (composición): el servicio TIENE un repositorio, que recibe por el constructor.
    private final CanchaRepositorio repositorio;

    public CanchaServicio(CanchaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Primer arranque: si no hay canchas, crea las tres del caso de estudio
     * del Complejo Deportivo "La 10".
     *
     * @return true si se crearon
     */
    public boolean asegurarDatosIniciales() {
        if (!repositorio.estaVacio()) {
            return false;
        }
        repositorio.agregar(new Cancha("C01", "La Bombonera", TipoCancha.FUTBOL_5, 10,
                EstadoCancha.DISPONIBLE, APERTURA_DEFECTO, CIERRE_DEFECTO, "Grama sintética"));
        repositorio.agregar(new Cancha("C02", "El Maracaná", TipoCancha.FUTBOL_5, 10,
                EstadoCancha.DISPONIBLE, APERTURA_DEFECTO, CIERRE_DEFECTO, "Grama sintética"));
        repositorio.agregar(new Cancha("C03", "El Monumental", TipoCancha.FUTBOL_7, 14,
                EstadoCancha.DISPONIBLE, APERTURA_DEFECTO, CIERRE_DEFECTO, "Grama sintética"));
        repositorio.guardar();
        return true;
    }

    /** Todas las canchas ordenadas por código. */
    public Cancha[] listar() {
        // POO - Colecciones: el repositorio entrega una copia del arreglo; se ordena sin afectar el original.
        Cancha[] canchas = repositorio.listar();
        Arrays.sort(canchas, Comparator.comparing(Cancha::getCodigo));
        return canchas;
    }

    /**
     * @param tipo tipo buscado, o null para todas
     */
    public Cancha[] listarPorTipo(TipoCancha tipo) {
        Cancha[] todas = listar();
        Cancha[] encontradas = new Cancha[todas.length];
        int cantidad = 0;
        for (Cancha c : todas) {
            if (tipo == null || c.getTipo() == tipo) {
                encontradas[cantidad] = c;
                cantidad++;
            }
        }
        return Arrays.copyOf(encontradas, cantidad);
    }

    /** Canchas que pueden recibir reservas (estado DISPONIBLE). */
    public Cancha[] listarReservables() {
        Cancha[] todas = listar();
        Cancha[] encontradas = new Cancha[todas.length];
        int cantidad = 0;
        for (Cancha c : todas) {
            if (c.permiteReservas()) {
                encontradas[cantidad] = c;
                cantidad++;
            }
        }
        return Arrays.copyOf(encontradas, cantidad);
    }

    public Cancha obtener(String codigo) {
        return repositorio.buscar(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe la cancha " + codigo));
    }

    /**
     * Registra una cancha nueva en estado DISPONIBLE con código C01, C02, …
     */
    public Cancha registrar(String nombre, TipoCancha tipo, int capacidad,
            LocalTime apertura, LocalTime cierre, String descripcion) {
        validarNombreDisponible(nombre, null);
        String codigo = GeneradorCodigo.siguiente(PREFIJO_CODIGO, 2, repositorio.codigos());
        Cancha cancha = new Cancha(codigo, nombre, tipo, capacidad, EstadoCancha.DISPONIBLE,
                apertura, cierre, descripcion);
        repositorio.agregar(cancha);
        repositorio.guardar();
        return cancha;
    }

    /**
     * Actualiza los datos de la cancha, incluido su estado. Una cancha en
     * MANTENIMIENTO o INACTIVA deja de aparecer como disponible de inmediato.
     */
    public Cancha actualizar(String codigo, String nombre, TipoCancha tipo, int capacidad,
            EstadoCancha estado, LocalTime apertura, LocalTime cierre, String descripcion) {
        Cancha cancha = obtener(codigo);
        validarNombreDisponible(nombre, codigo);
        cancha.actualizarDatos(nombre, tipo, capacidad, estado, apertura, cierre, descripcion);
        repositorio.actualizar(cancha);
        repositorio.guardar();
        return cancha;
    }

    /** No puede haber dos canchas con el mismo nombre (sin importar mayúsculas ni tildes). */
    private void validarNombreDisponible(String nombre, String codigoPropio) {
        String buscado = Texto.normalizar(nombre);
        for (Cancha c : repositorio.listar()) {
            if (!c.getCodigo().equals(codigoPropio) && Texto.normalizar(c.getNombre()).equals(buscado)) {
                throw new ReglaNegocioException("Ya existe la cancha " + c.getCodigo() + " con el nombre " + c.getNombre() + ".");
            }
        }
    }
}
