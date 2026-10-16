package canchapro.configuracion;

import canchapro.persistencia.RegistroErroresCarga;
import canchapro.util.excepciones.CanchaProException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Cambios de configuración sin recompilar y recarga de los archivos de datos.
 *
 * Patrón Observador: las pantallas se registran como oyentes para
 * enterarse cuando cambia la configuración (por ejemplo, para mostrar u
 * ocultar la Tienda) o cuando se vuelven a leer los archivos.
 */
public class ConfiguracionServicio {

    private static final int LINEAS_BITACORA = 300;

    private final Configuracion configuracion;
    private final ConfiguracionRepositorio repositorio;
    private final Path carpetaDatos;
    private final Supplier<RegistroErroresCarga> recargador;
    private final List<Runnable> oyentesCambios = new ArrayList<>();
    private final List<Runnable> oyentesRecarga = new ArrayList<>();

    /**
     * @param recargador vuelve a leer todos los archivos y devuelve los errores encontrados
     */
    public ConfiguracionServicio(Configuracion configuracion, ConfiguracionRepositorio repositorio,
            Path carpetaDatos, Supplier<RegistroErroresCarga> recargador) {
        this.configuracion = configuracion;
        this.repositorio = repositorio;
        this.carpetaDatos = carpetaDatos;
        this.recargador = recargador;
    }

    public Configuracion getConfiguracion() {
        return configuracion;
    }

    public Path getCarpetaDatos() {
        return carpetaDatos.toAbsolutePath();
    }

    public Path getArchivoConfiguracion() {
        return repositorio.getArchivo().toAbsolutePath();
    }

    // --- Observadores ---

    public void agregarOyenteCambios(Runnable oyente) {
        oyentesCambios.add(oyente);
    }

    public void agregarOyenteRecarga(Runnable oyente) {
        oyentesRecarga.add(oyente);
    }

    public void quitarOyente(Runnable oyente) {
        oyentesCambios.remove(oyente);
        oyentesRecarga.remove(oyente);
    }

    // --- Operaciones ---

    /**
     * Valida y guarda la configuración en canchapro.properties. Los cambios
     * aplican de inmediato (no hace falta reiniciar).
     */
    public void guardar(String nombreNegocio, boolean moduloTienda, int anticipoMinimoPorcentaje) {
        // Se valida con una copia para no dejar la configuración a medias.
        Configuracion propuesta = new Configuracion();
        propuesta.setNombreNegocio(nombreNegocio);
        propuesta.setModuloTienda(moduloTienda);
        propuesta.setAnticipoMinimoPorcentaje(anticipoMinimoPorcentaje);
        propuesta.setRutaDatos(configuracion.getRutaDatos());
        repositorio.guardar(propuesta);

        configuracion.setNombreNegocio(propuesta.getNombreNegocio());
        configuracion.setModuloTienda(propuesta.isModuloTienda());
        configuracion.setAnticipoMinimoPorcentaje(propuesta.getAnticipoMinimoPorcentaje());
        notificar(oyentesCambios);
    }

    /**
     * Vuelve a leer todos los archivos de datos (por ejemplo, después de
     * copiar datos de prueba) y avisa a las pantallas.
     *
     * @return errores encontrados durante la lectura
     */
    public RegistroErroresCarga recargarArchivos() {
        RegistroErroresCarga registro = recargador.get();
        notificar(oyentesRecarga);
        return registro;
    }

    /** Últimas líneas de la bitácora de errores de carga. */
    public String leerBitacora() {
        Path archivo = carpetaDatos.resolve(RegistroErroresCarga.ARCHIVO_LOG);
        if (Files.notExists(archivo)) {
            return "(No se han registrado errores de carga.)";
        }
        try {
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            int desde = Math.max(0, lineas.size() - LINEAS_BITACORA);
            return String.join("\n", lineas.subList(desde, lineas.size()));
        } catch (IOException e) {
            throw new CanchaProException("No se pudo leer " + archivo.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private static void notificar(List<Runnable> oyentes) {
        // Copia: un oyente podría quitarse a sí mismo mientras se notifica.
        new ArrayList<>(oyentes).forEach(Runnable::run);
    }
}
