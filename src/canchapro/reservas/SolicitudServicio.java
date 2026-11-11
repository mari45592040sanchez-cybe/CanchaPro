package canchapro.reservas;

import canchapro.servicios.ServicioAdicional;

// POO - Encapsulamiento: record inmutable (servicio + cantidad elegidos en pantalla).
public record SolicitudServicio(ServicioAdicional servicio, int cantidad) {
}
