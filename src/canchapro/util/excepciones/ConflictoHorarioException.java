package canchapro.util.excepciones;

// POO - Herencia: hereda de ReglaNegocioException (herencia de varios niveles).
public class ConflictoHorarioException extends ReglaNegocioException {

    public ConflictoHorarioException(String mensaje) {
        super(mensaje);
    }
}
