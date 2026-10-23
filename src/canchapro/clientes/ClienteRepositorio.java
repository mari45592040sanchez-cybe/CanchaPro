package canchapro.clientes;

import canchapro.persistencia.CamposCsv;
import canchapro.persistencia.RepositorioCsv;
import canchapro.util.FormatoFecha;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Clientes guardados en clientes.csv:
 * codigo;nombre;telefono;correo;fechaRegistro;activo
 */
// POO - Herencia / Reutilización: hereda de RepositorioCsv toda la lógica de cargar, guardar y buscar;
// aquí solo se define el formato del archivo de clientes.
public class ClienteRepositorio extends RepositorioCsv<Cliente> {

    public static final String ARCHIVO = "clientes.csv";

    public ClienteRepositorio(Path carpetaDatos) {
        super(carpetaDatos.resolve(ARCHIVO));
    }

    /** Busca por teléfono entre todos los clientes (activos e inactivos). */
    public Optional<Cliente> buscarPorTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return Optional.empty();
        }
        String buscado = telefono.trim();
        // POO - Colecciones: recorrido secuencial del arreglo de clientes.
        for (Cliente cliente : listar()) {
            if (cliente.getTelefono().equals(buscado)) {
                return Optional.of(cliente);
            }
        }
        return Optional.empty();
    }

    // --- Formato del archivo ---
    // POO - Polimorfismo: sobrescritura (@Override) de los métodos abstractos de RepositorioCsv.

    @Override
    protected String encabezado() {
        return "codigo;nombre;telefono;correo;fechaRegistro;activo";
    }

    @Override
    protected int cantidadCampos() {
        return 6;
    }

    @Override
    protected Cliente desdeCsv(String[] c) {
        LocalDate fechaRegistro = CamposCsv.fecha(c[4], "fechaRegistro");
        boolean activo = CamposCsv.booleano(c[5], "activo");
        return new Cliente(c[0], c[1], c[2], c[3], fechaRegistro, activo);
    }

    @Override
    protected String aCsv(Cliente cliente) {
        return String.join(";",
                cliente.getCodigo(), cliente.getNombre(), cliente.getTelefono(), cliente.getCorreo(),
                cliente.getFechaRegistro().format(FormatoFecha.ARCHIVO_FECHA),
                String.valueOf(cliente.isActivo()));
    }

    @Override
    protected Cliente[] crearArreglo(int tamanio) {
        return new Cliente[tamanio];
    }
}
