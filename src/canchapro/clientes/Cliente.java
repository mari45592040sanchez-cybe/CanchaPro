package canchapro.clientes;

import canchapro.usuarios.Persona;
import java.time.LocalDate;
import java.util.Objects;

// POO - Herencia: Cliente ES UNA Persona; hereda código, nombre, teléfono y correo con sus validaciones.
public class Cliente extends Persona {

    // POO - Encapsulamiento: atributos privados; se leen con getters y el estado se cambia con setActivo().
    private final LocalDate fechaRegistro;
    private boolean activo;

    public Cliente(String codigo, String nombre, String telefono, String correo,
            LocalDate fechaRegistro, boolean activo) {
        super(codigo, nombre, telefono, correo);
        this.fechaRegistro = Objects.requireNonNull(fechaRegistro, "La fecha de registro es obligatoria");
        this.activo = activo;
    }

    // POO - Polimorfismo: sobrescribe el método de Persona; para un cliente el teléfono es obligatorio.
    @Override
    protected boolean telefonoObligatorio() {
        return true;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getDescripcion() {
        return getCodigo() + " - " + getNombre() + " (" + getTelefono() + ")";
    }
}
