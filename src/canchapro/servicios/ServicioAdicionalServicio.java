package canchapro.servicios;

import canchapro.util.GeneradorCodigo;
import canchapro.util.Texto;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Reglas de negocio del catálogo de servicios adicionales.
 */
// POO - Responsabilidad: reglas del catálogo de servicios (nombre único, activar/desactivar).
public class ServicioAdicionalServicio {

    private static final String PREFIJO_CODIGO = "SRV-";

    // POO - Relaciones (composición): el servicio TIENE un repositorio, que recibe por el constructor.
    private final ServicioAdicionalRepositorio repositorio;

    public ServicioAdicionalServicio(ServicioAdicionalRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Primer arranque: crea un catálogo de ejemplo.
     *
     * @return true si se crearon
     */
    public boolean asegurarDatosIniciales() {
        if (!repositorio.estaVacio()) {
            return false;
        }
        agregarInicial("SRV-001", "Balón", "Balón profesional", "25", TipoCobro.POR_RESERVA);
        agregarInicial("SRV-002", "Chalecos", "Juego de chalecos de colores", "20", TipoCobro.POR_RESERVA);
        agregarInicial("SRV-003", "Árbitro", "Árbitro para el partido", "75", TipoCobro.POR_HORA);
        agregarInicial("SRV-004", "Iluminación", "Iluminación nocturna de la cancha", "50", TipoCobro.POR_HORA);
        agregarInicial("SRV-005", "Agua", "Botella de agua para jugadores", "10", TipoCobro.POR_RESERVA);
        repositorio.guardar();
        return true;
    }

    private void agregarInicial(String codigo, String nombre, String descripcion, String precio, TipoCobro tipo) {
        repositorio.agregar(new ServicioAdicional(codigo, nombre, descripcion, new BigDecimal(precio), tipo, true));
    }

    /** Todos los servicios ordenados por código. */
    public ServicioAdicional[] listar() {
        // POO - Colecciones: el repositorio entrega una copia del arreglo; se ordena sin afectar el original.
        ServicioAdicional[] servicios = repositorio.listar();
        Arrays.sort(servicios, Comparator.comparing(ServicioAdicional::getCodigo));
        return servicios;
    }

    /** Servicios que se pueden agregar a una reserva nueva. */
    public ServicioAdicional[] listarActivos() {
        ServicioAdicional[] todos = listar();
        ServicioAdicional[] activos = new ServicioAdicional[todos.length];
        int cantidad = 0;
        for (ServicioAdicional s : todos) {
            if (s.isActivo()) {
                activos[cantidad] = s;
                cantidad++;
            }
        }
        return Arrays.copyOf(activos, cantidad);
    }

    public ServicioAdicional obtener(String codigo) {
        return repositorio.buscar(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el servicio " + codigo));
    }

    public ServicioAdicional registrar(String nombre, String descripcion, BigDecimal precio, TipoCobro tipoCobro) {
        validarNombreDisponible(nombre, null);
        String codigo = GeneradorCodigo.siguiente(PREFIJO_CODIGO, 3, repositorio.codigos());
        ServicioAdicional servicio = new ServicioAdicional(codigo, nombre, descripcion, precio, tipoCobro, true);
        repositorio.agregar(servicio);
        repositorio.guardar();
        return servicio;
    }

    /**
     * Cambiar el precio no afecta reservas ya creadas: cada reserva guarda
     * el precio unitario vigente al momento de crearse.
     */
    public ServicioAdicional actualizar(String codigo, String nombre, String descripcion, BigDecimal precio,
            TipoCobro tipoCobro) {
        ServicioAdicional servicio = obtener(codigo);
        validarNombreDisponible(nombre, codigo);
        servicio.actualizarDatos(nombre, descripcion, precio, tipoCobro);
        repositorio.actualizar(servicio);
        repositorio.guardar();
        return servicio;
    }

    /**
     * Un servicio inactivo no aparece al crear reservas, pero las reservas
     * antiguas que lo usaron lo conservan.
     */
    public void cambiarEstado(String codigo, boolean activo) {
        ServicioAdicional servicio = obtener(codigo);
        if (servicio.isActivo() == activo) {
            return;
        }
        servicio.setActivo(activo);
        repositorio.actualizar(servicio);
        repositorio.guardar();
    }

    private void validarNombreDisponible(String nombre, String codigoPropio) {
        String buscado = Texto.normalizar(nombre);
        for (ServicioAdicional s : repositorio.listar()) {
            if (!s.getCodigo().equals(codigoPropio) && Texto.normalizar(s.getNombre()).equals(buscado)) {
                throw new ReglaNegocioException("Ya existe el servicio " + s.getCodigo() + " con el nombre " + s.getNombre() + ".");
            }
        }
    }
}
