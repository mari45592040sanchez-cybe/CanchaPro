package canchapro.configuracion;

import canchapro.persistencia.RegistroErroresCarga;
import canchapro.util.excepciones.CanchaProException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;

public class ConfiguracionServicio {

    private static final int LINEAS_BITACORA = 300;
    // POO - Atributo constante: carné (código fijo) que se pide para habilitar o deshabilitar el módulo Tienda.
    private static final String CARNE_MODULO_TIENDA = "3623";

    // POO - Relaciones (composición): el servicio TIENE la configuración y su repositorio.
    private final Configuracion configuracion;
    private final ConfiguracionRepositorio repositorio;
    private final Path carpetaDatos;
    private final Supplier<RegistroErroresCarga> recargador;
    // POO - Reutilización: la misma clase Oyentes sirve para los dos grupos de oyentes.
    private final Oyentes oyentesCambios = new Oyentes();
    private final Oyentes oyentesRecarga = new Oyentes();

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

    // POO - Método: compara el carné ingresado con el código fijo; si no coincide lanza una excepción.
    public void validarCarneTienda(String carne) {
        if (carne == null || !CARNE_MODULO_TIENDA.equals(carne.trim())) {
            throw new ReglaNegocioException("Código inválido. No se cambió el módulo Tienda / Cafetería.");
        }
    }

    public void guardar(String nombreNegocio, boolean moduloTienda, int anticipoMinimoPorcentaje) {
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

    public RegistroErroresCarga recargarArchivos() {
        RegistroErroresCarga registro = recargador.get();
        oyentesRecarga.notificar();
        return registro;
    }

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
            Runnable[] copia = Arrays.copyOf(lista, cantidad);
            for (Runnable oyente : copia) {
                oyente.run();
            }
        }
    }
}
