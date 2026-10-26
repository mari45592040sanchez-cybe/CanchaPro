package canchapro.disponibilidad;

import canchapro.canchas.Cancha;
import canchapro.util.BloquesHorario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Vista de un día: filas = bloques de 30 minutos, columnas = canchas.
 * Cada celda indica si el bloque está libre, ocupado, cerrado, etc.
 * No depende de Swing: la pantalla solo dibuja lo que aquí se calcula.
 */
// POO - Responsabilidad: calcula la grilla; la pantalla solo la dibuja.
public class GrillaOcupacion {

    /** Estado de un bloque de 30 minutos en una cancha. */
    public enum TipoCelda {
        LIBRE("Libre"),
        OCUPADA("Ocupada"),
        CERRADA("Fuera de horario"),
        NO_DISPONIBLE("Cancha no disponible"),
        PASADA("Hora pasada");

        private final String nombreVisible;

        TipoCelda(String nombreVisible) {
            this.nombreVisible = nombreVisible;
        }

        @Override
        public String toString() {
            return nombreVisible;
        }
    }

    /**
     * @param tipo       estado del bloque
     * @param texto      texto a mostrar (código de reserva si está ocupada)
     * @param solicitada true si el bloque forma parte del horario consultado
     */
    public record Celda(TipoCelda tipo, String texto, boolean solicitada) {
    }

    // POO - Encapsulamiento: los arreglos son privados; se consultan con getters que devuelven copias.
    // POO - Colecciones: arreglos de canchas y horas, y una matriz (arreglo de dos dimensiones) de celdas.
    private final LocalDate fecha;
    private final Cancha[] canchas;
    private final LocalTime[] horas;
    private final Celda[][] celdas;

    GrillaOcupacion(LocalDate fecha, Cancha[] canchas, Function<Cancha, Ocupacion[]> ocupaciones,
            LocalDateTime ahora, SolicitudDisponibilidad resaltar) {
        this.fecha = fecha;
        this.canchas = Arrays.copyOf(canchas, canchas.length);
        this.horas = calcularHoras(canchas);
        this.celdas = new Celda[horas.length][canchas.length];

        for (int col = 0; col < canchas.length; col++) {
            Cancha cancha = canchas[col];
            Ocupacion[] ocupadas = ocupaciones.apply(cancha);
            for (int fila = 0; fila < horas.length; fila++) {
                LocalTime hora = horas[fila];
                LocalDateTime momento = fecha.atTime(hora);
                boolean solicitada = resaltar != null && resaltar.fecha().equals(fecha)
                        && resaltar.intervalo().contiene(momento);
                celdas[fila][col] = clasificar(cancha, hora, momento, ocupadas, ahora, solicitada);
            }
        }
    }

    private static Celda clasificar(Cancha cancha, LocalTime hora, LocalDateTime momento,
            Ocupacion[] ocupadas, LocalDateTime ahora, boolean solicitada) {
        if (!cancha.dentroDeHorario(hora, hora.plusMinutes(BloquesHorario.MINUTOS_BLOQUE))) {
            return new Celda(TipoCelda.CERRADA, "", solicitada);
        }
        if (!cancha.permiteReservas()) {
            return new Celda(TipoCelda.NO_DISPONIBLE, cancha.getEstado().toString(), solicitada);
        }
        for (Ocupacion o : ocupadas) {
            if (o.intervalo().contiene(momento)) {
                String texto = o.referencia() + (o.descripcion().isBlank() ? "" : " · " + o.descripcion());
                return new Celda(TipoCelda.OCUPADA, texto, solicitada);
            }
        }
        if (momento.isBefore(ahora)) {
            return new Celda(TipoCelda.PASADA, "", solicitada);
        }
        return new Celda(TipoCelda.LIBRE, "", solicitada);
    }

    /** Desde la apertura más temprana hasta el cierre más tardío de las canchas mostradas. */
    private static LocalTime[] calcularHoras(Cancha[] canchas) {
        if (canchas.length == 0) {
            return new LocalTime[0];
        }
        LocalTime desde = canchas[0].getHoraApertura();
        LocalTime hasta = canchas[0].getHoraCierre();
        for (Cancha c : canchas) {
            if (c.getHoraApertura().isBefore(desde)) {
                desde = c.getHoraApertura();
            }
            if (c.getHoraCierre().isAfter(hasta)) {
                hasta = c.getHoraCierre();
            }
        }
        // Bloques de 30 minutos en [desde, hasta)
        return BloquesHorario.horasEntre(desde, hasta.minusMinutes(BloquesHorario.MINUTOS_BLOQUE));
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Cancha[] getCanchas() {
        return Arrays.copyOf(canchas, canchas.length);
    }

    public int cantidadCanchas() {
        return canchas.length;
    }

    public Cancha getCancha(int columna) {
        return canchas[columna];
    }

    public int cantidadHoras() {
        return horas.length;
    }

    public LocalTime getHora(int fila) {
        return horas[fila];
    }

    public Celda getCelda(int fila, int columna) {
        return celdas[fila][columna];
    }
}
