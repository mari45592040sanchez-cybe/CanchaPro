package canchapro.tarifas;

import canchapro.canchas.TipoCancha;
import canchapro.persistencia.CamposCsv;
import canchapro.persistencia.RepositorioCsv;
import canchapro.util.FormatoFecha;
import java.nio.file.Path;

/**
 * Tarifas guardadas en tarifas.csv:
 * codigo;tipoDia;horaDesde;horaHasta;precioHora;tipoCancha;activa
 * En tipoCancha se escribe TODOS cuando la tarifa aplica a cualquier tipo.
 */
// POO - Herencia / Reutilización: hereda de RepositorioCsv; aquí solo se define el formato de tarifas.csv.
// POO - Polimorfismo: los métodos con @Override implementan los métodos abstractos de RepositorioCsv.
public class TarifaRepositorio extends RepositorioCsv<Tarifa> {

    public static final String ARCHIVO = "tarifas.csv";
    private static final String TODOS = "TODOS";

    public TarifaRepositorio(Path carpetaDatos) {
        super(carpetaDatos.resolve(ARCHIVO));
    }

    // --- Formato del archivo ---

    @Override
    protected String encabezado() {
        return "codigo;tipoDia;horaDesde;horaHasta;precioHora;tipoCancha;activa";
    }

    @Override
    protected int cantidadCampos() {
        return 7;
    }

    @Override
    protected Tarifa desdeCsv(String[] c) {
        TipoCancha tipoCancha = TODOS.equals(c[5].trim())
                ? null
                : CamposCsv.enumeracion(TipoCancha.class, c[5], "tipoCancha");
        return new Tarifa(c[0],
                CamposCsv.enumeracion(TipoDia.class, c[1], "tipoDia"),
                CamposCsv.hora(c[2], "horaDesde"),
                CamposCsv.hora(c[3], "horaHasta"),
                CamposCsv.decimal(c[4], "precioHora"),
                tipoCancha,
                CamposCsv.booleano(c[6], "activa"));
    }

    @Override
    protected String aCsv(Tarifa t) {
        return String.join(";",
                t.getCodigo(), t.getTipoDia().name(),
                t.getHoraDesde().format(FormatoFecha.ARCHIVO_HORA),
                t.getHoraHasta().format(FormatoFecha.ARCHIVO_HORA),
                t.getPrecioHora().toPlainString(),
                t.getTipoCancha() == null ? TODOS : t.getTipoCancha().name(),
                String.valueOf(t.isActiva()));
    }

    @Override
    protected Tarifa[] crearArreglo(int tamanio) {
        return new Tarifa[tamanio];
    }
}
