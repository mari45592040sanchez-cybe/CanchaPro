package canchapro.reservas;

import canchapro.usuarios.Usuario;
import canchapro.util.Validador;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Datos de la cancelación de una reserva: cuándo, por qué y quién la canceló.
 *
 * @param fechaHora momento de la cancelación
 * @param motivo    motivo (10 a 150 caracteres)
 * @param usuario   usuario responsable
 */
// POO - Encapsulamiento: record inmutable. POO - Relaciones: guarda el usuario que canceló.
public record Cancelacion(LocalDateTime fechaHora, String motivo, Usuario usuario) {

    public Cancelacion {
        Objects.requireNonNull(fechaHora, "La fecha de cancelación es obligatoria");
        Objects.requireNonNull(usuario, "El usuario que cancela es obligatorio");
        motivo = Validador.texto(motivo, 10, 150, "El motivo de la cancelación");
    }
}
