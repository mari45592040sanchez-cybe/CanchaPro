package canchapro.util.excepciones;

/**
 * Se lanza cuando el horario solicitado se traslapa con una reserva existente.
 */
// POO - Herencia: hereda de ReglaNegocioException (herencia de varios niveles).
public class ConflictoHorarioException extends ReglaNegocioException {

    public ConflictoHorarioException(String mensaje) {
        super(mensaje);
    }
}
