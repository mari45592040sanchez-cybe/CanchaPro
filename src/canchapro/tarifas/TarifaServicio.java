package canchapro.tarifas;

import canchapro.canchas.TipoCancha;
import canchapro.util.GeneradorCodigo;
import canchapro.util.excepciones.DatosInvalidosException;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;

// POO - Responsabilidad: es la única clase que calcula precios; ninguna pantalla los escribe a mano.
public class TarifaServicio {

    private static final String PREFIJO_CODIGO = "TAR-";
    private static final BigDecimal MINUTOS_POR_HORA = BigDecimal.valueOf(60);
    private static final int SEGUNDOS_POR_DIA = 24 * 60 * 60;

    // POO - Relaciones (composición): el servicio TIENE un repositorio, que recibe por el constructor.
    private final TarifaRepositorio repositorio;

    public TarifaServicio(TarifaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public boolean asegurarDatosIniciales() {
        if (!repositorio.estaVacio()) {
            return false;
        }
        repositorio.agregar(new Tarifa("TAR-001", TipoDia.LUNES_VIERNES, LocalTime.of(8, 0), LocalTime.of(17, 0),
                new BigDecimal("150"), null, true));
        repositorio.agregar(new Tarifa("TAR-002", TipoDia.LUNES_VIERNES, LocalTime.of(17, 0), LocalTime.of(23, 0),
                new BigDecimal("225"), null, true));
        repositorio.agregar(new Tarifa("TAR-003", TipoDia.SABADO_DOMINGO, LocalTime.of(8, 0), LocalTime.of(23, 0),
                new BigDecimal("250"), null, true));
        repositorio.guardar();
        return true;
    }

    public Tarifa buscarTarifa(LocalDate fecha, LocalTime hora, TipoCancha tipoCancha) {
        Tarifa general = null;
        // POO - Colecciones: recorrido secuencial del arreglo de tarifas.
        for (Tarifa t : repositorio.listar()) {
            if (t.aplica(fecha, hora, tipoCancha)) {
                if (t.esEspecifica()) {
                    return t;
                }
                if (general == null) {
                    general = t;
                }
            }
        }
        if (general == null) {
            throw new ReglaNegocioException("No hay tarifa configurada para "
                    + TipoDia.de(fecha).toString().toLowerCase() + " a las " + hora
                    + (tipoCancha == null ? "" : " (" + tipoCancha + ")")
                    + ". Revise la configuración de tarifas.");
        }
        return general;
    }

    public DetallePrecio calcular(LocalDate fecha, LocalTime inicio, Duration duracion, TipoCancha tipoCancha) {
        if (fecha == null || inicio == null || duracion == null || tipoCancha == null) {
            throw new DatosInvalidosException("Indique fecha, hora de inicio, duración y tipo de cancha.");
        }
        if (duracion.isNegative() || duracion.isZero()) {
            throw new DatosInvalidosException("La duración debe ser mayor que cero.");
        }
        if (inicio.toSecondOfDay() + duracion.toSeconds() > SEGUNDOS_POR_DIA - 60) {
            throw new DatosInvalidosException("La reserva no puede terminar después de las 23:59.");
        }
        return calcular(fecha, inicio, inicio.plus(duracion), tipoCancha);
    }

    public DetallePrecio calcular(LocalDate fecha, LocalTime inicio, LocalTime fin, TipoCancha tipoCancha) {
        if (!inicio.isBefore(fin)) {
            throw new DatosInvalidosException("La hora final debe ser posterior a la hora de inicio.");
        }
        if (inicio.getMinute() % Tarifa.MINUTOS_BLOQUE != 0 || fin.getMinute() % Tarifa.MINUTOS_BLOQUE != 0) {
            throw new DatosInvalidosException("Las horas deben ser en punto o y media.");
        }

        int bloques = (int) (Duration.between(inicio, fin).toMinutes() / Tarifa.MINUTOS_BLOQUE);
        TramoTarifa[] tramos = new TramoTarifa[bloques];
        int cantidad = 0;
        Tarifa tarifaTramo = null;
        LocalTime inicioTramo = inicio;
        LocalTime bloque = inicio;
        while (bloque.isBefore(fin)) {
            Tarifa tarifa = buscarTarifa(fecha, bloque, tipoCancha);
            if (tarifaTramo != null && tarifa != tarifaTramo) {
                tramos[cantidad] = crearTramo(inicioTramo, bloque, tarifaTramo);
                cantidad++;
                inicioTramo = bloque;
            }
            tarifaTramo = tarifa;
            bloque = bloque.plusMinutes(Tarifa.MINUTOS_BLOQUE);
        }
        tramos[cantidad] = crearTramo(inicioTramo, fin, tarifaTramo);
        cantidad++;

        //tarifa
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < cantidad; i++) {
            total = total.add(tramos[i].subtotal());
        }
        return new DetallePrecio(Arrays.copyOf(tramos, cantidad), total.setScale(2, RoundingMode.HALF_UP));
    }
//fin
    
