package canchapro.tienda;

import canchapro.clientes.Cliente;
import canchapro.pagos.MetodoPago;
import canchapro.usuarios.Usuario;
import canchapro.util.GeneradorCodigo;
import canchapro.util.Texto;
import canchapro.util.excepciones.CanchaProException;
import canchapro.util.excepciones.DatosInvalidosException;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Reglas de la tienda/cafetería: catálogo de productos, inventario y ventas.
 */
// POO - Responsabilidad: reglas de la tienda (stock suficiente, todo o nada al vender, anular devuelve unidades).
public class TiendaServicio {

    private static final String PREFIJO_PRODUCTO = "PRD-";
    private static final String PREFIJO_VENTA = "VTA-";

    // POO - Relaciones (composición): usa los repositorios de productos, ventas y detalle de ventas.
    private final ProductoRepositorio productos;
    private final VentaRepositorio ventas;
    private final DetalleVentaRepositorio detalles;
    private final Clock reloj;

    public TiendaServicio(ProductoRepositorio productos, VentaRepositorio ventas, DetalleVentaRepositorio detalles,
            Clock reloj) {
        this.productos = productos;
        this.ventas = ventas;
        this.detalles = detalles;
        this.reloj = reloj;
    }

    /** Primer arranque: algunos productos de ejemplo. */
    public boolean asegurarDatosIniciales() {
        if (!productos.estaVacio()) {
            return false;
        }
        agregarInicial("PRD-001", "Agua pura 600 ml", CategoriaProducto.BEBIDA, "5", 48, 12);
        agregarInicial("PRD-002", "Bebida hidratante", CategoriaProducto.BEBIDA, "12", 24, 6);
        agregarInicial("PRD-003", "Gaseosa lata", CategoriaProducto.BEBIDA, "8", 24, 6);
        agregarInicial("PRD-004", "Papalinas", CategoriaProducto.SNACK, "6", 30, 10);
        agregarInicial("PRD-005", "Sándwich de pollo", CategoriaProducto.COMIDA, "20", 10, 3);
        agregarInicial("PRD-006", "Calcetas deportivas", CategoriaProducto.ARTICULO_DEPORTIVO, "35", 8, 2);
        productos.guardar();
        return true;
    }

    private void agregarInicial(String codigo, String nombre, CategoriaProducto categoria, String precio,
            int existencia, int minimo) {
        productos.agregar(new Producto(codigo, nombre, categoria, new BigDecimal(precio), existencia, minimo, true));
    }

    // --- Productos ---

    public Producto[] listarProductos() {
        Producto[] lista = productos.listar();
        Arrays.sort(lista, Comparator.comparing(Producto::getCodigo));
        return lista;
    }

    /** Productos activos cuyo código o nombre contiene el texto (sin tildes ni mayúsculas). */
    public Producto[] buscarProductosActivos(String texto) {
        boolean sinFiltro = texto == null || texto.isBlank();
        Producto[] todos = listarProductos();
        Producto[] encontrados = new Producto[todos.length];
        int cantidad = 0;
        for (Producto p : todos) {
            if (p.isActivo() && (sinFiltro || Texto.contieneTodas(p.getCodigo() + " " + p.getNombre(), texto))) {
                encontrados[cantidad] = p;
                cantidad++;
            }
        }
        return Arrays.copyOf(encontrados, cantidad);
    }

