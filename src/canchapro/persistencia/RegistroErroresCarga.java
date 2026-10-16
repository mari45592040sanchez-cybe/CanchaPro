package canchapro.persistencia;

import canchapro.util.FormatoFecha;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acumula los errores encontrados durante el arranque del sistema y los
 * guarda en un archivo de bitácora para poder revisarlos después.
 */
public class RegistroErroresCarga {

    public static final String ARCHIVO_LOG = "errores_carga.log";

    private final List<ErrorCarga> errores = new ArrayList<>();
    private final List<String> respaldos = new ArrayList<>();

    public void agregar(ErrorCarga error) {
        errores.add(error);
    }

    public void agregarTodos(List<ErrorCarga> nuevos) {
        errores.addAll(nuevos);
    }

    /** Registra la ruta de una copia del archivo original hecha antes de descartar líneas. */
    public void agregarRespaldo(Path respaldo) {
        respaldos.add(respaldo.toString());
    }

    public boolean hayErrores() {
        return !errores.isEmpty();
    }

    public List<ErrorCarga> getErrores() {
        return List.copyOf(errores);
    }

    public List<String> getRespaldos() {
        return List.copyOf(respaldos);
    }

    /**
     * Agrega al final de la bitácora los errores de esta carga, con fecha y
     * hora. Si no hubo errores no escribe nada.
     */
    public void escribirLog(Path carpetaDatos) {
        if (!hayErrores()) {
            return;
        }
        List<String> lineas = new ArrayList<>();
        lineas.add("=== Carga del " + FormatoFecha.vista(LocalDateTime.now()) + " — "
                + errores.size() + " error(es) ===");
        errores.forEach(e -> lineas.add(e.toString()));
        respaldos.forEach(r -> lineas.add("Copia del archivo original: " + r));
        lineas.add("");
        try {
            Files.createDirectories(carpetaDatos);
            Files.write(carpetaDatos.resolve(ARCHIVO_LOG), lineas, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // La bitácora es informativa: si no se puede escribir, el sistema sigue funcionando.
            System.err.println("No se pudo escribir " + ARCHIVO_LOG + ": " + e.getMessage());
        }
    }
}
