package app.routia;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Versioned, local snapshot of habit data. The allowlist is independent of the database schema. */
final class HabitBackup {
    private static final String FORMAT = "routia-habits";
    private static final int VERSION = 1;
    private static final long MAX_BYTES = 32L * 1024 * 1024;

    private enum Table {
        HABITS("habits", null, "id", "cue", "goal", "minimum", "created", "hour", "minute", "reminder",
            "metric", "target", "unit", "full_goal", "extra_goal", "description"),
        RECORDS("records", null, "habit_id", "day", "level", "amount", "unit", "note"),
        SESSIONS("sessions", "completed=1", "habit_id", "day", "seconds"),
        TICKETS("tickets", null, "habit_id", "day", "used_on"),
        TICKET_STATE("ticket_state", null, "habit_id", "next_day");

        final String name;
        final String selection;
        final String[] columns;

        Table(String name, String selection, String... columns) {
            this.name = name;
            this.selection = selection;
            this.columns = columns;
        }
    }

    static final class InvalidBackup extends IOException { }
    static final class ExistingHabits extends IOException { }

    static Intent createDocument(LocalDate today) {
        return new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json").putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            .putExtra(Intent.EXTRA_TITLE, "routia-backup-" + today + ".json");
    }

    static Intent openDocument() {
        return new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*").putExtra(Intent.EXTRA_LOCAL_ONLY, true);
    }

    static void save(Context context, Uri destination) throws IOException {
        File staged = File.createTempFile("routia-backup-", ".json", context.getCacheDir());
        try {
            try (OutputStream snapshot = new FileOutputStream(staged)) {
                write(Store.get(context), snapshot);
            }
            if (staged.length() > MAX_BYTES) throw new IOException();
            try (InputStream snapshot = new FileInputStream(staged);
                 OutputStream file = context.getContentResolver().openOutputStream(destination, "wt")) {
                if (file == null) throw new IOException();
                copy(snapshot, file);
            }
        } finally {
            staged.delete();
        }
    }

    static int load(Context context, Uri source) throws IOException {
        File staged = File.createTempFile("routia-restore-", ".json", context.getCacheDir());
        try {
            try (InputStream file = context.getContentResolver().openInputStream(source);
                 OutputStream snapshot = new FileOutputStream(staged)) {
                if (file == null) throw new IOException();
                copy(file, snapshot);
            }
            try (InputStream snapshot = new FileInputStream(staged)) {
                return restore(Store.get(context), snapshot);
            }
        } finally {
            staged.delete();
        }
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        for (int size; (size = input.read(buffer)) != -1;) {
            total += size;
            if (total > MAX_BYTES) throw new InvalidBackup();
            output.write(buffer, 0, size);
        }
    }

