package canchapro.util.excepciones;

/**
 * Se lanza cuando el horario solicitado se traslapa con una reserva existente.
 */
public class ConflictoHorarioException extends ReglaNegocioException {

    public ConflictoHorarioException(String mensaje) {
        super(mensaje);
    }
}
