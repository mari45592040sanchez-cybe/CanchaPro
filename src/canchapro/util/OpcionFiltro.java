package canchapro.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Opción de una lista desplegable de filtro: un valor concreto, o "todos"
 * cuando {@code valor} es null.
 *
 * @param <T>   tipo del valor
 * @param valor valor filtrado, o null para no filtrar
 * @param texto texto que se muestra
 */
public record OpcionFiltro<T>(T valor, String texto) {

    /** Crea la opción "todos" seguida de una opción por cada valor. */
    public static <T> List<OpcionFiltro<T>> lista(String textoTodos, List<T> valores, Function<T, String> texto) {
        List<OpcionFiltro<T>> opciones = new ArrayList<>();
        opciones.add(new OpcionFiltro<>(null, textoTodos));
        for (T v : valores) {
            opciones.add(new OpcionFiltro<>(v, texto.apply(v)));
        }
        return opciones;
    }

    @Override
    public String toString() {
        return texto;
    }
}
