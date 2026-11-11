package canchapro.reportes;

import canchapro.persistencia.ArchivoCsv;
import canchapro.reportes.Reporte.Filtro;
import canchapro.util.Texto;
import canchapro.util.excepciones.DatosInvalidosException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

// POO - Responsabilidad: registra los reportes, valida los filtros y exporta a CSV.
public class ReporteServicio {

    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final String BOM = "﻿";

    // POO - Colecciones / Polimorfismo: arreglo del tipo Reporte (interfaz); cada posición guarda
    // un reporte distinto y todos se tratan igual.
    private Reporte[] reportes = new Reporte[10];
    private int cantidad;
    private final Path carpetaExportacion;
    private final Clock reloj;

    public ReporteServicio(Path carpetaExportacion, Clock reloj) {
        this.carpetaExportacion = carpetaExportacion;
        this.reloj = reloj;
    }

    /** Agrega un reporte al arreglo (los módulos opcionales pueden sumar los suyos). */
    public void registrar(Reporte reporte) {
        if (cantidad == reportes.length) {
            reportes = Arrays.copyOf(reportes, reportes.length * 2);
        }
        reportes[cantidad] = reporte;
        cantidad++;
    }

    public Reporte[] listar() {
        Reporte[] disponibles = new Reporte[cantidad];
        int n = 0;
        for (int i = 0; i < cantidad; i++) {
            // POO - Polimorfismo: estaDisponible() puede estar sobrescrito (reporte de la Tienda).
            if (reportes[i].estaDisponible()) {
                disponibles[n] = reportes[i];
                n++;
            }
        }
        return Arrays.copyOf(disponibles, n);
    }

    public ResultadoReporte generar(Reporte reporte, FiltroReporte f) {
        boolean usaFechas = reporte.usaFiltro(Filtro.FECHAS);
        if (usaFechas) {
            if (f.desde() == null || f.hasta() == null) {
                throw new DatosInvalidosException("Indique la fecha inicial y final del reporte.");
            }
            if (f.hasta().isBefore(f.desde())) {
                throw new DatosInvalidosException("La fecha final no puede ser anterior a la fecha inicial.");
            }
        }
        String texto = f.textoCliente() == null || f.textoCliente().isBlank() ? null : f.textoCliente().trim();
        FiltroReporte efectivo = new FiltroReporte(
                usaFechas ? f.desde() : null,
                usaFechas ? f.hasta() : null,
                reporte.usaFiltro(Filtro.CANCHA) ? f.codigoCancha() : null,
                reporte.usaFiltro(Filtro.ESTADO) ? f.estado() : null,
                reporte.usaFiltro(Filtro.CLIENTE) ? texto : null);
        return reporte.generar(efectivo);
    }

    public Path exportarCsv(ResultadoReporte resultado) {
        String[] lineas = new String[5 + resultado.getCantidadFilas() + resultado.getCantidadTotales()];
        int n = 0;
        lineas[n++] = BOM + limpiar(resultado.getTitulo());
        lineas[n++] = limpiar(resultado.getSubtitulo());
        lineas[n++] = "";
        lineas[n++] = unir(resultado.getColumnas());
        for (String[] fila : resultado.getFilas()) {
            lineas[n++] = unir(fila);
        }
        lineas[n++] = "";
        for (int i = 0; i < resultado.getCantidadTotales(); i++) {
            lineas[n++] = limpiar(resultado.getConceptoTotal(i)) + ";" + limpiar(resultado.getValorTotal(i));
        }
        String nombre = Texto.normalizar(resultado.getTitulo()).replaceAll("[^a-z0-9]+", "_")
                + "_" + LocalDateTime.now(reloj).format(SELLO) + ".csv";
        Path archivo = carpetaExportacion.resolve(nombre);
        ArchivoCsv.escribirLineas(archivo, lineas);
        return archivo.toAbsolutePath();
    }

    private static String unir(String[] valores) {
        String[] limpios = new String[valores.length];
        for (int i = 0; i < valores.length; i++) {
            limpios[i] = limpiar(valores[i]);
        }
        return String.join(";", limpios);
    }

    private static String limpiar(String valor) {
        return valor == null ? "" : valor.replace(';', ',').replace('\n', ' ');
    }
}
