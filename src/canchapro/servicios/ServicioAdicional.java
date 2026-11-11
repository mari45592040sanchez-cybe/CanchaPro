package canchapro.servicios;

import canchapro.util.FormatoMoneda;
import canchapro.util.Identificable;
import canchapro.util.Validador;
import canchapro.util.excepciones.DatosInvalidosException;
import java.math.BigDecimal;
import java.time.Duration;

// POO - Polimorfismo (interfaz): implementa Identificable para poder guardarse en un RepositorioCsv.
// POO - Encapsulamiento: atributos privados; actualizarDatos() valida todo antes de asignar.
// POO - Relaciones: TIENE un TipoCobro que decide cómo se calcula su importe.
public class ServicioAdicional implements Identificable {

    public static final int CANTIDAD_MAXIMA = 50;
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("5000");

    private final String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private TipoCobro tipoCobro;
    private boolean activo;

    public ServicioAdicional(String codigo, String nombre, String descripcion, BigDecimal precio,
            TipoCobro tipoCobro, boolean activo) {
        this.codigo = Validador.requerido(codigo, "El código");
        actualizarDatos(nombre, descripcion, precio, tipoCobro);
        this.activo = activo;
    }

    public final void actualizarDatos(String nombre, String descripcion, BigDecimal precio, TipoCobro tipoCobro) {
        String nombreValido = Validador.texto(nombre, 3, 40, "El nombre del servicio");
        String descripcionValida = Validador.opcional(descripcion);
        Validador.longitud(descripcionValida, 0, 120, "La descripción");
        Validador.sinSeparadorCsv(descripcionValida, "La descripción");
        BigDecimal precioValido = Validador.precio(precio, PRECIO_MAXIMO, "El precio");
        if (tipoCobro == null) {
            throw new DatosInvalidosException("Seleccione el tipo de cobro.");
        }

        this.nombre = nombreValido;
        this.descripcion = descripcionValida;
        this.precio = precioValido;
        this.tipoCobro = tipoCobro;
    }

    public BigDecimal calcularImporte(int cantidad, Duration duracion) {
        if (cantidad < 1 || cantidad > CANTIDAD_MAXIMA) {
            throw new DatosInvalidosException("La cantidad de " + nombre + " debe estar entre 1 y " + CANTIDAD_MAXIMA + ".");
        }
        return tipoCobro.calcular(precio, cantidad, duracion);
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public TipoCobro getTipoCobro() {
        return tipoCobro;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return nombre + " (" + FormatoMoneda.formatear(precio) + " " + tipoCobro.toString().toLowerCase() + ")";
    }
}
