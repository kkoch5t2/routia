package app.routia;
/** Numeric form values with field-specific messages; blanks are allowed only when optional. */
public final class Inputs {
    public static String clean(String raw){if(raw==null)return "";int start=0,end=raw.length();while(start<end&&(Character.isWhitespace(raw.charAt(start))||Character.isSpaceChar(raw.charAt(start))))start++;while(end>start&&(Character.isWhitespace(raw.charAt(end-1))||Character.isSpaceChar(raw.charAt(end-1))))end--;return raw.substring(start,end);}
    public static Double number(String raw,String label,double minimum,double maximum,boolean integer,boolean optional){
        if(clean(raw).isEmpty()){if(optional)return null;throw new IllegalArgumentException(label+"を入力してください");}
        final double value;try{value=Double.parseDouble(clean(raw));}catch(NumberFormatException e){throw new IllegalArgumentException(label+"は数値で入力してください");}
        if(!Double.isFinite(value)||value<minimum||value>maximum||integer&&value!=Math.rint(value))throw new IllegalArgumentException(label+"は"+format(minimum)+"〜"+format(maximum)+(integer?"の整数":"の数値")+"で入力してください");
        return value;
    }
    static String format(double n){return String.format(java.util.Locale.ROOT,"%.0f",n);}
    private Inputs(){}
}
