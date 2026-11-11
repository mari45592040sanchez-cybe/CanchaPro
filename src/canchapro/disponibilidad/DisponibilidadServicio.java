package canchapro.disponibilidad;

import canchapro.canchas.Cancha;
import canchapro.canchas.CanchaServicio;
import canchapro.canchas.EstadoCancha;
import canchapro.canchas.TipoCancha;
import canchapro.tarifas.DetallePrecio;
import canchapro.tarifas.TarifaServicio;
import canchapro.util.BloquesHorario;
import canchapro.util.FormatoFecha;
import canchapro.util.excepciones.CanchaProException;
import canchapro.util.excepciones.ConflictoHorarioException;
import canchapro.util.excepciones.DatosInvalidosException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

// POO - Responsabilidad: es el único que decide si una cancha está disponible; el operador nunca lo decide.
public class DisponibilidadServicio {

    // POO - Relaciones (composición): usa los servicios de canchas y tarifas, y una fuente de ocupación.
    private final CanchaServicio canchaServicio;
    private final TarifaServicio tarifaServicio;
    private final FuenteOcupacion fuenteOcupacion;
    private final Clock reloj;

    public DisponibilidadServicio(CanchaServicio canchaServicio, TarifaServicio tarifaServicio,
            FuenteOcupacion fuenteOcupacion) {
        this(canchaServicio, tarifaServicio, fuenteOcupacion, Clock.systemDefaultZone());
    }

    public DisponibilidadServicio(CanchaServicio canchaServicio, TarifaServicio tarifaServicio,
            FuenteOcupacion fuenteOcupacion, Clock reloj) {
        this.canchaServicio = canchaServicio;
        this.tarifaServicio = tarifaServicio;
        this.fuenteOcupacion = fuenteOcupacion;
        this.reloj = reloj;
    }

    public void validarSolicitud(SolicitudDisponibilidad s) {
        if (s == null || s.fecha() == null || s.horaInicio() == null || s.duracion() == null) {
            throw new DatosInvalidosException("Indique fecha, hora de inicio y duración.");
        }
        Duration d = s.duracion();
        if (d.compareTo(BloquesHorario.DURACION_MINIMA) < 0 || d.compareTo(BloquesHorario.DURACION_MAXIMA) > 0
                || d.toMinutes() % BloquesHorario.MINUTOS_BLOQUE != 0) {
            throw new DatosInvalidosException("La duración debe ser de " + BloquesHorario.describir(BloquesHorario.DURACION_MINIMA)
                    + " a " + BloquesHorario.describir(BloquesHorario.DURACION_MAXIMA) + ", en bloques de 30 minutos.");
        }
        if (s.horaInicio().getMinute() % BloquesHorario.MINUTOS_BLOQUE != 0 || s.horaInicio().getSecond() != 0) {
            throw new DatosInvalidosException("La hora de inicio debe ser en punto o y media.");
        }
        if (s.horaInicio().toSecondOfDay() + d.toSeconds() >= Duration.ofDays(1).toSeconds()) {
            throw new DatosInvalidosException("La reserva no puede terminar después de la medianoche.");
        }
        if (!LocalDateTime.of(s.fecha(), s.horaInicio()).isAfter(ahora())) {
            throw new ReglaNegocioException("No se puede reservar en una fecha u hora que ya pasó ("
                    + FormatoFecha.vista(LocalDateTime.of(s.fecha(), s.horaInicio())) + ").");
        }
    }

    public ResultadoDisponibilidad[] consultar(SolicitudDisponibilidad solicitud) {
        validarSolicitud(solicitud);
        Cancha[] canchas = canchaServicio.listarPorTipo(solicitud.tipoCancha());
        // POO - Colecciones: un resultado por cada cancha, en un arreglo del mismo tamaño.
        ResultadoDisponibilidad[] resultados = new ResultadoDisponibilidad[canchas.length];
        for (int i = 0; i < canchas.length; i++) {
            Cancha cancha = canchas[i];
            Optional<String> motivo = motivoNoDisponible(cancha, solicitud, null).map(Impedimento::mensaje);
            if (motivo.isPresent()) {
                resultados[i] = ResultadoDisponibilidad.noDisponible(cancha, motivo.get());
                continue;
            }
            try {
                resultados[i] = ResultadoDisponibilidad.disponible(cancha, calcularPrecio(cancha, solicitud));
            } catch (CanchaProException e) {
                resultados[i] = ResultadoDisponibilidad.noDisponible(cancha, e.getMessage());
            }
        }
        return resultados;
    }

