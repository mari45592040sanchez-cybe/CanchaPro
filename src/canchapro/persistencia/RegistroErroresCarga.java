package canchapro.persistencia;

import canchapro.util.FormatoFecha;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * Acumula los errores encontrados durante el arranque del sistema y los
 * guarda en un archivo de bitácora para poder revisarlos después.
 */
public class RegistroErroresCarga {

    public static final String ARCHIVO_LOG = "errores_carga.log";

    // POO - Encapsulamiento / Colecciones: arreglos privados con su contador;
    // desde afuera solo se agregan elementos o se obtiene una copia.
    private ErrorCarga[] errores = new ErrorCarga[10];
    private int cantidadErrores;
    private String[] respaldos = new String[10];
    private int cantidadRespaldos;

    public void agregar(ErrorCarga error) {
        if (cantidadErrores == errores.length) {
            ErrorCarga[] mayor = new ErrorCarga[errores.length * 2];
            for (int i = 0; i < cantidadErrores; i++) {
                mayor[i] = errores[i];
            }
            errores = mayor;
        }
        errores[cantidadErrores] = error;
        cantidadErrores++;
    }

    public void agregarTodos(ErrorCarga[] nuevos) {
        for (ErrorCarga error : nuevos) {
            agregar(error);
        }
    }

    /** Registra la ruta de una copia del archivo original hecha antes de descartar líneas. */
    public void agregarRespaldo(Path respaldo) {
        if (cantidadRespaldos == respaldos.length) {
            String[] mayor = new String[respaldos.length * 2];
            for (int i = 0; i < cantidadRespaldos; i++) {
                mayor[i] = respaldos[i];
            }
            respaldos = mayor;
        }
        respaldos[cantidadRespaldos] = respaldo.toString();
        cantidadRespaldos++;
    }

    public boolean hayErrores() {
        return cantidadErrores > 0;
    }

    public ErrorCarga[] getErrores() {
        ErrorCarga[] copia = new ErrorCarga[cantidadErrores];
        for (int i = 0; i < cantidadErrores; i++) {
            copia[i] = errores[i];
        }
        return copia;
    }

    public String[] getRespaldos() {
        String[] copia = new String[cantidadRespaldos];
        for (int i = 0; i < cantidadRespaldos; i++) {
            copia[i] = respaldos[i];
        }
        return copia;
    }

    /**
     * Agrega al final de la bitácora los errores de esta carga, con fecha y
     * hora. Si no hubo errores no escribe nada.
     */
    public void escribirLog(Path carpetaDatos) {
        if (!hayErrores()) {
            return;
        }
        String salto = System.lineSeparator();
        StringBuilder texto = new StringBuilder();
        texto.append("=== Carga del ").append(FormatoFecha.vista(LocalDateTime.now())).append(" — ")
                .append(cantidadErrores).append(" error(es) ===").append(salto);
        for (int i = 0; i < cantidadErrores; i++) {
            texto.append(errores[i]).append(salto);
        }
        for (int i = 0; i < cantidadRespaldos; i++) {
            texto.append("Copia del archivo original: ").append(respaldos[i]).append(salto);
        }
        texto.append(salto);
        try {
            Files.createDirectories(carpetaDatos);
            Files.writeString(carpetaDatos.resolve(ARCHIVO_LOG), texto, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // La bitácora es informativa: si no se puede escribir, el sistema sigue funcionando.
            System.err.println("No se pudo escribir " + ARCHIVO_LOG + ": " + e.getMessage());
        }
    }
}
