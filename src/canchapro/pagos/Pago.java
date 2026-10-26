package canchapro.pagos;

import canchapro.usuarios.Usuario;
import canchapro.util.Identificable;
import canchapro.util.Validador;
import canchapro.util.excepciones.DatosInvalidosException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Pago (anticipo o abono) recibido para una reserva. Es inmutable: un pago
 * registrado no se modifica.
 */
// POO - Polimorfismo (interfaz): implementa Identificable para poder guardarse en un RepositorioCsv.
// POO - Encapsulamiento: clase final e inmutable; un pago registrado no se modifica.
// POO - Relaciones: el pago conoce el código de su reserva y al usuario que lo recibió.
public final class Pago implements Identificable {

    private final String codigo;
    private final String codigoReserva;
    private final LocalDateTime fechaHora;
    private final BigDecimal monto;
    private final MetodoPago metodo;
    private final String observacion;
    private final Usuario usuario;

    public Pago(String codigo, String codigoReserva, LocalDateTime fechaHora, BigDecimal monto,
            MetodoPago metodo, String observacion, Usuario usuario) {
        this.codigo = Validador.requerido(codigo, "El código del pago");
        this.codigoReserva = Validador.requerido(codigoReserva, "La reserva del pago");
        this.fechaHora = Objects.requireNonNull(fechaHora, "La fecha del pago es obligatoria");
        if (monto == null || monto.signum() <= 0) {
            throw new DatosInvalidosException("No se aceptan pagos negativos o en cero.");
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new DatosInvalidosException("El monto admite como máximo dos decimales.");
        }
        this.monto = monto.setScale(2, RoundingMode.HALF_UP);
        if (metodo == null) {
            throw new DatosInvalidosException("Seleccione el método de pago.");
        }
        this.metodo = metodo;
        String obs = Validador.opcional(observacion);
        Validador.longitud(obs, 0, 100, "La observación");
        Validador.sinSeparadorCsv(obs, "La observación");
        this.observacion = obs;
        this.usuario = Objects.requireNonNull(usuario, "El usuario que registra el pago es obligatorio");
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    public String getCodigoReserva() {
        return codigoReserva;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public String getObservacion() {
        return observacion;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
