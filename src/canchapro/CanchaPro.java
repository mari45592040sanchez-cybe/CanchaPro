package canchapro;

import canchapro.login.FrmLogin;
import canchapro.persistencia.ErrorCarga;
import canchapro.persistencia.RegistroErroresCarga;
import canchapro.usuarios.UsuarioServicio;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada de CanchaPro.
 */
public class CanchaPro {

    private static final Path ARCHIVO_CONFIGURACION = Path.of("config", "canchapro.properties");
    private static final int MAX_ERRORES_MOSTRADOS = 10;

    public static void main(String[] args) {
        aplicarLookAndFeel();
        SwingUtilities.invokeLater(CanchaPro::iniciar);
    }

    private static void iniciar() {
        ContextoAplicacion contexto;
        try {
            contexto = new ContextoAplicacion(ARCHIVO_CONFIGURACION);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo iniciar el sistema:\n" + e.getMessage(),
                    "CanchaPro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        mostrarErroresCarga(contexto);

        FrmLogin login = new FrmLogin(contexto);
        login.setVisible(true);

        if (contexto.isAdministradorInicialCreado()) {
            JOptionPane.showMessageDialog(login,
                    "Se creó el usuario administrador inicial.\n\n"
                    + "Usuario: " + UsuarioServicio.USERNAME_INICIAL + "\n"
                    + "Contraseña: " + UsuarioServicio.PASSWORD_INICIAL + "\n\n"
                    + "Deberá cambiar la contraseña en el primer ingreso.",
                    "Primer inicio", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static void mostrarErroresCarga(ContextoAplicacion contexto) {
        RegistroErroresCarga registro = contexto.getRegistroErrores();
        if (!registro.hayErrores()) {
            return;
        }
        ErrorCarga[] errores = registro.getErrores();
        StringBuilder mensaje = new StringBuilder("Se encontraron ")
                .append(errores.length).append(" problema(s) al cargar los datos.\n")
                .append("Los registros con errores se omitieron; el resto se cargó normalmente.\n\n");
        for (int i = 0; i < errores.length && i < MAX_ERRORES_MOSTRADOS; i++) {
            mensaje.append("• ").append(errores[i]).append('\n');
        }
        if (errores.length > MAX_ERRORES_MOSTRADOS) {
            mensaje.append("… y ").append(errores.length - MAX_ERRORES_MOSTRADOS).append(" más.\n");
        }
        mensaje.append("\nDetalle en: ")
                .append(contexto.getCarpetaDatos().resolve(RegistroErroresCarga.ARCHIVO_LOG));
        if (registro.getRespaldos().length > 0) {
            mensaje.append("\nCopia de los archivos originales en: ").append(String.join(", ", registro.getRespaldos()));
        }
        JOptionPane.showMessageDialog(null, mensaje.toString(), "Errores al cargar datos", JOptionPane.WARNING_MESSAGE);
    }

    private static void aplicarLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            // Se mantiene el aspecto por defecto de Swing.
        }
    }
}