    static void write(Store store, OutputStream output) throws IOException {
        SQLiteDatabase db = store.getReadableDatabase();
        try (JsonWriter json = new JsonWriter(new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8)))) {
            db.beginTransactionNonExclusive();
            try {
                json.beginObject();
                json.name("format").value(FORMAT);
                json.name("version").value(VERSION);
                for (Table table : Table.values()) {
                    json.name(table.name).beginArray();
                    try (Cursor rows = db.query(table.name, table.columns, table.selection, null, null, null,
                            table == Table.HABITS ? "id" : null)) {
                        while (rows.moveToNext()) {
                            json.beginArray();
                            for (int col = 0; col < table.columns.length; col++) {
                                switch (rows.getType(col)) {
                                    case Cursor.FIELD_TYPE_NULL: json.nullValue(); break;
                                    case Cursor.FIELD_TYPE_INTEGER: json.value(rows.getLong(col)); break;
                                    case Cursor.FIELD_TYPE_FLOAT: json.value(rows.getDouble(col)); break;
                                    default: json.value(rows.getString(col)); break;
                                }
                            }
                            json.endArray();
                        }
                    }
                    json.endArray();
                }
                json.endObject();
                json.flush();
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    static int restore(Store store, InputStream input) throws IOException {
        SQLiteDatabase db = store.getWritableDatabase();
        try (JsonReader json = new JsonReader(new BufferedReader(new InputStreamReader(input,
                StandardCharsets.UTF_8.newDecoder())))) {
            db.beginTransaction();
            try {
                if (store.hasHabits()) throw new ExistingHabits();
                json.beginObject();
                expectName(json, "format");
                if (!FORMAT.equals(readText(json, FORMAT.length()))) throw new InvalidBackup();
                expectName(json, "version");
                if (integer(json, 1, Integer.MAX_VALUE) != VERSION) throw new InvalidBackup();
                Map<Long, Long> habitIds = new HashMap<>();
                for (Table table : Table.values()) {
                    expectName(json, table.name);
                    json.beginArray();
                    while (json.hasNext()) {
                        json.beginArray();
                        long id = integer(json, 1, Long.MAX_VALUE);
                        ContentValues row = new ContentValues();
                        for (int col = 1; col < table.columns.length; col++) readColumn(json, table.columns[col], row);
                        json.endArray();
                        if (table == Table.HABITS) {
                            if (habitIds.size() >= Rules.MAX_HABITS || habitIds.containsKey(id)) throw new InvalidBackup();
                            habitIds.put(id, db.insertOrThrow(table.name, null, row));
                        } else {
                            Long destination = habitIds.get(id);
                            if (destination == null) throw new InvalidBackup();
                            row.put("habit_id", destination);
                            if (table == Table.SESSIONS) row.put("completed", 1);
                            db.insertOrThrow(table.name, null, row);
                        }
                    }
                    json.endArray();
                }
                json.endObject();
                if (json.peek() != JsonToken.END_DOCUMENT) throw new InvalidBackup();
                try (Cursor missing = db.rawQuery("SELECT 1 FROM habits h LEFT JOIN ticket_state s"
                        + " ON h.id=s.habit_id WHERE s.habit_id IS NULL LIMIT 1", null)) {
                    if (missing.moveToFirst()) throw new InvalidBackup();
                }
                db.setTransactionSuccessful();
                return habitIds.size();
            } finally {
                db.endTransaction();
            }
        } catch (IllegalStateException | IllegalArgumentException | DateTimeException
                | SQLiteConstraintException | java.nio.charset.CharacterCodingException
                | android.util.MalformedJsonException | EOFException invalid) {
            throw new InvalidBackup();
        }
    }

    private static void expectName(JsonReader json, String name) throws IOException {
        if (!name.equals(json.nextName())) throw new InvalidBackup();
    }

    private static String readText(JsonReader json, int maxLength) throws IOException {
        if (json.peek() != JsonToken.STRING) throw new InvalidBackup();
        String value = json.nextString();
        if (value.length() > maxLength) throw new InvalidBackup();
        return value;
    }

    private static long integer(JsonReader json, long minimum, long maximum) throws IOException {
        if (json.peek() != JsonToken.NUMBER) throw new InvalidBackup();
        long value = Long.parseLong(json.nextString());
        if (value < minimum || value > maximum) throw new InvalidBackup();
        return value;
    }

    private static void readColumn(JsonReader json, String column, ContentValues row) throws IOException {
        switch (column) {
            case "created": case "day": case "used_on": case "next_day":
                String date = readText(json, 10);
                if (date.length() != 10 || !LocalDate.parse(date).toString().equals(date)) throw new InvalidBackup();
                row.put(column, date);
                return;
            case "metric":
                String kind = readText(json, 5);
                if (!kind.equals("check") && !kind.equals("time") && !kind.equals("count")) throw new InvalidBackup();
                row.put(column, kind);
                return;
            case "target": case "amount":
                if (column.equals("amount") && json.peek() == JsonToken.NULL) {
                    json.nextNull();
                    row.putNull(column);
                    return;
                }
                if (json.peek() != JsonToken.NUMBER) throw new InvalidBackup();
                double value = json.nextDouble();
                if (!Double.isFinite(value) || value < 0 || value > 1_000_000) throw new InvalidBackup();
                row.put(column, value);
                return;
            case "hour": row.put(column, integer(json, 0, 23)); return;
            case "minute": row.put(column, integer(json, 0, 59)); return;
            case "reminder": row.put(column, integer(json, 0, 1)); return;
            case "level": row.put(column, integer(json, Rules.MINIMUM, Rules.EXTRA)); return;
            case "seconds": row.put(column, integer(json, 0, Integer.MAX_VALUE)); return;
            case "unit": row.put(column, readText(json, 8)); return;
            case "description": case "note": row.put(column, readText(json, 1000)); return;
            default: row.put(column, readText(json, 120));
        }
    }

    private HabitBackup() { }
}
