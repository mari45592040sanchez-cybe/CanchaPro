package canchapro.util;

import java.util.function.Function;

/**
 * Opción de una lista desplegable de filtro: un valor concreto, o "todos"
 * cuando {@code valor} es null.
 *
 * @param <T>   tipo del valor
 * @param valor valor filtrado, o null para no filtrar
 * @param texto texto que se muestra
 */
// POO - Reutilización: clase genérica; sirve para filtrar por cancha, por estado o por cualquier otro tipo.
public record OpcionFiltro<T>(T valor, String texto) {

    /** Crea la opción "todos" seguida de una opción por cada valor. */
    // POO - Colecciones: arreglo de opciones; la posición 0 es "todos".
    @SuppressWarnings("unchecked") // Java no permite new OpcionFiltro<T>[]; se crea sin tipo y se convierte.
    public static <T> OpcionFiltro<T>[] arreglo(String textoTodos, T[] valores, Function<T, String> texto) {
        OpcionFiltro<T>[] opciones = (OpcionFiltro<T>[]) new OpcionFiltro[valores.length + 1];
        opciones[0] = new OpcionFiltro<>(null, textoTodos);
        for (int i = 0; i < valores.length; i++) {
            opciones[i + 1] = new OpcionFiltro<>(valores[i], texto.apply(valores[i]));
        }
        return opciones;
    }

    @Override
    public String toString() {
        return texto;
    }
}
