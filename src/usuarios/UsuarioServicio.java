package canchapro.usuarios;

import canchapro.util.GeneradorCodigo;
import canchapro.util.SeguridadPassword;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reglas de negocio para la gestión de usuarios (administradores y empleados).
 */
public class UsuarioServicio {

    public static final String USERNAME_INICIAL = "admin";
    public static final String PASSWORD_INICIAL = "admin123";
    private static final String PREFIJO_CODIGO = "USR-";
    private static final int LARGO_PASSWORD_TEMPORAL = 8;

    private final UsuarioRepositorio repositorio;

    public UsuarioServicio(UsuarioRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Garantiza que exista al menos un administrador activo. Si no hay
     * ninguno (primer arranque), crea el usuario admin / admin123 y le
     * obliga a cambiar la contraseña en el primer ingreso.
     *
     * @return true si se creó el administrador inicial
     */
    public boolean asegurarAdministrador() {
        if (repositorio.contarAdministradoresActivos() > 0) {
            return false;
        }
        String username = USERNAME_INICIAL;
        int sufijo = 1;
        while (repositorio.buscarPorUsername(username).isPresent()) {
            username = USERNAME_INICIAL + sufijo++;
        }
        Administrador admin = new Administrador(siguienteCodigo(), "Administrador", "", "",
                username, "", "", true, true);
        admin.establecerPassword(PASSWORD_INICIAL, true);
        repositorio.agregar(admin);
        repositorio.guardar();
        return true;
    }

    public List<Usuario> listar() {
        return repositorio.listar().stream()
                .sorted(Comparator.comparing(Usuario::getCodigo))
                .toList();
    }

    public Usuario obtener(String codigo) {
        return repositorio.buscar(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el usuario " + codigo));
    }

    /**
     * Registra un nuevo usuario. La contraseña inicial deberá cambiarse en
     * el primer ingreso.
     *
     * @param modulos módulos visibles (solo aplica a empleados)
     */
    public Usuario registrar(Rol rol, String nombre, String telefono, String correo,
            String username, String passwordInicial, Set<Modulo> modulos) {
        if (rol == null) {
            throw new ReglaNegocioException("Seleccione el rol del usuario.");
        }
        String usernameValido = Usuario.validarUsername(username);
        validarUsernameDisponible(usernameValido, null);
        Usuario.validarPassword(passwordInicial);

        String codigo = siguienteCodigo();
        Usuario usuario = switch (rol) {
            case ADMINISTRADOR ->
                new Administrador(codigo, nombre, telefono, correo, usernameValido, "", "", true, true);
            case EMPLEADO ->
                new Empleado(codigo, nombre, telefono, correo, usernameValido, "", "", true, true, copiar(modulos));
        };
        usuario.establecerPassword(passwordInicial, true);
        repositorio.agregar(usuario);
        repositorio.guardar();
        return usuario;
    }

    /**
     * Actualiza datos personales, nombre de usuario y, si es empleado, sus
     * módulos. El rol no se puede cambiar después de creado el usuario.
     * Todas las validaciones se hacen antes de modificar el objeto, para no
     * dejarlo a medio actualizar si algún dato es inválido.
     */
    public Usuario actualizar(String codigo, String nombre, String telefono, String correo,
            String username, Set<Modulo> modulos) {
        Usuario usuario = obtener(codigo);

        String nombreValido = Persona.validarNombre(nombre);
        String telefonoValido = Persona.validarTelefono(telefono);
        String correoValido = Persona.validarCorreo(correo);
        String usernameValido = Usuario.validarUsername(username);
        validarUsernameDisponible(usernameValido, codigo);
        Set<Modulo> modulosValidos = copiar(modulos);
        if (usuario instanceof Empleado) {
            modulosValidos.stream()
                    .filter(Modulo::isSoloAdministrador)
                    .findFirst()
                    .ifPresent(m -> {
                        throw new ReglaNegocioException("El módulo '" + m + "' es exclusivo del administrador.");
                    });
        }

        usuario.setNombre(nombreValido);
        usuario.setTelefono(telefonoValido);
        usuario.setCorreo(correoValido);
        usuario.setUsername(usernameValido);
        if (usuario instanceof Empleado empleado) {
            empleado.asignarModulos(modulosValidos);
        }
        repositorio.actualizar(usuario);
        repositorio.guardar();
        return usuario;
    }

    /**
     * Activa o desactiva un usuario. Los usuarios nunca se eliminan.
     *
     * @param responsable usuario que realiza la operación
     */
    public void cambiarEstado(String codigo, boolean activo, Usuario responsable) {
        Usuario usuario = obtener(codigo);
        if (usuario.isActivo() == activo) {
            return;
        }
        if (!activo) {
            if (usuario.getCodigo().equals(responsable.getCodigo())) {
                throw new ReglaNegocioException("No puede desactivar su propio usuario.");
            }
            if (usuario.getRol() == Rol.ADMINISTRADOR && repositorio.contarAdministradoresActivos() <= 1) {
                throw new ReglaNegocioException("Debe existir al menos un administrador activo.");
            }
        }
        usuario.setActivo(activo);
        repositorio.actualizar(usuario);
        repositorio.guardar();
    }

    /**
     * Genera una contraseña temporal que el usuario deberá cambiar al ingresar.
     *
     * @return la contraseña temporal, para entregarla al usuario
     */
    public String restablecerPassword(String codigo) {
        Usuario usuario = obtener(codigo);
        String temporal = SeguridadPassword.generarTemporal(LARGO_PASSWORD_TEMPORAL);
        usuario.establecerPassword(temporal, true);
        repositorio.actualizar(usuario);
        repositorio.guardar();
        return temporal;
    }

    private void validarUsernameDisponible(String username, String codigoPropio) {
        repositorio.buscarPorUsername(username)
                .filter(existente -> !existente.getCodigo().equals(codigoPropio))
                .ifPresent(existente -> {
                    throw new ReglaNegocioException("El nombre de usuario '" + username + "' ya está en uso.");
                });
    }

    private String siguienteCodigo() {
        return GeneradorCodigo.siguiente(PREFIJO_CODIGO, 3, repositorio.codigos());
    }

    private static Set<Modulo> copiar(Set<Modulo> modulos) {
        return modulos == null || modulos.isEmpty() ? EnumSet.noneOf(Modulo.class) : EnumSet.copyOf(modulos);
    }
}
