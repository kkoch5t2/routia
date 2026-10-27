package app.routia;
/** Optional setup fields use visible, general defaults; explicit user wording is preserved. */
public final class HabitDefaults {
    public static final String TITLE="今日の習慣",MINIMUM="少しでも取り組む",FULL="今日の目標に取り組む",EXTRA="目標を超えて取り組む";
    private static String value(String input,String fallback){String text=Inputs.clean(input==null?"":input);return text.isEmpty()?fallback:text;}
    public static String title(String input){return value(input,TITLE);}
    public static String minimum(String input){return value(input,MINIMUM);}
    public static String full(String input){return value(input,FULL);}
    public static String extra(String input){return value(input,EXTRA);}
    public static String unit(String kind,String input){return "time".equals(kind)?"分":"count".equals(kind)?value(input,"回"):"";}
    private HabitDefaults(){}
}
