package canchapro.configuracion;

import canchapro.persistencia.RegistroErroresCarga;
import canchapro.util.excepciones.CanchaProException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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

    // POO - Relaciones (composición): el servicio TIENE la configuración y su repositorio.
    private final Configuracion configuracion;
    private final ConfiguracionRepositorio repositorio;
    private final Path carpetaDatos;
    private final Supplier<RegistroErroresCarga> recargador;
    // POO - Reutilización: la misma clase Oyentes sirve para los dos grupos de oyentes.
    private final Oyentes oyentesCambios = new Oyentes();
    private final Oyentes oyentesRecarga = new Oyentes();

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
        oyentesCambios.agregar(oyente);
    }

    public void agregarOyenteRecarga(Runnable oyente) {
        oyentesRecarga.agregar(oyente);
    }

    public void quitarOyente(Runnable oyente) {
        oyentesCambios.quitar(oyente);
        oyentesRecarga.quitar(oyente);
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
        oyentesCambios.notificar();
    }

    /**
     * Vuelve a leer todos los archivos de datos (por ejemplo, después de
     * copiar datos de prueba) y avisa a las pantallas.
     *
     * @return errores encontrados durante la lectura
     */
    public RegistroErroresCarga recargarArchivos() {
        RegistroErroresCarga registro = recargador.get();
        oyentesRecarga.notificar();
        return registro;
    }

    /** Últimas líneas de la bitácora de errores de carga. */
    public String leerBitacora() {
        Path archivo = carpetaDatos.resolve(RegistroErroresCarga.ARCHIVO_LOG);
        if (Files.notExists(archivo)) {
            return "(No se han registrado errores de carga.)";
        }
        try {
            // POO - Colecciones: las líneas del archivo en un arreglo de String.
            String[] lineas = Files.readString(archivo, StandardCharsets.UTF_8).split("\\r?\\n");
            int desde = Math.max(0, lineas.length - LINEAS_BITACORA);
            return String.join("\n", Arrays.copyOfRange(lineas, desde, lineas.length));
        } catch (IOException e) {
            throw new CanchaProException("No se pudo leer " + archivo.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Grupo de oyentes guardado en un arreglo con su contador.
     */
    // POO - Encapsulamiento: el arreglo es privado; solo se agrega, se quita o se notifica.
    // POO - Colecciones: arreglo de objetos Runnable; si se llena, se crea uno del doble de tamaño.
    private static final class Oyentes {

        private Runnable[] lista = new Runnable[4];
        private int cantidad;

        void agregar(Runnable oyente) {
            if (cantidad == lista.length) {
                lista = Arrays.copyOf(lista, lista.length * 2);
            }
            lista[cantidad] = oyente;
            cantidad++;
        }

        /** Quita el oyente y corre una posición hacia arriba los siguientes. */
        void quitar(Runnable oyente) {
            for (int i = 0; i < cantidad; i++) {
                if (lista[i] == oyente) {
                    for (int j = i; j < cantidad - 1; j++) {
                        lista[j] = lista[j + 1];
                    }
                    cantidad--;
                    lista[cantidad] = null;
                    return;
                }
            }
        }

        void notificar() {
            // Copia: un oyente podría quitarse a sí mismo mientras se notifica.
            Runnable[] copia = Arrays.copyOf(lista, cantidad);
            for (Runnable oyente : copia) {
                oyente.run();
            }
        }
    }
}
