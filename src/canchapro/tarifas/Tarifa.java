package canchapro.tarifas;

import canchapro.canchas.TipoCancha;
import canchapro.util.BloquesHorario;
import canchapro.util.Identificable;
import canchapro.util.Validador;
import canchapro.util.excepciones.DatosInvalidosException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Precio por hora de una franja horaria para un tipo de día.
 * Si {@code tipoCancha} es null, la tarifa aplica a todos los tipos de
 * cancha; si tiene un tipo, es una tarifa específica que tiene prioridad.
 */
// POO - Polimorfismo (interfaz): implementa Identificable para poder guardarse en un RepositorioCsv.
// POO - Encapsulamiento: atributos privados; actualizarDatos() valida todo antes de asignar.
public class Tarifa implements Identificable {

    public static final int MINUTOS_BLOQUE = BloquesHorario.MINUTOS_BLOQUE;
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("10000");

    private final String codigo;
    private TipoDia tipoDia;
    private LocalTime horaDesde;
    private LocalTime horaHasta;
    private BigDecimal precioHora;
    private TipoCancha tipoCancha;
    private boolean activa;

    public Tarifa(String codigo, TipoDia tipoDia, LocalTime horaDesde, LocalTime horaHasta,
            BigDecimal precioHora, TipoCancha tipoCancha, boolean activa) {
        this.codigo = Validador.requerido(codigo, "El código");
        actualizarDatos(tipoDia, horaDesde, horaHasta, precioHora, tipoCancha);
        this.activa = activa;
    }

    /**
     * Valida todos los datos y solo entonces los asigna.
     */
    public final void actualizarDatos(TipoDia tipoDia, LocalTime horaDesde, LocalTime horaHasta,
            BigDecimal precioHora, TipoCancha tipoCancha) {
        if (tipoDia == null) {
            throw new DatosInvalidosException("Seleccione el tipo de día.");
        }
        validarFranja(horaDesde, horaHasta);
        BigDecimal precioValido = validarPrecio(precioHora);

        this.tipoDia = tipoDia;
        this.horaDesde = horaDesde;
        this.horaHasta = horaHasta;
        this.precioHora = precioValido;
        this.tipoCancha = tipoCancha;
    }

    public static void validarFranja(LocalTime desde, LocalTime hasta) {
        if (desde == null || hasta == null) {
            throw new DatosInvalidosException("Las horas de la franja son obligatorias.");
        }
        if (desde.getMinute() % MINUTOS_BLOQUE != 0 || hasta.getMinute() % MINUTOS_BLOQUE != 0
                || desde.getSecond() != 0 || hasta.getSecond() != 0) {
            throw new DatosInvalidosException("Las horas de la franja deben ser en punto o y media.");
        }
        if (!desde.isBefore(hasta)) {
            throw new DatosInvalidosException("La hora inicial de la franja debe ser anterior a la hora final.");
        }
    }

    public static BigDecimal validarPrecio(BigDecimal precio) {
        return Validador.precio(precio, PRECIO_MAXIMO, "El precio por hora");
    }

    /**
     * Indica si esta tarifa (activa) cubre el bloque que inicia a la hora
     * indicada. La franja incluye la hora inicial y excluye la final:
     * [horaDesde, horaHasta).
     */
    public boolean aplica(LocalDate fecha, LocalTime hora, TipoCancha tipo) {
        return activa
                && tipoDia == TipoDia.de(fecha)
                && (tipoCancha == null || tipoCancha == tipo)
                && !hora.isBefore(horaDesde)
                && hora.isBefore(horaHasta);
    }

    /** Dos tarifas chocan si son del mismo tipo de día y alcance, y sus franjas se traslapan. */
    public boolean seTraslapaCon(Tarifa otra) {
        return tipoDia == otra.tipoDia
                && tipoCancha == otra.tipoCancha
                && horaDesde.isBefore(otra.horaHasta)
                && horaHasta.isAfter(otra.horaDesde);
    }

    public boolean esEspecifica() {
        return tipoCancha != null;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    public TipoDia getTipoDia() {
        return tipoDia;
    }

    public LocalTime getHoraDesde() {
        return horaDesde;
    }

    public LocalTime getHoraHasta() {
        return horaHasta;
    }

    public BigDecimal getPrecioHora() {
        return precioHora;
    }

    /** null = aplica a todos los tipos de cancha. */
    public TipoCancha getTipoCancha() {
        return tipoCancha;
    }

    public String getAlcance() {
        return tipoCancha == null ? "Todos los tipos" : tipoCancha.toString();
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    /** "Lunes a viernes 08:00-17:00 (Todos los tipos)" */
    public String getDescripcion() {
        return tipoDia + " " + horaDesde + "-" + horaHasta + " (" + getAlcance() + ")";
    }
}
