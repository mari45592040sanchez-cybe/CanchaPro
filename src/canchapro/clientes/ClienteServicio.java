package canchapro.clientes;

import canchapro.usuarios.Persona;
import canchapro.util.GeneradorCodigo;
import canchapro.util.Texto;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.text.Collator;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

/**
 * Reglas de negocio de clientes: búsqueda, registro sin duplicados,
 * edición y activación/desactivación.
 */
// POO - Responsabilidad: aquí están las reglas de negocio de clientes; la pantalla no valida
// ni guarda directamente, y el repositorio solo guarda.
public class ClienteServicio {

    private static final String PREFIJO_CODIGO = "CLI-";
    private static final Comparator<Cliente> POR_NOMBRE =
            Comparator.comparing(Cliente::getNombre, Collator.getInstance(Locale.of("es")));

    // POO - Relaciones (composición): el servicio TIENE un repositorio, que recibe por el constructor.
    private final ClienteRepositorio repositorio;

    public ClienteServicio(ClienteRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Busca clientes cuyo código, nombre o teléfono contengan todas las
     * palabras del texto (sin distinguir mayúsculas ni tildes).
     * Un texto vacío devuelve todos.
     *
     * @param incluirInactivos false para listar solo clientes activos
     */
    public Cliente[] buscar(String texto, boolean incluirInactivos) {
        boolean sinFiltro = texto == null || texto.isBlank();
        Cliente[] todos = repositorio.listar();

        // POO - Colecciones: se copian a un arreglo temporal solo los que cumplen el filtro.
        Cliente[] encontrados = new Cliente[todos.length];
        int cantidad = 0;
        for (Cliente c : todos) {
            boolean estadoValido = incluirInactivos || c.isActivo();
            boolean coincide = sinFiltro || Texto.contieneTodas(
                    c.getCodigo() + " " + c.getNombre() + " " + c.getTelefono(), texto);
            if (estadoValido && coincide) {
                encontrados[cantidad] = c;
                cantidad++;
            }
        }

        // Arreglo del tamaño exacto, ordenado alfabéticamente.
        Cliente[] resultado = new Cliente[cantidad];
        for (int i = 0; i < cantidad; i++) {
            resultado[i] = encontrados[i];
        }
        Arrays.sort(resultado, POR_NOMBRE);
        return resultado;
    }

    public Cliente obtener(String codigo) {
        return repositorio.buscar(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el cliente " + codigo));
    }

    /** Cliente (activo o inactivo) que ya usa ese teléfono, si existe. */
    public Optional<Cliente> buscarPorTelefono(String telefono) {
        return repositorio.buscarPorTelefono(telefono);
    }

    /**
     * Registra un cliente nuevo con fecha de registro de hoy.
     *
     * @throws ReglaNegocioException si el teléfono ya pertenece a otro cliente
     */
    public Cliente registrar(String nombre, String telefono, String correo) {
        String telefonoValido = Persona.validarTelefono(telefono, true);
        validarTelefonoDisponible(telefonoValido, null);

        String codigo = GeneradorCodigo.siguiente(PREFIJO_CODIGO, 4, repositorio.codigos());
        Cliente cliente = new Cliente(codigo, nombre, telefonoValido, correo, LocalDate.now(), true);
        repositorio.agregar(cliente);
        repositorio.guardar();
        return cliente;
    }

    /**
     * Actualiza nombre, teléfono y correo. El código y la fecha de registro
     * no cambian. Se valida todo antes de modificar el objeto.
     */
    public Cliente actualizar(String codigo, String nombre, String telefono, String correo) {
        Cliente cliente = obtener(codigo);

        String nombreValido = Persona.validarNombre(nombre);
        String telefonoValido = Persona.validarTelefono(telefono, true);
        String correoValido = Persona.validarCorreo(correo);
        validarTelefonoDisponible(telefonoValido, codigo);

        // POO - Encapsulamiento: los datos se cambian solo mediante los setters, que vuelven a validar.
        cliente.setNombre(nombreValido);
        cliente.setTelefono(telefonoValido);
        cliente.setCorreo(correoValido);
        repositorio.actualizar(cliente);
        repositorio.guardar();
        return cliente;
    }

    /**
     * Activa o desactiva un cliente. Un cliente inactivo no aparece al
     * crear reservas o ventas, pero conserva su historial.
     */
    public void cambiarEstado(String codigo, boolean activo) {
        Cliente cliente = obtener(codigo);
        if (cliente.isActivo() == activo) {
            return;
        }
        cliente.setActivo(activo);
        repositorio.actualizar(cliente);
        repositorio.guardar();
    }

    /**
     * Regla anti-duplicados: el teléfono identifica al cliente, por lo que
     * no puede repetirse. Si pertenece a un cliente inactivo, se sugiere
     * reactivarlo en lugar de crear otro.
     */
    private void validarTelefonoDisponible(String telefono, String codigoPropio) {
        repositorio.buscarPorTelefono(telefono)
                .filter(existente -> !existente.getCodigo().equals(codigoPropio))
                .ifPresent(existente -> {
                    String sugerencia = existente.isActivo()
                            ? "Búsquelo y selecciónelo en lugar de registrarlo de nuevo."
                            : "Está inactivo: actívelo desde el módulo Clientes (opción \"Mostrar inactivos\").";
                    throw new ReglaNegocioException("El teléfono " + telefono + " ya pertenece al cliente "
                            + existente.getDescripcion() + ".\n" + sugerencia);
                });
    }
}
