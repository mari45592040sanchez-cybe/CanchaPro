package canchapro.configuracion;

import canchapro.usuarios.Modulo;
import canchapro.util.Validador;
import canchapro.util.excepciones.DatosInvalidosException;

/**
 * Parámetros del sistema que se pueden cambiar sin recompilar
 * (se guardan en config/canchapro.properties).
 */
public class Configuracion {

    public static final boolean MODULO_TIENDA_DEFECTO = true;
    public static final String RUTA_DATOS_DEFECTO = "data";
    public static final int ANTICIPO_MINIMO_DEFECTO = 25;
    public static final String NOMBRE_NEGOCIO_DEFECTO = "Complejo Deportivo La 10";

    private boolean moduloTienda = MODULO_TIENDA_DEFECTO;
    private String rutaDatos = RUTA_DATOS_DEFECTO;
    private int anticipoMinimoPorcentaje = ANTICIPO_MINIMO_DEFECTO;
    private String nombreNegocio = NOMBRE_NEGOCIO_DEFECTO;

    /**
     * Indica si un módulo está disponible en esta instalación.
     * Por ahora solo la Tienda/Cafetería es opcional; el resto del sistema
     * (el núcleo de reservas) siempre está habilitado.
     */
    public boolean estaHabilitado(Modulo modulo) {
        return modulo != Modulo.TIENDA || moduloTienda;
    }

    public boolean isModuloTienda() {
        return moduloTienda;
    }

    public void setModuloTienda(boolean moduloTienda) {
        this.moduloTienda = moduloTienda;
    }

    public String getRutaDatos() {
        return rutaDatos;
    }

    public void setRutaDatos(String rutaDatos) {
        this.rutaDatos = Validador.requerido(rutaDatos, "La ruta de datos");
    }

    /** Porcentaje del total que debe pagarse para que una reserva quede CONFIRMADA. */
    public int getAnticipoMinimoPorcentaje() {
        return anticipoMinimoPorcentaje;
    }

    public void setAnticipoMinimoPorcentaje(int porcentaje) {
        if (porcentaje < 0 || porcentaje > 100) {
            throw new DatosInvalidosException("El anticipo mínimo debe estar entre 0 y 100.");
        }
        this.anticipoMinimoPorcentaje = porcentaje;
    }

    public String getNombreNegocio() {
        return nombreNegocio;
    }

    public void setNombreNegocio(String nombreNegocio) {
        this.nombreNegocio = Validador.texto(nombreNegocio, 3, 80, "El nombre del negocio");
    }
}
