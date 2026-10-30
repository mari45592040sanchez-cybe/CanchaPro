package canchapro.reportes;

import java.util.Arrays;

/**
 * Resultado de un reporte: columnas, filas y totales (todos como texto listo
 * para mostrar o exportar).
 */
// POO - Encapsulamiento: los arreglos son privados; se llenan con agregarFila/agregarTotal
// y se consultan con getters que devuelven copias.
public class ResultadoReporte {

    private static final int CAPACIDAD_INICIAL = 10;

    private final String titulo;
    private final String subtitulo;
    // POO - Colecciones: arreglo de columnas y matriz de filas (cada fila es un arreglo de textos).
    private final String[] columnas;
    private String[][] filas = new String[CAPACIDAD_INICIAL][];
    private int cantidadFilas;
    // Totales en dos arreglos paralelos: el concepto i tiene el valor i, en el orden en que se agregan.
    private String[] conceptosTotales = new String[CAPACIDAD_INICIAL];
    private String[] valoresTotales = new String[CAPACIDAD_INICIAL];
    private int cantidadTotales;

    public ResultadoReporte(String titulo, String subtitulo, String... columnas) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.columnas = Arrays.copyOf(columnas, columnas.length);
    }

    public void agregarFila(Object... valores) {
        String[] fila = new String[valores.length];
        for (int i = 0; i < valores.length; i++) {
            fila[i] = valores[i] == null ? "" : String.valueOf(valores[i]);
        }
        if (cantidadFilas == filas.length) {
            filas = Arrays.copyOf(filas, filas.length * 2);
        }
        filas[cantidadFilas] = fila;
        cantidadFilas++;
    }

    /** Agrega un total; si el concepto ya existe, reemplaza su valor. */
    public void agregarTotal(String concepto, Object valor) {
        String texto = String.valueOf(valor);
        int indice = indiceTotal(concepto);
        if (indice >= 0) {
            valoresTotales[indice] = texto;
            return;
        }
        if (cantidadTotales == conceptosTotales.length) {
            conceptosTotales = Arrays.copyOf(conceptosTotales, conceptosTotales.length * 2);
            valoresTotales = Arrays.copyOf(valoresTotales, valoresTotales.length * 2);
        }
        conceptosTotales[cantidadTotales] = concepto;
        valoresTotales[cantidadTotales] = texto;
        cantidadTotales++;
    }

    public String getTitulo() {
        return titulo;
    }

    /** Período y filtros aplicados, en texto. */
    public String getSubtitulo() {
        return subtitulo;
    }

    public String[] getColumnas() {
        return Arrays.copyOf(columnas, columnas.length);
    }

    public int getCantidadColumnas() {
        return columnas.length;
    }

    public String getColumna(int indice) {
        return columnas[indice];
    }

    /** Copia de las filas (cada fila también se copia). */
    public String[][] getFilas() {
        String[][] copia = new String[cantidadFilas][];
        for (int i = 0; i < cantidadFilas; i++) {
            copia[i] = Arrays.copyOf(filas[i], filas[i].length);
        }
        return copia;
    }

    public int getCantidadFilas() {
        return cantidadFilas;
    }

    public String getValor(int fila, int columna) {
        return filas[fila][columna];
    }

    public int getCantidadTotales() {
        return cantidadTotales;
    }

    public String getConceptoTotal(int indice) {
        return conceptosTotales[indice];
    }

    public String getValorTotal(int indice) {
        return valoresTotales[indice];
    }

    /** Valor de un total por su concepto, o null si no existe. */
    public String getTotal(String concepto) {
        int indice = indiceTotal(concepto);
        return indice < 0 ? null : valoresTotales[indice];
    }

    private int indiceTotal(String concepto) {
        for (int i = 0; i < cantidadTotales; i++) {
            if (conceptosTotales[i].equals(concepto)) {
                return i;
            }
        }
        return -1;
    }
}
