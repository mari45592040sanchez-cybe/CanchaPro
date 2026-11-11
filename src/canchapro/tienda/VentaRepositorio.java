package canchapro.tienda;

import canchapro.clientes.Cliente;
import canchapro.clientes.ClienteRepositorio;
import canchapro.pagos.MetodoPago;
import canchapro.persistencia.CamposCsv;
import canchapro.persistencia.RepositorioCsv;
import canchapro.usuarios.Usuario;
import canchapro.usuarios.UsuarioRepositorio;
import canchapro.util.FormatoFecha;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.function.Supplier;

// POO - Herencia / Reutilización: hereda de RepositorioCsv; aquí solo se define el formato de ventas.csv.
// POO - Polimorfismo: los métodos con @Override implementan los métodos abstractos de RepositorioCsv.
public class VentaRepositorio extends RepositorioCsv<Venta> {

    public static final String ARCHIVO = "ventas.csv";

    private static final String ENCABEZADO = "codigo;fechaHora;codigoCliente;metodoPago;codigoUsuario;estado;codigoReserva";
    private static final String ENCABEZADO_ANTERIOR = "codigo;fechaHora;codigoCliente;metodoPago;codigoUsuario;estado";
    private static final int CAMPOS = 7;

    // POO - Relaciones: usa los repositorios de clientes y usuarios para reconstruir las referencias.
    private final ClienteRepositorio clientes;
    private final UsuarioRepositorio usuarios;
    private final Predicate<String> existeReserva;
    private Supplier<DetalleVenta[]> fuenteDetalles = () -> new DetalleVenta[0];
    private Runnable alCargar = () -> { };

    public VentaRepositorio(Path carpetaDatos, ClienteRepositorio clientes, UsuarioRepositorio usuarios,
            Predicate<String> existeReserva) {
        super(carpetaDatos.resolve(ARCHIVO));
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.existeReserva = existeReserva;
    }

    public void setFuenteDetalles(Supplier<DetalleVenta[]> fuenteDetalles) {
        this.fuenteDetalles = fuenteDetalles;
    }

    public void setAlCargar(Runnable alCargar) {
        this.alCargar = alCargar;
    }

    // POO - Polimorfismo: sobrescribe el método de RepositorioCsv que se ejecuta al terminar cada carga.
    @Override
    protected void despuesDeCargar() {
        vincularDetalles();
        alCargar.run();
    }

    public void vincularDetalles() {
        DetalleVenta[] todos = fuenteDetalles.get();
        for (Venta venta : listar()) {
            // POO - Colecciones: se copian a un arreglo temporal las líneas de esta venta.
            DetalleVenta[] deLaVenta = new DetalleVenta[todos.length];
            int cantidad = 0;
            for (DetalleVenta d : todos) {
                if (d.getCodigoVenta().equals(venta.getCodigo())) {
                    deLaVenta[cantidad] = d;
                    cantidad++;
                }
            }
            venta.reemplazarDetalles(Arrays.copyOf(deLaVenta, cantidad));
        }
    }

    @Override
    protected String encabezado() {
        return ENCABEZADO;
    }

    @Override
    protected int cantidadCampos() {
        return CAMPOS;
    }

    // POO - Polimorfismo: sobrescribe el método de RepositorioCsv para aceptar también el archivo
    // de ventas anterior, que no tenía la columna codigoReserva.
    @Override
    protected boolean encabezadoAceptado(String encontrado) {
        return encontrado.equals(ENCABEZADO) || encontrado.equals(ENCABEZADO_ANTERIOR);
    }

    // POO - Polimorfismo: sobrescribe el método de RepositorioCsv. Una línea del archivo anterior
    // (sin codigoReserva) se completa con un campo vacío: es una venta con factura individual.
    // POO - Colecciones: arreglo de String con los campos de la línea.
    @Override
    protected String[] adaptarCampos(String[] campos) {
        if (campos.length != CAMPOS - 1) {
            return campos;
        }
        String[] completos = Arrays.copyOf(campos, CAMPOS);
        completos[CAMPOS - 1] = "";
        return completos;
    }

    @Override
    protected Venta desdeCsv(String[] c) {
        Cliente cliente = null;
        if (!c[2].isBlank()) {
            cliente = clientes.buscar(c[2].trim())
                    .orElseThrow(() -> new EntidadNoEncontradaException("cliente inexistente " + c[2]));
        }
        Usuario usuario = usuarios.buscar(c[4].trim())
                .orElseThrow(() -> new EntidadNoEncontradaException("usuario inexistente " + c[4]));
        String codigoReserva = c[6].isBlank() ? null : c[6].trim();
        MetodoPago metodo = null;
        if (codigoReserva == null) {
            metodo = CamposCsv.enumeracion(MetodoPago.class, c[3], "metodoPago");
        } else if (!existeReserva.test(codigoReserva)) {
            throw new EntidadNoEncontradaException("la venta corresponde a la reserva inexistente " + c[6]);
        }
        return new Venta(c[0].trim(), CamposCsv.fechaHora(c[1], "fechaHora"), cliente, metodo, usuario,
                CamposCsv.enumeracion(Venta.Estado.class, c[5], "estado"), codigoReserva);
    }

    @Override
    protected String aCsv(Venta v) {
        return String.join(";",
                v.getCodigo(), v.getFechaHora().format(FormatoFecha.ARCHIVO_FECHA_HORA),
                v.getCliente() == null ? "" : v.getCliente().getCodigo(),
                v.getMetodoPago() == null ? "" : v.getMetodoPago().name(),
                v.getUsuario().getCodigo(), v.getEstado().name(),
                v.getCodigoReserva() == null ? "" : v.getCodigoReserva());
    }

    @Override
    protected Venta[] crearArreglo(int tamanio) {
        return new Venta[tamanio];
    }
}
