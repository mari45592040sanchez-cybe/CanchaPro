package canchapro.reservas;

import canchapro.usuarios.Usuario;
import canchapro.util.Validador;
import java.time.LocalDateTime;
import java.util.Objects;

// POO - Encapsulamiento: record inmutable. POO - Relaciones: guarda el usuario que canceló.
public record Cancelacion(LocalDateTime fechaHora, String motivo, Usuario usuario) {

    public Cancelacion {
        Objects.requireNonNull(fechaHora, "La fecha de cancelación es obligatoria");
        Objects.requireNonNull(usuario, "El usuario que cancela es obligatorio");
        motivo = Validador.texto(motivo, 10, 150, "El motivo de la cancelación");
    }
}
