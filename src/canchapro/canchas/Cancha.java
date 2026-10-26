package canchapro.canchas;

import canchapro.util.BloquesHorario;
import canchapro.util.Identificable;
import canchapro.util.Validador;
import canchapro.util.excepciones.DatosInvalidosException;
import java.time.Duration;
import java.time.LocalTime;

/**
 * Cancha del complejo. El precio no se guarda aquí: se calcula con la
 * configuración de tarifas según el tipo de cancha, la fecha y la hora.
 */
// POO - Polimorfismo (interfaz): implementa Identificable para poder guardarse en un RepositorioCsv.
public class Cancha implements Identificable {

    public static final int CAPACIDAD_MAXIMA = 50;
    /** Las horas de apertura y cierre van en bloques de 30 minutos, igual que las reservas. */
    public static final int MINUTOS_BLOQUE = BloquesHorario.MINUTOS_BLOQUE;
    private static final Duration HORARIO_MINIMO = Duration.ofHours(1);

    // POO - Encapsulamiento: atributos privados; solo se cambian con actualizarDatos(), que valida todo.
    private final String codigo;
    private String nombre;
    // POO - Relaciones: la cancha TIENE un tipo y un estado (enumeraciones).
    private TipoCancha tipo;
    private int capacidad;
    private EstadoCancha estado;
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private String descripcion;

    public Cancha(String codigo, String nombre, TipoCancha tipo, int capacidad, EstadoCancha estado,
            LocalTime horaApertura, LocalTime horaCierre, String descripcion) {
        this.codigo = Validador.requerido(codigo, "El código");
        actualizarDatos(nombre, tipo, capacidad, estado, horaApertura, horaCierre, descripcion);
    }

    /**
     * Valida todos los datos y, solo si todos son correctos, los asigna.
     * Así la cancha nunca queda modificada a medias.
     */
    public final void actualizarDatos(String nombre, TipoCancha tipo, int capacidad, EstadoCancha estado,
            LocalTime horaApertura, LocalTime horaCierre, String descripcion) {
        String nombreValido = Validador.texto(nombre, 3, 40, "El nombre de la cancha");
        if (tipo == null || estado == null) {
            throw new DatosInvalidosException("Seleccione el tipo y el estado de la cancha.");
        }
        validarCapacidad(capacidad);
        validarHorario(horaApertura, horaCierre);
        String descripcionValida = Validador.opcional(descripcion);
        Validador.longitud(descripcionValida, 0, 120, "La descripción");
        Validador.sinSeparadorCsv(descripcionValida, "La descripción");

        this.nombre = nombreValido;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.estado = estado;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
        this.descripcion = descripcionValida;
    }

    public static void validarCapacidad(int capacidad) {
        if (capacidad < 1 || capacidad > CAPACIDAD_MAXIMA) {
            throw new DatosInvalidosException("La capacidad debe estar entre 1 y " + CAPACIDAD_MAXIMA + " jugadores.");
        }
    }

    /**
     * El horario debe abrir antes de cerrar, durar al menos una hora y usar
     * bloques de 30 minutos (08:00, 08:30, …).
     */
    public static void validarHorario(LocalTime apertura, LocalTime cierre) {
        if (apertura == null || cierre == null) {
            throw new DatosInvalidosException("Las horas de apertura y cierre son obligatorias.");
        }
        if (!esBloqueValido(apertura) || !esBloqueValido(cierre)) {
            throw new DatosInvalidosException("Las horas de apertura y cierre deben ser en punto o y media (ej. 08:00, 08:30).");
        }
        if (!apertura.isBefore(cierre)) {
            throw new DatosInvalidosException("La hora de apertura debe ser anterior a la hora de cierre.");
        }
        if (Duration.between(apertura, cierre).compareTo(HORARIO_MINIMO) < 0) {
            throw new DatosInvalidosException("La cancha debe estar abierta al menos una hora.");
        }
    }

    private static boolean esBloqueValido(LocalTime hora) {
        return hora.getSecond() == 0 && hora.getNano() == 0 && hora.getMinute() % MINUTOS_BLOQUE == 0;
    }

    /** Indica si el intervalo [inicio, fin] cabe dentro del horario de la cancha. */
    public boolean dentroDeHorario(LocalTime inicio, LocalTime fin) {
        return inicio.isBefore(fin) && !inicio.isBefore(horaApertura) && !fin.isAfter(horaCierre);
    }

    public boolean permiteReservas() {
        return estado.permiteReservas();
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoCancha getTipo() {
        return tipo;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public EstadoCancha getEstado() {
        return estado;
    }

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** "08:00 - 23:00" */
    public String getHorario() {
        return horaApertura + " - " + horaCierre;
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + tipo + ")";
    }
}
