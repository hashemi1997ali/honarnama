package ir.hashemi.market;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Legacy class name retained to avoid touching every call site.
 * Dates are now displayed in English using the Gregorian calendar.
 */
public final class ShamsiCalleder {

    private static final String INPUT_PATTERN = "yyyy/MM/dd";
    private static final String OUTPUT_PATTERN = "MMM d, yyyy";

    private ShamsiCalleder() {
    }

    public static String getCurrentShamsidate() {
        return new SimpleDateFormat(OUTPUT_PATTERN, Locale.US).format(new Date());
    }

    public static String getCurrentShamsidate(String value) {
        if (value == null || value.trim().isEmpty()) return "";

        SimpleDateFormat input = new SimpleDateFormat(INPUT_PATTERN, Locale.US);
        input.setLenient(false);
        try {
            Date date = input.parse(value.trim());
            return new SimpleDateFormat(OUTPUT_PATTERN, Locale.US).format(date);
        } catch (ParseException ignored) {
            return value;
        }
    }
}
