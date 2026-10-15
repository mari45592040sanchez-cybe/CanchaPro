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
        return listar().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(buscado))
                .findFirst();
    }

    public long contarAdministradoresActivos() {
        return listar().stream()
                .filter(u -> u.getRol() == Rol.ADMINISTRADOR && u.isActivo())
                .count();
    }

    // --- Formato del archivo ---

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