    private static TramoTarifa crearTramo(LocalTime desde, LocalTime hasta, Tarifa tarifa) {
        int minutos = (int) Duration.between(desde, hasta).toMinutes();
        BigDecimal subtotal = tarifa.getPrecioHora()
                .multiply(BigDecimal.valueOf(minutos))
                .divide(MINUTOS_POR_HORA, 2, RoundingMode.HALF_UP);
        return new TramoTarifa(desde, hasta, minutos, tarifa.getPrecioHora(), subtotal);
    }

    public Tarifa[] listar() {
        Tarifa[] tarifas = repositorio.listar();
        Arrays.sort(tarifas, Comparator.comparing(Tarifa::getTipoDia)
                .thenComparing(t -> t.getTipoCancha() == null ? -1 : t.getTipoCancha().ordinal())
                .thenComparing(Tarifa::getHoraDesde));
        return tarifas;
    }

    public Tarifa obtener(String codigo) {
        return repositorio.buscar(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe la tarifa " + codigo));
    }

    public Tarifa registrar(TipoDia tipoDia, LocalTime desde, LocalTime hasta, BigDecimal precioHora,
            TipoCancha tipoCancha) {
        String codigo = GeneradorCodigo.siguiente(PREFIJO_CODIGO, 3, repositorio.codigos());
        Tarifa tarifa = new Tarifa(codigo, tipoDia, desde, hasta, precioHora, tipoCancha, true);
        validarSinTraslape(tarifa);
        repositorio.agregar(tarifa);
        repositorio.guardar();
        return tarifa;
    }

    public Tarifa actualizar(String codigo, TipoDia tipoDia, LocalTime desde, LocalTime hasta,
            BigDecimal precioHora, TipoCancha tipoCancha) {
        Tarifa tarifa = obtener(codigo);
        Tarifa propuesta = new Tarifa(codigo, tipoDia, desde, hasta, precioHora, tipoCancha, tarifa.isActiva());
        validarSinTraslape(propuesta);
        tarifa.actualizarDatos(tipoDia, desde, hasta, precioHora, tipoCancha);
        repositorio.actualizar(tarifa);
        repositorio.guardar();
        return tarifa;
    }

    public void cambiarEstado(String codigo, boolean activa) {
        Tarifa tarifa = obtener(codigo);
        if (tarifa.isActiva() == activa) {
            return;
        }
        if (activa) {
            validarSinTraslape(tarifa);
        }
        tarifa.setActiva(activa);
        repositorio.actualizar(tarifa);
        repositorio.guardar();
    }

    private void validarSinTraslape(Tarifa tarifa) {
        for (Tarifa otra : repositorio.listar()) {
            if (otra.isActiva() && !otra.getCodigo().equals(tarifa.getCodigo()) && tarifa.seTraslapaCon(otra)) {
                throw new ReglaNegocioException("La franja choca con la tarifa " + otra.getCodigo() + ": "
                        + otra.getDescripcion() + ".");
            }
        }
    }
}
