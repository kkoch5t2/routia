package app.routia;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

final class HabitBackupTest {
    private int checks;

    int run(Context context) throws IOException {
        String sourceName = "habit-backup-source-qa.db";
        String targetName = "habit-backup-target-qa.db";
        String rejectedName = "habit-backup-rejected-qa.db";
        try (Store source = new Store(context, sourceName);
             Store target = new Store(context, targetName);
             Store rejected = new Store(context, rejectedName)) {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            long timed = source.saveConfiguredHabit(0, "食後", "英語", "", "", "", "time", 15, "分", "ニュースを聞く");
            long checkHabit = source.saveConfiguredHabit(0, "朝", "読書", "一行", "一頁", "一章", "check", 0, "");
            SQLiteDatabase db = source.getWritableDatabase();
            db.execSQL("UPDATE habits SET created=? WHERE id=?", new Object[]{yesterday.toString(), timed});
            db.execSQL("INSERT INTO tickets(habit_id,day,used_on) VALUES(?,?,?)",
                new Object[]{timed, yesterday.toString(), today.toString()});
            source.recordAmount(timed, today, 2.5, "英語\n\"復習\" ☕", false);
            source.record(checkHabit, today, Rules.FULL, false);
            source.completeTimedSession(source.newSession(timed), 120);
            source.finishSession(source.newSession(timed), 30, false);
            source.newSession(timed);
            db.execSQL("INSERT INTO pledges(habit_id,goal,week,day,reason,yen) VALUES(?,?,?,?,?,?)",
                new Object[]{timed, "英語", today.toString(), today.toString(), "検証", 100});

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            HabitBackup.write(source, buffer);
            String backup = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            check(!backup.contains("pledges") && !backup.contains("api_key"), "private settings and pledges are not exported");
            check(HabitBackup.createDocument(today).getAction().equals(Intent.ACTION_CREATE_DOCUMENT)
                && HabitBackup.openDocument().getAction().equals(Intent.ACTION_OPEN_DOCUMENT), "uses the local Android document picker");

            long old = target.save(0, "", "削除済み", "", 21, 0, false);
            target.delete(old);
            check(restore(target, backup) == 2, "two habits restored");
            long imported = target.habits().get(0).id();
            long importedCheck = target.habits().get(1).id();
            check(imported != timed && target.habit(imported).created().equals(yesterday), "new IDs retain original creation dates");
            check(target.metric(imported).equals(source.metric(timed)) && target.description(imported).equals("ニュースを聞く"), "habit settings restored");
            check(target.entry(imported, today).equals(source.entry(timed, today)), "record, numeric amount and multilingual notes survive");
            check(target.entry(importedCheck, today).amount() == null && target.level(importedCheck, today) == Rules.FULL
                && target.goals(importedCheck).equals(source.goals(checkHabit)), "legacy-style null amount and check criteria survive");
            check(target.focused(imported, today, today) == 120 && target.pledges().isEmpty(), "only completed focus sessions restored");
            check(target.ticket(imported, yesterday), "ticket state restored");
            try {
                restore(target, backup);
                throw new AssertionError("restore overwrote existing habits");
            } catch (HabitBackup.ExistingHabits expected) {
                check(target.habits().size() == 2 && target.level(imported, today) == Rules.MINIMUM, "repeated restore leaves data intact");
            }
            reject(rejected, backup.substring(0, backup.length() - 1));
            reject(rejected, backup.replace("\"version\":1", "\"version\":2"));
            reject(rejected, backup.replace("\"records\":[[" + timed, "\"records\":[[999999"));
            reject(rejected, backup + " {}");
            check(restore(rejected, backup) == 2, "valid restore works after a failed attempt");
        } finally {
            context.deleteDatabase(sourceName);
            context.deleteDatabase(targetName);
            context.deleteDatabase(rejectedName);
        }
        return checks;
    }

    private int restore(Store target, String text) throws IOException {
        return HabitBackup.restore(target, new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private void reject(Store target, String text) throws IOException {
        try {
            restore(target, text);
            throw new AssertionError("invalid backup was accepted");
        } catch (HabitBackup.InvalidBackup expected) {
            check(target.habits().isEmpty(), "invalid backup rolls back the whole transaction");
        }
    }

    private void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        checks++;
    }
}
