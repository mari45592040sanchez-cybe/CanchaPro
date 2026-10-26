package canchapro.canchas;

/**
 * Opción para listas desplegables que filtran o aplican a un tipo de
 * cancha: un tipo concreto, o todos los tipos cuando {@code tipo} es null.
 *
 * @param tipo tipo de cancha, o null para todos
 */
// POO - Encapsulamiento: record inmutable. POO - Reutilización: lo usan Tarifas, Disponibilidad y Reservas.
public record OpcionTipoCancha(TipoCancha tipo) {

    /** "Todos los tipos", "Fútbol 5", "Fútbol 7", … */
    // POO - Colecciones: arreglo del tamaño exacto (todos los tipos + la opción "todos").
    public static OpcionTipoCancha[] opciones() {
        TipoCancha[] tipos = TipoCancha.values();
        OpcionTipoCancha[] opciones = new OpcionTipoCancha[tipos.length + 1];
        opciones[0] = new OpcionTipoCancha(null);
        for (int i = 0; i < tipos.length; i++) {
            opciones[i + 1] = new OpcionTipoCancha(tipos[i]);
        }
        return opciones;
    }

    // POO - Polimorfismo: sobrescribe toString() para mostrar el texto en el combo.
    @Override
    public String toString() {
        return tipo == null ? "Todos los tipos" : tipo.toString();
    }
}
