package canchapro.usuarios;

import canchapro.persistencia.CamposCsv;
import canchapro.persistencia.RepositorioCsv;
import canchapro.util.Validador;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Usuarios del sistema guardados en usuarios.csv:
 * codigo;rol;nombre;telefono;correo;username;passwordHash;salt;activo;debeCambiarPassword;modulos
 * Los módulos se separan con '|' y quedan vacíos para administradores.
 */
// POO - Herencia / Reutilización: hereda de RepositorioCsv toda la lógica de cargar, guardar y buscar.
public class UsuarioRepositorio extends RepositorioCsv<Usuario> {

    public static final String ARCHIVO = "usuarios.csv";
    private static final String SEPARADOR_MODULOS = "|";

    public UsuarioRepositorio(Path carpetaDatos) {
        super(carpetaDatos.resolve(ARCHIVO));
    }

    /** Búsqueda sin distinguir mayúsculas y minúsculas. */
    public Optional<Usuario> buscarPorUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        String buscado = username.trim();
        // POO - Colecciones: recorrido secuencial del arreglo de usuarios.
        for (Usuario usuario : listar()) {
            if (usuario.getUsername().equalsIgnoreCase(buscado)) {
                return Optional.of(usuario);
            }
        }
        return Optional.empty();
    }

    public int contarAdministradoresActivos() {
        int total = 0;
        for (Usuario usuario : listar()) {
            if (usuario.getRol() == Rol.ADMINISTRADOR && usuario.isActivo()) {
                total++;
            }
        }
        return total;
    }

    // --- Formato del archivo ---
    // POO - Polimorfismo: sobrescritura (@Override) de los métodos abstractos de RepositorioCsv.

    @Override
    protected String encabezado() {
        return "codigo;rol;nombre;telefono;correo;username;passwordHash;salt;activo;debeCambiarPassword;modulos";
    }

    @Override
    protected int cantidadCampos() {
        return 11;
    }

    @Override
    protected Usuario desdeCsv(String[] c) {
        Rol rol = CamposCsv.enumeracion(Rol.class, c[1], "rol");
        String hash = Validador.requerido(c[6], "passwordHash");
        String salt = Validador.requerido(c[7], "salt");
        boolean activo = CamposCsv.booleano(c[8], "activo");
        boolean debeCambiar = CamposCsv.booleano(c[9], "debeCambiarPassword");

        // POO - Polimorfismo: según el rol se crea un Administrador o un Empleado,
        // pero el método devuelve el tipo general Usuario.
        return switch (rol) {
            case ADMINISTRADOR ->
                new Administrador(c[0], c[2], c[3], c[4], c[5], hash, salt, activo, debeCambiar);
            case EMPLEADO ->
                new Empleado(c[0], c[2], c[3], c[4], c[5], hash, salt, activo, debeCambiar, leerModulos(c[10]));
        };
    }

    @Override
    protected String aCsv(Usuario u) {
        String modulos = "";
        if (u instanceof Empleado empleado) {
            modulos = empleado.getModulos().stream()
                    .map(Modulo::name)
                    .collect(Collectors.joining(SEPARADOR_MODULOS));
        }
        return String.join(";",
                u.getCodigo(), u.getRol().name(), u.getNombre(), u.getTelefono(), u.getCorreo(),
                u.getUsername(), u.getPasswordHash(), u.getSalt(),
                String.valueOf(u.isActivo()), String.valueOf(u.isDebeCambiarPassword()), modulos);
    }

    @Override
    protected Usuario[] crearArreglo(int tamanio) {
        return new Usuario[tamanio];
    }

    private Set<Modulo> leerModulos(String texto) {
        Set<Modulo> modulos = EnumSet.noneOf(Modulo.class);
        if (texto != null && !texto.isBlank()) {
            for (String nombre : texto.split("\\|")) {
                modulos.add(CamposCsv.enumeracion(Modulo.class, nombre, "modulos"));
            }
        }
        return modulos;
    }
}
