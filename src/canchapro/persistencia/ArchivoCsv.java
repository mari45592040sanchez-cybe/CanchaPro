package canchapro.persistencia;

import canchapro.util.excepciones.CanchaProException;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Lectura y escritura de archivos CSV separados por ';' en UTF-8.
 * La primera línea de cada archivo es el encabezado.
 */
public final class ArchivoCsv {

    public static final String SEPARADOR = ";";
    private static final char BOM = '﻿';
    private static final DateTimeFormatter SELLO_RESPALDO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ArchivoCsv() {
    }

    /**
     * Lee el archivo completo. Si no existe, lo crea únicamente con el
     * encabezado y devuelve un contenido vacío.
     */
    public static ContenidoCsv leer(Path archivo, String encabezadoEsperado) {
        try {
            if (Files.notExists(archivo)) {
                escribir(archivo, encabezadoEsperado, List.of());
                return new ContenidoCsv(encabezadoEsperado, List.of());
            }
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            if (lineas.isEmpty()) {
                return new ContenidoCsv("", List.of());
            }
            String encabezado = quitarBom(lineas.get(0)).trim();
            List<LineaCsv> datos = new ArrayList<>();
            // La línea 1 (índice 0) es el encabezado.
            for (int i = 1; i < lineas.size(); i++) {
                String texto = lineas.get(i);
                if (!texto.isBlank()) {
                    datos.add(new LineaCsv(i + 1, texto, texto.split(SEPARADOR, -1)));
                }
            }
            return new ContenidoCsv(encabezado, datos);
        } catch (CharacterCodingException e) {
            throw new CanchaProException("El archivo " + archivo.getFileName()
                    + " no está guardado en UTF-8. Ábralo y guárdelo con codificación UTF-8.", e);
        } catch (IOException e) {
            throw new CanchaProException(mensajeError("leer", archivo, e), e);
        }
    }

    /**
     * Escritura segura: primero se escribe un archivo temporal y luego se
     * reemplaza el original. Si el programa se interrumpe a mitad de la
     * escritura, el archivo original queda intacto.
     */
    public static void escribir(Path archivo, String encabezado, List<String> lineas) {
        List<String> contenido = new ArrayList<>(lineas.size() + 1);
        contenido.add(encabezado);
        contenido.addAll(lineas);
        escribirLineas(archivo, contenido);
    }

    /**
     * Escritura segura de cualquier archivo de texto en UTF-8 (también se
     * usa para el archivo de configuración).
     */
    public static void escribirLineas(Path archivo, List<String> contenido) {
        try {
            Path carpeta = archivo.toAbsolutePath().getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            Files.write(temporal, contenido, StandardCharsets.UTF_8);
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new CanchaProException(mensajeError("guardar", archivo, e), e);
        }
    }

    /**
     * Copia el archivo a la carpeta de respaldos con fecha y hora en el
     * nombre, por ejemplo: respaldos/usuarios_20261008_183000.csv
     *
     * @return ruta de la copia creada
     */
    public static Path respaldar(Path archivo, Path carpetaRespaldos) {
        try {
            Files.createDirectories(carpetaRespaldos);
            String nombre = archivo.getFileName().toString();
            int punto = nombre.lastIndexOf('.');
            String base = punto > 0 ? nombre.substring(0, punto) : nombre;
            String extension = punto > 0 ? nombre.substring(punto) : "";
            Path destino = carpetaRespaldos.resolve(
                    base + "_" + LocalDateTime.now().format(SELLO_RESPALDO) + extension);
            return Files.copy(archivo, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CanchaProException(mensajeError("respaldar", archivo, e), e);
        }
    }

    /** Quita la marca BOM que agregan algunos editores (Excel, Bloc de notas). */
    private static String quitarBom(String texto) {
        return !texto.isEmpty() && texto.charAt(0) == BOM ? texto.substring(1) : texto;
    }

    /**
     * En Windows, un archivo abierto en Excel queda bloqueado y no se puede
     * reemplazar; se explica eso al usuario en lugar del error técnico.
     */
    private static String mensajeError(String accion, Path archivo, IOException e) {
        String nombre = archivo.getFileName().toString();
        if (e instanceof FileSystemException && !(e instanceof NoSuchFileException)) {
            return "No se pudo " + accion + " el archivo " + nombre
                    + ". Es posible que esté abierto en otro programa (por ejemplo Excel). "
                    + "Ciérrelo e intente de nuevo.";
        }
        return "No se pudo " + accion + " el archivo " + nombre + ": " + e.getMessage();
    }
}
