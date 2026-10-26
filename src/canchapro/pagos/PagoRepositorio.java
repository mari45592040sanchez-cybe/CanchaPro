package canchapro.pagos;

import canchapro.persistencia.CamposCsv;
import canchapro.persistencia.RepositorioCsv;
import canchapro.usuarios.Usuario;
import canchapro.usuarios.UsuarioRepositorio;
import canchapro.util.FormatoFecha;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Pagos guardados en pagos.csv:
 * codigo;codigoReserva;fechaHora;monto;metodo;observacion;codigoUsuario
 * Cada pago guarda el código de su reserva; un pago de una reserva
 * inexistente se omite al cargar y se registra como error.
 */
// POO - Herencia / Reutilización: hereda de RepositorioCsv; aquí solo se define el formato de pagos.csv.
// POO - Polimorfismo: los métodos con @Override implementan los métodos abstractos de RepositorioCsv.
public class PagoRepositorio extends RepositorioCsv<Pago> {

    public static final String ARCHIVO = "pagos.csv";

    private final Predicate<String> existeReserva;
    private final UsuarioRepositorio usuarios;
    private Runnable alCargar = () -> { };

    /**
     * @param existeReserva indica si un código de reserva existe (integridad referencial)
     */
    public PagoRepositorio(Path carpetaDatos, Predicate<String> existeReserva, UsuarioRepositorio usuarios) {
        super(carpetaDatos.resolve(ARCHIVO));
        this.existeReserva = existeReserva;
        this.usuarios = usuarios;
    }

    /**
     * Acción que se ejecuta cada vez que se vuelve a leer el archivo (por
     * ejemplo, para volver a asociar los pagos a sus reservas).
     */
    public void setAlCargar(Runnable alCargar) {
        this.alCargar = alCargar;
    }

    @Override
    protected void despuesDeCargar() {
        alCargar.run();
    }

    public Pago[] listarPorReserva(String codigoReserva) {
        Pago[] todos = listar();
        Pago[] encontrados = new Pago[todos.length];
        int cantidad = 0;
        for (Pago p : todos) {
            if (p.getCodigoReserva().equals(codigoReserva)) {
                encontrados[cantidad] = p;
                cantidad++;
            }
        }
        return Arrays.copyOf(encontrados, cantidad);
    }

    /** Pagos recibidos entre dos fechas (ambas incluidas). */
    public Pago[] listarPorPeriodo(LocalDate desde, LocalDate hasta) {
        Pago[] todos = listar();
        Pago[] encontrados = new Pago[todos.length];
        int cantidad = 0;
        for (Pago p : todos) {
            LocalDate fecha = p.getFechaHora().toLocalDate();
            if (!fecha.isBefore(desde) && !fecha.isAfter(hasta)) {
                encontrados[cantidad] = p;
                cantidad++;
            }
        }
        return Arrays.copyOf(encontrados, cantidad);
    }

    // --- Formato del archivo ---

    @Override
    protected String encabezado() {
        return "codigo;codigoReserva;fechaHora;monto;metodo;observacion;codigoUsuario";
    }

    @Override
    protected int cantidadCampos() {
        return 7;
    }

    @Override
    protected Pago desdeCsv(String[] c) {
        if (!existeReserva.test(c[1].trim())) {
            throw new EntidadNoEncontradaException("el pago corresponde a la reserva inexistente " + c[1]);
        }
        Usuario usuario = usuarios.buscar(c[6].trim())
                .orElseThrow(() -> new EntidadNoEncontradaException("usuario inexistente " + c[6]));
        return new Pago(c[0], c[1].trim(),
                CamposCsv.fechaHora(c[2], "fechaHora"),
                CamposCsv.decimal(c[3], "monto"),
                CamposCsv.enumeracion(MetodoPago.class, c[4], "metodo"),
                c[5], usuario);
    }

    @Override
    protected String aCsv(Pago p) {
        return String.join(";",
                p.getCodigo(), p.getCodigoReserva(), p.getFechaHora().format(FormatoFecha.ARCHIVO_FECHA_HORA),
                p.getMonto().toPlainString(), p.getMetodo().name(), p.getObservacion(), p.getUsuario().getCodigo());
    }

    @Override
    protected Pago[] crearArreglo(int tamanio) {
        return new Pago[tamanio];
    }
}
