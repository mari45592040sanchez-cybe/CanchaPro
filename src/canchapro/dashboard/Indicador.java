package canchapro.dashboard;

import java.util.function.Supplier;

/**
 * Indicador adicional para la pantalla de inicio, aportado por un módulo
 * opcional (por ejemplo, la Tienda). Así el dashboard no depende de ese módulo.
 *
 * @param titulo título de la tarjeta
 * @param valor  calcula el valor a mostrar cada vez que se refresca
 */
// POO - Encapsulamiento: record inmutable. POO - Reutilización: cualquier módulo opcional puede aportar
// tarjetas al inicio sin que el dashboard dependa de él.
public record Indicador(String titulo, Supplier<String> valor) {
}
