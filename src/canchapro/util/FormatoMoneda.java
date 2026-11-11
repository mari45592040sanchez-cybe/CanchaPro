package canchapro.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class FormatoMoneda {

    private static final DecimalFormatSymbols SIMBOLOS = DecimalFormatSymbols.getInstance(Locale.US);

    private FormatoMoneda() {
    }

    public static String formatear(BigDecimal monto) {
        if (monto == null) {
            return "";
        }
        DecimalFormat formato = new DecimalFormat("#,##0.00", SIMBOLOS);
        return "Q " + formato.format(monto.setScale(2, RoundingMode.HALF_UP));
    }
}