    public ResultadoDisponibilidad[] listarDisponibles(SolicitudDisponibilidad solicitud) {
        ResultadoDisponibilidad[] todos = consultar(solicitud);
        ResultadoDisponibilidad[] disponibles = new ResultadoDisponibilidad[todos.length];
        int cantidad = 0;
        for (ResultadoDisponibilidad r : todos) {
            if (r.disponible()) {
                disponibles[cantidad] = r;
                cantidad++;
            }
        }
        return Arrays.copyOf(disponibles, cantidad);
    }

    public void verificarDisponible(Cancha cancha, SolicitudDisponibilidad solicitud, String referenciaExcluir) {
        validarSolicitud(solicitud);
        Optional<Impedimento> impedimento = motivoNoDisponible(cancha, solicitud, referenciaExcluir);
        if (impedimento.isPresent()) {
            String mensaje = cancha.getCodigo() + " " + cancha.getNombre() + ": " + impedimento.get().mensaje();
            throw impedimento.get().esConflicto()
                    ? new ConflictoHorarioException(mensaje)
                    : new ReglaNegocioException(mensaje);
        }
    }

    public DetallePrecio calcularPrecio(Cancha cancha, SolicitudDisponibilidad solicitud) {
        return tarifaServicio.calcular(solicitud.fecha(), solicitud.horaInicio(), solicitud.duracion(), cancha.getTipo());
    }

    public GrillaOcupacion ocupacionDelDia(LocalDate fecha, TipoCancha tipo, SolicitudDisponibilidad resaltar) {
        Cancha[] delTipo = canchaServicio.listarPorTipo(tipo);
        Cancha[] canchas = new Cancha[delTipo.length];
        int cantidad = 0;
        for (Cancha c : delTipo) {
            if (c.getEstado() != EstadoCancha.INACTIVA) {
                canchas[cantidad] = c;
                cantidad++;
            }
        }
        return new GrillaOcupacion(fecha, Arrays.copyOf(canchas, cantidad),
                c -> fuenteOcupacion.ocupaciones(c.getCodigo(), fecha), ahora(), resaltar);
    }

    private record Impedimento(String mensaje, boolean esConflicto) {
    }

    private Optional<Impedimento> motivoNoDisponible(Cancha cancha, SolicitudDisponibilidad s, String excluir) {
        if (!cancha.permiteReservas()) {
            return Optional.of(new Impedimento("cancha " + cancha.getEstado().toString().toLowerCase() + ".", false));
        }
        if (!cancha.dentroDeHorario(s.horaInicio(), s.horaFin())) {
            return Optional.of(new Impedimento("fuera del horario de atención (" + cancha.getHorario() + ").", false));
        }
        IntervaloHorario solicitado = s.intervalo();
        // POO - Polimorfismo: fuenteOcupacion puede ser cualquier implementación de FuenteOcupacion.
        for (Ocupacion o : fuenteOcupacion.ocupaciones(cancha.getCodigo(), s.fecha())) {
            if (!Objects.equals(o.referencia(), excluir) && o.intervalo().seTraslapaCon(solicitado)) {
                return Optional.of(new Impedimento("ocupada de " + o.intervalo().describirHoras() + " por "
                        + o.referencia() + (o.descripcion().isBlank() ? "" : " (" + o.descripcion() + ")") + ".", true));
            }
        }
        return Optional.empty();
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj).withSecond(0).withNano(0);
    }

    public LocalTime horaActual() {
        return ahora().toLocalTime();
    }
}
