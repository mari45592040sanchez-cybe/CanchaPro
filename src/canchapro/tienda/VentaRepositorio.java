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
import java.util.function.Supplier;

/**
 * Ventas guardadas en ventas.csv:
 * codigo;fechaHora;codigoCliente;metodoPago;codigoUsuario;estado
 * codigoCliente queda vacío en ventas de mostrador. Las líneas de cada
 * venta se leen de detalle_ventas.csv y se asocian al cargar.
 */
// POO - Herencia / Reutilización: hereda de RepositorioCsv; aquí solo se define el formato de ventas.csv.
// POO - Polimorfismo: los métodos con @Override implementan los métodos abstractos de RepositorioCsv.
public class VentaRepositorio extends RepositorioCsv<Venta> {

    public static final String ARCHIVO = "ventas.csv";

    private final ClienteRepositorio clientes;
    private final UsuarioRepositorio usuarios;
    private Supplier<DetalleVenta[]> fuenteDetalles = () -> new DetalleVenta[0];

    public VentaRepositorio(Path carpetaDatos, ClienteRepositorio clientes, UsuarioRepositorio usuarios) {
        super(carpetaDatos.resolve(ARCHIVO));
        this.clientes = clientes;
        this.usuarios = usuarios;
    }

    public void setFuenteDetalles(Supplier<DetalleVenta[]> fuenteDetalles) {
        this.fuenteDetalles = fuenteDetalles;
    }

    @Override
    protected void despuesDeCargar() {
        vincularDetalles();
    }

    /** Asocia a cada venta sus líneas (se llama tras leer ventas o detalles). */
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

    // --- Formato del archivo ---

    @Override
    protected String encabezado() {
        return "codigo;fechaHora;codigoCliente;metodoPago;codigoUsuario;estado";
    }

    @Override
    protected int cantidadCampos() {
        return 6;
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
        return new Venta(c[0].trim(), CamposCsv.fechaHora(c[1], "fechaHora"), cliente,
                CamposCsv.enumeracion(MetodoPago.class, c[3], "metodoPago"), usuario,
                CamposCsv.enumeracion(Venta.Estado.class, c[5], "estado"));
    }

    @Override
    protected String aCsv(Venta v) {
        return String.join(";",
                v.getCodigo(), v.getFechaHora().format(FormatoFecha.ARCHIVO_FECHA_HORA),
                v.getCliente() == null ? "" : v.getCliente().getCodigo(),
                v.getMetodoPago().name(), v.getUsuario().getCodigo(), v.getEstado().name());
    }

    @Override
    protected Venta[] crearArreglo(int tamanio) {
        return new Venta[tamanio];
    }
}
