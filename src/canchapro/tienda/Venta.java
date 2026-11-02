package canchapro.tienda;

import canchapro.clientes.Cliente;
import canchapro.pagos.MetodoPago;
import canchapro.usuarios.Usuario;
import canchapro.util.Identificable;
import canchapro.util.excepciones.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * Venta de la tienda con uno o varios productos. El cliente es opcional
 * (venta de mostrador). Una venta no se elimina: se anula.
 */
// POO - Polimorfismo (interfaz): implementa Identificable para poder guardarse en un RepositorioCsv.
// POO - Encapsulamiento: atributos privados; el estado solo cambia con anular().
public class Venta implements Identificable {

    /** Estados de una venta. */
    public enum Estado {
        REGISTRADA("Registrada"),
        ANULADA("Anulada");

        private final String nombreVisible;

        Estado(String nombreVisible) {
            this.nombreVisible = nombreVisible;
        }

        @Override
        public String toString() {
            return nombreVisible;
        }
    }

    private final String codigo;
    private final LocalDateTime fechaHora;
    // POO - Relaciones: la venta conoce a su cliente (opcional) y al usuario que la registró.
    private final Cliente cliente;
    // POO - Colecciones / Relaciones (composición): arreglo con las líneas (detalles) de la venta.
    private DetalleVenta[] detalles = new DetalleVenta[0];
    private final MetodoPago metodoPago;
    private final Usuario usuario;
    private Estado estado;

    public Venta(String codigo, LocalDateTime fechaHora, Cliente cliente, MetodoPago metodoPago, Usuario usuario,
            Estado estado) {
        this.codigo = Objects.requireNonNull(codigo);
        this.fechaHora = Objects.requireNonNull(fechaHora);
        this.cliente = cliente;
        this.metodoPago = Objects.requireNonNull(metodoPago, "Seleccione el método de pago");
        this.usuario = Objects.requireNonNull(usuario);
        this.estado = Objects.requireNonNull(estado);
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVenta d : detalles) {
            total = total.add(d.getSubtotal());
        }
        return total;
    }

    public int getCantidadArticulos() {
        int cantidad = 0;
        for (DetalleVenta d : detalles) {
            cantidad += d.getCantidad();
        }
        return cantidad;
    }

    /** Marca la venta como anulada. El inventario lo devuelve el servicio. */
    public void anular() {
        if (estado == Estado.ANULADA) {
            throw new ReglaNegocioException("La venta " + codigo + " ya está anulada.");
        }
        estado = Estado.ANULADA;
    }

    /** Reemplaza las líneas al crear la venta o al reconstruirla desde archivo. */
    void reemplazarDetalles(DetalleVenta[] nuevos) {
        detalles = Arrays.copyOf(nuevos, nuevos.length);
        Arrays.sort(detalles, Comparator.comparingInt(DetalleVenta::getLinea));
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    /** null si fue una venta de mostrador sin cliente. */
    public Cliente getCliente() {
        return cliente;
    }

    // POO - Encapsulamiento: se devuelve una copia; desde afuera no se pueden alterar las líneas.
    public DetalleVenta[] getDetalles() {
        return Arrays.copyOf(detalles, detalles.length);
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Estado getEstado() {
        return estado;
    }
}
