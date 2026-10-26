package canchapro.reservas;

import canchapro.servicios.ServicioAdicional;

/**
 * Servicio adicional que el cliente pide agregar a la reserva.
 *
 * @param servicio servicio del catálogo
 * @param cantidad unidades
 */
// POO - Encapsulamiento: record inmutable (servicio + cantidad elegidos en pantalla).
public record SolicitudServicio(ServicioAdicional servicio, int cantidad) {
}
