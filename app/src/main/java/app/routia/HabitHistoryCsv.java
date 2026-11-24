package app.routia;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;

final class HabitHistoryCsv {
    private HabitHistoryCsv() {}

    enum Period {
        ALL("全期間"),
        THIS_MONTH("今月"),
        LAST_MONTH("先月");

        final String label;

        Period(String label) {
            this.label = label;
        }
    }

    record Selection(long habitId, Period period, LocalDate date) {
        YearMonth month() {
            YearMonth current = YearMonth.from(date);
            return period == Period.LAST_MONTH ? current.minusMonths(1) : current;
        }

        Bundle saveState() {
            Bundle bundle = new Bundle();
            bundle.putLong("habit", habitId);
            bundle.putString("period", period.name());
            bundle.putString("date", date.toString());
            return bundle;
        }

        static Selection restore(Bundle bundle) {
            if (bundle == null) return null;
            return new Selection(bundle.getLong("habit"),
                Period.valueOf(bundle.getString("period")),
                LocalDate.parse(bundle.getString("date")));
        }
    }

    static Intent createDocumentIntent(LocalDate date) {
        return new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_TITLE, "routia-records-" + date + ".csv");
    }

    static boolean save(Context context, Uri destination, Selection selection) {
        if (!ContentResolver.SCHEME_CONTENT.equals(destination.getScheme())) return false;
        ContentResolver resolver = context.getContentResolver();
        try {
            OutputStream stream = resolver.openOutputStream(destination);
            if (stream == null) throw new IOException("No output stream");
            try (Writer output = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
                Store.get(context).writeHistoryCsv(output, selection);
            }
            return true;
        } catch (IOException | RuntimeException failure) {
            // ACTION_CREATE_DOCUMENT gave us a new file. Remove partial output when the provider allows it.
            try {
                DocumentsContract.deleteDocument(resolver, destination);
            } catch (IOException | RuntimeException ignored) { }
            return false;
        }
    }

    static void writeHeader(Writer output) throws IOException {
        output.write("\uFEFF日付,習慣名,達成段階,記録値,単位\r\n");
    }

    static void writeRow(Writer output, String day, String name, int level, Double amount, String unit)
            throws IOException {
        output.write(day);
        output.write(',');
        writeText(output, name);
        output.write(',');
        writeText(output, Rules.grade(level));
        output.write(',');
        boolean hasAmount = amount != null && Double.isFinite(amount);
        if (hasAmount) output.write(BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString());
        output.write(',');
        if (hasAmount) writeText(output, unit);
        output.write("\r\n");
    }

    private static void writeText(Writer output, String value) throws IOException {
        int first = 0;
        while (first < value.length() && (Character.isWhitespace(value.charAt(first))
                || Character.isSpaceChar(value.charAt(first)))) first++;
        boolean formula = first < value.length() && "=+-@＝＋－＠".indexOf(value.charAt(first)) >= 0;
        boolean control = !value.isEmpty() && "\t\r\n".indexOf(value.charAt(0)) >= 0;

        output.write('"');
        if (formula || control) output.write('\'');
        output.write(value.replace("\"", "\"\""));
        output.write('"');
    }
}
