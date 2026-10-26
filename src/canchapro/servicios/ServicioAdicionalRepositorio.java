package canchapro.servicios;

import canchapro.persistencia.CamposCsv;
import canchapro.persistencia.RepositorioCsv;
import java.nio.file.Path;

/**
 * Servicios adicionales guardados en servicios.csv:
 * codigo;nombre;descripcion;precio;tipoCobro;activo
 */
// POO - Herencia / Reutilización: hereda de RepositorioCsv; aquí solo se define el formato de servicios.csv.
// POO - Polimorfismo: los métodos con @Override implementan los métodos abstractos de RepositorioCsv.
public class ServicioAdicionalRepositorio extends RepositorioCsv<ServicioAdicional> {

    public static final String ARCHIVO = "servicios.csv";

    public ServicioAdicionalRepositorio(Path carpetaDatos) {
        super(carpetaDatos.resolve(ARCHIVO));
    }

    // --- Formato del archivo ---

    @Override
    protected String encabezado() {
        return "codigo;nombre;descripcion;precio;tipoCobro;activo";
    }

    @Override
    protected int cantidadCampos() {
        return 6;
    }

    @Override
    protected ServicioAdicional desdeCsv(String[] c) {
        return new ServicioAdicional(c[0], c[1], c[2],
                CamposCsv.decimal(c[3], "precio"),
                CamposCsv.enumeracion(TipoCobro.class, c[4], "tipoCobro"),
                CamposCsv.booleano(c[5], "activo"));
    }

    @Override
    protected String aCsv(ServicioAdicional s) {
        return String.join(";",
                s.getCodigo(), s.getNombre(), s.getDescripcion(), s.getPrecio().toPlainString(),
                s.getTipoCobro().name(), String.valueOf(s.isActivo()));
    }

    @Override
    protected ServicioAdicional[] crearArreglo(int tamanio) {
        return new ServicioAdicional[tamanio];
    }
}
