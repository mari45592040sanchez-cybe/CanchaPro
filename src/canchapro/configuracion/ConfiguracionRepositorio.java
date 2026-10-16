package canchapro.configuracion;

import canchapro.persistencia.ArchivoCsv;
import canchapro.persistencia.CamposCsv;
import canchapro.persistencia.ErrorCarga;
import canchapro.persistencia.RegistroErroresCarga;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Lee y guarda la configuración en un archivo .properties (UTF-8).
 */
public class ConfiguracionRepositorio {

    public static final String CLAVE_MODULO_TIENDA = "modulo_tienda";
    public static final String CLAVE_RUTA_DATOS = "ruta_datos";
    public static final String CLAVE_ANTICIPO_MINIMO = "anticipo_minimo_porcentaje";
    public static final String CLAVE_NOMBRE_NEGOCIO = "nombre_negocio";

    private final Path archivo;

    public ConfiguracionRepositorio(Path archivo) {
        this.archivo = archivo;
    }

    /**
     * Lee la configuración. Si el archivo no existe se crea con los valores
     * por defecto; si falta alguna clave se agrega. Un valor inválido no
     * detiene el sistema: se usa el valor por defecto y se registra el error.
     */
    public Configuracion cargar(RegistroErroresCarga registro) {
        Configuracion configuracion = new Configuracion();
        if (Files.notExists(archivo)) {
            guardar(configuracion);
            return configuracion;
        }

        Properties propiedades = new Properties();
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        } catch (IOException | IllegalArgumentException e) {
            registro.agregar(new ErrorCarga(nombreArchivo(), 0, "",
                    "no se pudo leer (" + e.getMessage() + "). Se usarán los valores por defecto"));
            return configuracion;
        }

        boolean faltanClaves = false;
        faltanClaves |= aplicar(propiedades, CLAVE_MODULO_TIENDA, registro,
                v -> configuracion.setModuloTienda(CamposCsv.booleano(v, CLAVE_MODULO_TIENDA)));
        faltanClaves |= aplicar(propiedades, CLAVE_RUTA_DATOS, registro, configuracion::setRutaDatos);
        faltanClaves |= aplicar(propiedades, CLAVE_ANTICIPO_MINIMO, registro,
                v -> configuracion.setAnticipoMinimoPorcentaje(CamposCsv.entero(v, CLAVE_ANTICIPO_MINIMO)));
        faltanClaves |= aplicar(propiedades, CLAVE_NOMBRE_NEGOCIO, registro, configuracion::setNombreNegocio);

        if (faltanClaves) {
            guardar(configuracion);
        }
        return configuracion;
    }

    public void guardar(Configuracion c) {
        ArchivoCsv.escribirLineas(archivo, List.of(
                "# Configuración de CanchaPro",
                "# Se puede editar con el programa cerrado. Los cambios aplican al volver a iniciar.",
                "",
                "# true = muestra el módulo Tienda/Cafetería, false = lo oculta",
                CLAVE_MODULO_TIENDA + "=" + c.isModuloTienda(),
                "",
                "# Carpeta de los archivos de datos (relativa a la carpeta del programa). Use / en las rutas.",
                CLAVE_RUTA_DATOS + "=" + escapar(c.getRutaDatos()),
                "",
                "# Porcentaje del total que debe pagarse para que una reserva quede CONFIRMADA (0-100)",
                CLAVE_ANTICIPO_MINIMO + "=" + c.getAnticipoMinimoPorcentaje(),
                "",
                "# Nombre que aparece en las pantallas y comprobantes",
                CLAVE_NOMBRE_NEGOCIO + "=" + escapar(c.getNombreNegocio())
        ));
    }

    public Path getArchivo() {
        return archivo;
    }

    /**
     * Aplica el valor de una clave.
     *
     * @return true si la clave no existe en el archivo
     */
    private boolean aplicar(Properties propiedades, String clave, RegistroErroresCarga registro,
            Consumer<String> asignar) {
        String valor = propiedades.getProperty(clave);
        if (valor == null) {
            return true;
        }
        try {
            asignar.accept(valor.trim());
        } catch (RuntimeException e) {
            registro.agregar(new ErrorCarga(nombreArchivo(), 0, clave + "=" + valor,
                    e.getMessage() + " (se usará el valor por defecto)"));
        }
        return false;
    }

    /** En archivos .properties la barra invertida es un carácter especial. */
    private static String escapar(String valor) {
        return valor.replace("\\", "\\\\");
    }

    private String nombreArchivo() {
        return archivo.getFileName().toString();
    }
}
