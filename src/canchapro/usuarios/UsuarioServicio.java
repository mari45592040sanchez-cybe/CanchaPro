package canchapro.usuarios;

import canchapro.util.GeneradorCodigo;
import canchapro.util.SeguridadPassword;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.util.Arrays;
import java.util.Comparator;

// POO - Responsabilidad: reglas de negocio de usuarios (no repetir username, siempre un admin activo...).
public class UsuarioServicio {

    public static final String USERNAME_INICIAL = "admin";
    public static final String PASSWORD_INICIAL = "admin123";
    private static final String PREFIJO_CODIGO = "USR-";
    private static final int LARGO_PASSWORD_TEMPORAL = 8;

    // POO - Relaciones (composición): el servicio TIENE un repositorio, que recibe por el constructor.
    private final UsuarioRepositorio repositorio;

    public UsuarioServicio(UsuarioRepositorio repositorio) {
        this.repositorio = repositorio;
    }

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

    public Usuario[] listar() {
        // POO - Colecciones: el repositorio entrega una copia del arreglo; se ordena sin afectar el original.
        Usuario[] usuarios = repositorio.listar();
        Arrays.sort(usuarios, Comparator.comparing(Usuario::getCodigo));
        return usuarios;
    }

    public Usuario obtener(String codigo) {
        return repositorio.buscar(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el usuario " + codigo));
    }

    public Usuario registrar(Rol rol, String nombre, String telefono, String correo,
            String username, String passwordInicial, Modulo[] modulos) {
        if (rol == null) {
            throw new ReglaNegocioException("Seleccione el rol del usuario.");
        }
        String usernameValido = Usuario.validarUsername(username);
        validarUsernameDisponible(usernameValido, null);
        Usuario.validarPassword(passwordInicial);

        String codigo = siguienteCodigo();
        // POO - Polimorfismo: la variable es de tipo Usuario, pero el objeto real es Administrador o Empleado.
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

    public Usuario actualizar(String codigo, String nombre, String telefono, String correo,
            String username, Modulo[] modulos) {
        Usuario usuario = obtener(codigo);

        String nombreValido = Persona.validarNombre(nombre);
        String telefonoValido = Persona.validarTelefono(telefono);
        String correoValido = Persona.validarCorreo(correo);
        String usernameValido = Usuario.validarUsername(username);
        validarUsernameDisponible(usernameValido, codigo);
        Modulo[] modulosValidos = copiar(modulos);
        if (usuario instanceof Empleado) {
            for (Modulo m : modulosValidos) {
                if (m.isSoloAdministrador()) {
                    throw new ReglaNegocioException("El módulo '" + m + "' es exclusivo del administrador.");
                }
            }
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

    private static Modulo[] copiar(Modulo[] modulos) {
        return modulos == null ? new Modulo[0] : Arrays.copyOf(modulos, modulos.length);
    }
}
