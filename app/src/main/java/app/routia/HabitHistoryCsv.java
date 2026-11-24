package app.routia;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

final class HabitHistoryCsv {
    private HabitHistoryCsv() {}

    static Intent createDocumentIntent(LocalDate date) {
        return new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_TITLE, "routia-records-" + date + ".csv");
    }

    static boolean save(Context context, Uri destination) {
        if (!ContentResolver.SCHEME_CONTENT.equals(destination.getScheme())) return false;
        ContentResolver resolver = context.getContentResolver();
        try {
            OutputStream stream = resolver.openOutputStream(destination);
            if (stream == null) throw new IOException("No output stream");
            try (Writer output = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
                Store.get(context).writeHistoryCsv(output);
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
