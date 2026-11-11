package canchapro.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

public final class SelectorFecha {

    private SelectorFecha() {
    }

    public static void configurar(JSpinner spinner, LocalDate fechaInicial) {
        spinner.setModel(new SpinnerDateModel(aDate(fechaInicial), null, null, Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
    }

    public static LocalDate obtener(JSpinner spinner) {
        Date fecha = (Date) spinner.getValue();
        return fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static void establecer(JSpinner spinner, LocalDate fecha) {
        spinner.setValue(aDate(fecha));
    }

    private static Date aDate(LocalDate fecha) {
        return Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
