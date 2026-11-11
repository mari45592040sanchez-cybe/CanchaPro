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

// POO - Responsabilidad / Reutilización: única clase que lee y escribe archivos;
// todos los repositorios y la configuración la reutilizan.
public final class ArchivoCsv {

    public static final String SEPARADOR = ";";
    private static final char BOM = '﻿';
    private static final DateTimeFormatter SELLO_RESPALDO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ArchivoCsv() {
    }

    public static ContenidoCsv leer(Path archivo, String encabezadoEsperado) {
        try {
            if (Files.notExists(archivo)) {
                escribir(archivo, encabezadoEsperado, new String[0]);
                return new ContenidoCsv(encabezadoEsperado, new LineaCsv[0]);
            }
            String texto = Files.readString(archivo, StandardCharsets.UTF_8);
            if (texto.isEmpty()) {
                return new ContenidoCsv("", new LineaCsv[0]);
            }
            String[] lineas = texto.split("\\r?\\n");
            String encabezado = quitarBom(lineas[0]).trim();

            // Primera pasada: contar las líneas con datos para crear el arreglo del tamaño exacto.
            int conDatos = 0;
            for (int i = 1; i < lineas.length; i++) {
                if (!lineas[i].isBlank()) {
                    conDatos++;
                }
            }
            // Segunda pasada: llenar el arreglo. La línea 1 (índice 0) es el encabezado.
            LineaCsv[] datos = new LineaCsv[conDatos];
            int posicion = 0;
            for (int i = 1; i < lineas.length; i++) {
                if (!lineas[i].isBlank()) {
                    datos[posicion] = new LineaCsv(i + 1, lineas[i], lineas[i].split(SEPARADOR, -1));
                    posicion++;
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

    public static void escribir(Path archivo, String encabezado, String[] lineas) {
        String[] contenido = new String[lineas.length + 1];
        contenido[0] = encabezado;
        for (int i = 0; i < lineas.length; i++) {
            contenido[i + 1] = lineas[i];
        }
        escribirLineas(archivo, contenido);
    }

    public static void escribirLineas(Path archivo, String[] contenido) {
        StringBuilder texto = new StringBuilder();
        for (String linea : contenido) {
            texto.append(linea).append(System.lineSeparator());
        }
        try {
            Path carpeta = archivo.toAbsolutePath().getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            Files.writeString(temporal, texto, StandardCharsets.UTF_8);
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new CanchaProException(mensajeError("guardar", archivo, e), e);
        }
    }

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

    private static String quitarBom(String texto) {
        return !texto.isEmpty() && texto.charAt(0) == BOM ? texto.substring(1) : texto;
    }

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
