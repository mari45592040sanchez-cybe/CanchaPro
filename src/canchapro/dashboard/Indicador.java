package canchapro.dashboard;

import java.util.function.Supplier;

// POO - Encapsulamiento: record inmutable. POO - Reutilización: cualquier módulo opcional puede aportar
// tarjetas al inicio sin que el dashboard dependa de él.
public record Indicador(String titulo, Supplier<String> valor) {
}
