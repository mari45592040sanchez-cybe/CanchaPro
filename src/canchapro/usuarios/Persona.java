package canchapro.usuarios;

import canchapro.util.Identificable;
import canchapro.util.Validador;
import canchapro.util.excepciones.DatosInvalidosException;

// POO - Herencia: clase base (abstracta) de Cliente y Usuario; contiene los datos comunes de una persona.
// POO - Polimorfismo: implementa la interfaz Identificable (toda entidad tiene un código).
public abstract class Persona implements Identificable {

    // POO - Encapsulamiento: atributos privados; solo se cambian con los setters, que validan los datos.
    private final String codigo;
    private String nombre;
    private String telefono;
    private String correo;

    protected Persona(String codigo, String nombre, String telefono, String correo) {
        this.codigo = Validador.requerido(codigo, "El código");
        setNombre(nombre);
        setTelefono(telefono);
        setCorreo(correo);
    }

    // POO - Reutilización: validaciones estáticas que usan Cliente, Usuario y los servicios.
    public static String validarNombre(String nombre) {
        return Validador.texto(nombre, 3, 80, "El nombre");
    }

    public static String validarTelefono(String telefono) {
        return validarTelefono(telefono, false);
    }

    public static String validarTelefono(String telefono, boolean obligatorio) {
        String limpio = obligatorio
                ? Validador.requerido(telefono, "El teléfono")
                : Validador.opcional(telefono);
        if (!limpio.isEmpty() && !Validador.telefonoValido(limpio)) {
            throw new DatosInvalidosException("El teléfono debe tener 8 dígitos (sin guiones ni espacios).");
        }
        return limpio;
    }

    public static String validarCorreo(String correo) {
        String limpio = Validador.opcional(correo);
        if (!limpio.isEmpty() && !Validador.correoValido(limpio)) {
            throw new DatosInvalidosException("El correo electrónico no tiene un formato válido.");
        }
        Validador.sinSeparadorCsv(limpio, "El correo");
        return limpio;
    }

    // --- Getters y setters ---

    @Override
    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    // POO - Encapsulamiento: el setter valida antes de asignar; nunca queda un nombre inválido.
    public final void setNombre(String nombre) {
        this.nombre = validarNombre(nombre);
    }

    public String getTelefono() {
        return telefono;
    }

    public final void setTelefono(String telefono) {
        this.telefono = validarTelefono(telefono, telefonoObligatorio());
    }

    // POO - Polimorfismo: las subclases pueden sobrescribirlo (Cliente devuelve true).
    protected boolean telefonoObligatorio() {
        return false;
    }

    public String getCorreo() {
        return correo;
    }

    public final void setCorreo(String correo) {
        this.correo = validarCorreo(correo);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
