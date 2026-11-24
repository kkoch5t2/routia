package app.routia;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;

final class HabitHistoryCsvTest {
    private int checks;

    private void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }

    private static String export(Store store,long habit,HabitHistoryCsv.Period period,LocalDate date) throws IOException {
        StringWriter writer = new StringWriter();
        store.writeHistoryCsv(writer,new HabitHistoryCsv.Selection(habit,period,date));
        return writer.toString();
    }

    int run(Context context) throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 20);
        Intent picker = HabitHistoryCsv.createDocumentIntent(date);
        check(Intent.ACTION_CREATE_DOCUMENT.equals(picker.getAction()), "Uses the system create-file picker");
        check(picker.hasCategory(Intent.CATEGORY_OPENABLE) && "text/csv".equals(picker.getType()),
            "Picker creates a writable CSV document");
        check("routia-records-2026-10-20.csv".equals(picker.getStringExtra(Intent.EXTRA_TITLE)),
            "Filename has the export date");

        String databaseName = "routia-history-csv-test.db";
        context.deleteDatabase(databaseName);
        try (Store store = new Store(context, databaseName)) {
            String header = "\uFEFF日付,習慣名,達成段階,記録値,単位\r\n";
            StringWriter empty = new StringWriter();
            HabitHistoryCsv.Selection all = new HabitHistoryCsv.Selection(0,HabitHistoryCsv.Period.ALL,date);
            store.writeHistoryCsv(empty,all);
            check(empty.toString().equals(header), "An empty history exports the header with a UTF-8 BOM marker");

            long time = store.saveHabit(0, "", "英語を聞く", "1分", "time", 15, "分");
            long checkHabit = store.saveHabit(0, "", "読む", "1ページ", "check", 0, "");
            store.saveHabit(0, "", "未記録の習慣", "ひとつ", "check", 0, "");
            SQLiteDatabase db = store.getWritableDatabase();
            db.execSQL("UPDATE habits SET goal=? WHERE id=?", new Object[]{"英語,\"会話\"\n基礎", time});
            db.execSQL("UPDATE habits SET goal=? WHERE id=?", new Object[]{"　＝1+1", checkHabit});
            db.execSQL("UPDATE habits SET created=?", new Object[]{date.minusDays(1).toString()});
            db.execSQL("INSERT INTO records(habit_id,day,level,amount,unit,note) VALUES(?,?,?,?,?,?)",
                new Object[]{time, date.toString(), Rules.FULL, 15.5, "分", "秘密のテストメモ"});
            db.execSQL("INSERT INTO records(habit_id,day,level) VALUES(?,?,?)",
                new Object[]{checkHabit, date.toString(), Rules.EXTRA});
            db.execSQL("INSERT INTO records(habit_id,day,level) VALUES(?,?,?)",
                new Object[]{time, date.minusDays(1).toString(), Rules.MINIMUM});

            StringWriter file = new StringWriter();
            store.writeHistoryCsv(file,all);
            String csv = file.toString();
            String oldRow = "2026-10-19,\"英語,\"\"会話\"\"\n基礎\",\"🌱 ミニ達成\",,\r\n";
            String amountRow = "2026-10-20,\"英語,\"\"会話\"\"\n基礎\",\"🏆 目標達成\",15.5,\"分\"\r\n";
            String checkRow = "2026-10-20,\"'　＝1+1\",\"🚀 チャレンジ達成\",,\r\n";
            check(csv.equals(header + oldRow + amountRow + checkRow),
                "Rows are ordered, preserve Japanese and quoted text, and use recorded units and blank legacy values");
            check(!csv.contains("秘密のテストメモ") && !csv.contains("未記録の習慣"),
                "Memos and unrecorded days are outside the export");
            String september = "2026-09-30,\"英語,\"\"会話\"\"\n基礎\",\"🌱 ミニ達成\",,\r\n";
            String october = "2026-10-01,\"英語,\"\"会話\"\"\n基礎\",\"🌱 ミニ達成\",,\r\n";
            for (String boundary : new String[]{"2026-09-30","2026-10-01","2026-11-01"}) {
                db.execSQL("INSERT INTO records(habit_id,day,level) VALUES(?,?,?)",
                    new Object[]{time,boundary,Rules.MINIMUM});
            }
            check(export(store,time,HabitHistoryCsv.Period.THIS_MONTH,date).equals(header + october + oldRow + amountRow),
                "Selected habit and current month use inclusive start and exclusive end");
            check(export(store,0,HabitHistoryCsv.Period.LAST_MONTH,date).equals(header + september),
                "All habits can be exported for the previous month");
            check(export(store,checkHabit,HabitHistoryCsv.Period.ALL,date).equals(header + checkRow),
                "Selecting a habit alone uses its ID");
            check(export(store,checkHabit,HabitHistoryCsv.Period.LAST_MONTH,date).equals(header),
                "A filtered window with no records exports only the header");
            LocalDate january = LocalDate.of(2027,1,1);
            db.execSQL("INSERT INTO records(habit_id,day,level) VALUES(?,?,?)",
                new Object[]{checkHabit,"2026-12-31",Rules.MINIMUM});
            check(export(store,checkHabit,HabitHistoryCsv.Period.LAST_MONTH,january)
                .equals(header + "2026-12-31,\"'　＝1+1\",\"🌱 ミニ達成\",,\r\n"),
                "The previous month in January is December of the previous year");
            HabitHistoryCsv.Selection pending = new HabitHistoryCsv.Selection(time,HabitHistoryCsv.Period.LAST_MONTH,january);
            check(pending.equals(HabitHistoryCsv.Selection.restore(pending.saveState())),
                "Picker state preserves the habit and the date used to resolve the period");

            check(store.entry(time, date).amount() == 15.5
                && store.entry(time, date).note().equals("秘密のテストメモ")
                && store.level(checkHabit, date) == Rules.EXTRA && store.ticketDays(time).isEmpty(),
                "Export leaves achievements, memos and ticket state untouched");
        } finally {
            context.deleteDatabase(databaseName);
        }
        return checks;
    }
}
