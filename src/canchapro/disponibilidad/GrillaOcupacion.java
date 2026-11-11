package canchapro.disponibilidad;

import canchapro.canchas.Cancha;
import canchapro.util.BloquesHorario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.function.Function;

// POO - Responsabilidad: calcula la grilla; la pantalla solo la dibuja.
public class GrillaOcupacion {

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
