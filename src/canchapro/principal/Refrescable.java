package canchapro.principal;

/**
 * Pantalla que puede volver a leer sus datos. La ventana principal la
 * refresca al volver a mostrarla (los datos pudieron cambiar en otro
 * módulo) y cuando las reservas cambian de estado automáticamente.
 */
public interface Refrescable {

    void refrescar();
}
