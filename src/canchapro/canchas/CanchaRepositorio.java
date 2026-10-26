package canchapro.canchas;

import canchapro.persistencia.CamposCsv;
import canchapro.persistencia.RepositorioCsv;
import canchapro.util.FormatoFecha;
import java.nio.file.Path;

/**
 * Canchas guardadas en canchas.csv:
 * codigo;nombre;tipo;capacidad;estado;horaApertura;horaCierre;descripcion
 */
// POO - Herencia / Reutilización: hereda de RepositorioCsv toda la lógica de cargar, guardar y buscar;
// aquí solo se define el formato del archivo de canchas.
public class CanchaRepositorio extends RepositorioCsv<Cancha> {

    public static final String ARCHIVO = "canchas.csv";

    public CanchaRepositorio(Path carpetaDatos) {
        super(carpetaDatos.resolve(ARCHIVO));
    }

    // --- Formato del archivo ---
    // POO - Polimorfismo: sobrescritura (@Override) de los métodos abstractos de RepositorioCsv.

    @Override
    protected String encabezado() {
        return "codigo;nombre;tipo;capacidad;estado;horaApertura;horaCierre;descripcion";
    }

    @Override
    protected int cantidadCampos() {
        return 8;
    }

    @Override
    protected Cancha desdeCsv(String[] c) {
        return new Cancha(c[0], c[1],
                CamposCsv.enumeracion(TipoCancha.class, c[2], "tipo"),
                CamposCsv.entero(c[3], "capacidad"),
                CamposCsv.enumeracion(EstadoCancha.class, c[4], "estado"),
                CamposCsv.hora(c[5], "horaApertura"),
                CamposCsv.hora(c[6], "horaCierre"),
                c[7]);
    }

    @Override
    protected String aCsv(Cancha cancha) {
        return String.join(";",
                cancha.getCodigo(), cancha.getNombre(), cancha.getTipo().name(),
                String.valueOf(cancha.getCapacidad()), cancha.getEstado().name(),
                cancha.getHoraApertura().format(FormatoFecha.ARCHIVO_HORA),
                cancha.getHoraCierre().format(FormatoFecha.ARCHIVO_HORA),
                cancha.getDescripcion());
    }

    @Override
    protected Cancha[] crearArreglo(int tamanio) {
        return new Cancha[tamanio];
    }
}