    public Producto obtenerProducto(String codigo) {
        return productos.buscar(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el producto " + codigo));
    }

    public Producto registrarProducto(String nombre, CategoriaProducto categoria, BigDecimal precio,
            int existenciaInicial, int stockMinimo) {
        validarNombreDisponible(nombre, null);
        String codigo = GeneradorCodigo.siguiente(PREFIJO_PRODUCTO, 3, productos.codigos());
        Producto producto = new Producto(codigo, nombre, categoria, precio, existenciaInicial, stockMinimo, true);
        productos.agregar(producto);
        productos.guardar();
        return producto;
    }

    /** Cambiar el precio no afecta ventas anteriores (cada línea guarda su precio). */
    public Producto actualizarProducto(String codigo, String nombre, CategoriaProducto categoria, BigDecimal precio,
            int stockMinimo) {
        Producto producto = obtenerProducto(codigo);
        validarNombreDisponible(nombre, codigo);
        producto.actualizarDatos(nombre, categoria, precio, stockMinimo);
        productos.actualizar(producto);
        productos.guardar();
        return producto;
    }

    public void cambiarEstadoProducto(String codigo, boolean activo) {
        Producto producto = obtenerProducto(codigo);
        if (producto.isActivo() != activo) {
            producto.setActivo(activo);
            productos.actualizar(producto);
            productos.guardar();
        }
    }

    public Producto reponerInventario(String codigo, int cantidad) {
        Producto producto = obtenerProducto(codigo);
        producto.reponer(cantidad);
        productos.actualizar(producto);
        productos.guardar();
        return producto;
    }

    public Producto[] productosConStockBajo() {
        Producto[] todos = listarProductos();
        Producto[] bajos = new Producto[todos.length];
        int cantidad = 0;
        for (Producto p : todos) {
            if (p.isActivo() && p.stockBajo()) {
                bajos[cantidad] = p;
                cantidad++;
            }
        }
        return Arrays.copyOf(bajos, cantidad);
    }

    /** Verifica (sin vender) que haya existencia para la cantidad indicada. */
    public void verificarStock(Producto producto, int cantidad) {
        obtenerProducto(producto.getCodigo()).verificarStock(cantidad);
    }

    // --- Ventas ---

    /**
     * Registra una venta y descuenta el inventario.
     * Primero se valida el stock de TODOS los productos; si alguno no
     * alcanza, no se descuenta nada (todo o nada).
     *
     * @param cliente cliente, o null para venta de mostrador
     */
    public Venta registrarVenta(Cliente cliente, ItemVenta[] items, MetodoPago metodo, Usuario usuario) {
        if (items == null || items.length == 0) {
            throw new DatosInvalidosException("Agregue al menos un producto a la venta.");
        }
        if (metodo == null) {
            throw new DatosInvalidosException("Seleccione el método de pago.");
        }
        if (cliente != null && !cliente.isActivo()) {
            throw new ReglaNegocioException("El cliente " + cliente.getNombre() + " está inactivo.");
        }

        // Un mismo producto agregado dos veces se suma en una sola línea.
        // POO - Colecciones: arreglos paralelos de códigos y cantidades (posición i = producto i).
        String[] codigos = new String[items.length];
        int[] cantidades = new int[items.length];
        int distintos = 0;
        for (ItemVenta item : items) {
            if (item.cantidad() <= 0) {
                throw new DatosInvalidosException("La cantidad de " + item.producto().getNombre() + " debe ser mayor que cero.");
            }
            int indice = -1;
            for (int i = 0; i < distintos; i++) {
                if (codigos[i].equals(item.producto().getCodigo())) {
                    indice = i;
                    break;
                }
            }
            if (indice < 0) {
                codigos[distintos] = item.producto().getCodigo();
                indice = distintos;
                distintos++;
            }
            cantidades[indice] += item.cantidad();
        }

        // 1. Validar todo antes de modificar nada.
        for (int i = 0; i < distintos; i++) {
            Producto producto = obtenerProducto(codigos[i]);
            if (!producto.isActivo()) {
                throw new ReglaNegocioException("El producto " + producto.getNombre() + " no está activo.");
            }
            producto.verificarStock(cantidades[i]);
        }

        // 2. Crear la venta y descontar el inventario.
        String codigo = GeneradorCodigo.siguiente(PREFIJO_VENTA, 5, ventas.codigos());
        Venta venta = new Venta(codigo, LocalDateTime.now(reloj), cliente, metodo, usuario, Venta.Estado.REGISTRADA);
        DetalleVenta[] lineas = new DetalleVenta[distintos];
        for (int i = 0; i < distintos; i++) {
            Producto producto = obtenerProducto(codigos[i]);
            producto.descontar(cantidades[i]);
            lineas[i] = new DetalleVenta(codigo, i + 1, producto, cantidades[i], producto.getPrecio());
        }
        venta.reemplazarDetalles(lineas);

        ventas.agregar(venta);
        for (DetalleVenta linea : lineas) {
            detalles.agregar(linea);
        }
        guardarTodo();
        return venta;
    }

    /** Anula la venta y devuelve las unidades al inventario. */
    public void anularVenta(String codigo) {
        Venta venta = obtenerVenta(codigo);
        venta.anular();
        for (DetalleVenta d : venta.getDetalles()) {
            d.getProducto().reponer(d.getCantidad());
        }
        guardarTodo();
    }

    public Venta obtenerVenta(String codigo) {
        return ventas.buscar(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe la venta " + codigo));
    }

    /** Ventas entre dos fechas (incluidas), de la más reciente a la más antigua. */
    public Venta[] ventasPorPeriodo(LocalDate desde, LocalDate hasta) {
        Venta[] todas = ventas.listar();
        Venta[] encontradas = new Venta[todas.length];
        int cantidad = 0;
        for (Venta v : todas) {
            LocalDate fecha = v.getFechaHora().toLocalDate();
            if (!fecha.isBefore(desde) && !fecha.isAfter(hasta)) {
                encontradas[cantidad] = v;
                cantidad++;
            }
        }
        Venta[] resultado = Arrays.copyOf(encontradas, cantidad);
        Arrays.sort(resultado, Comparator.comparing(Venta::getFechaHora).reversed());
        return resultado;
    }

    /** Total vendido hoy (sin ventas anuladas). */
    public BigDecimal totalVendidoHoy() {
        LocalDate hoy = LocalDate.now(reloj);
        BigDecimal total = BigDecimal.ZERO;
        for (Venta v : ventasPorPeriodo(hoy, hoy)) {
            if (v.getEstado() == Venta.Estado.REGISTRADA) {
                total = total.add(v.getTotal());
            }
        }
        return total;
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private void validarNombreDisponible(String nombre, String codigoPropio) {
        String buscado = Texto.normalizar(nombre);
        for (Producto p : productos.listar()) {
            if (!p.getCodigo().equals(codigoPropio) && Texto.normalizar(p.getNombre()).equals(buscado)) {
                throw new ReglaNegocioException("Ya existe el producto " + p.getCodigo() + " con el nombre " + p.getNombre() + ".");
            }
        }
    }

    /**
     * Guarda ventas, líneas y productos. Si algo falla, se vuelve a leer
     * todo para que la memoria coincida con los archivos.
     */
    private void guardarTodo() {
        try {
            ventas.guardar();
            detalles.guardar();
            productos.guardar();
        } catch (CanchaProException e) {
            productos.cargar();
            ventas.cargar();
            detalles.cargar();
            throw e;
        }
    }
}
