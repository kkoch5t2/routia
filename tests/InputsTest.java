import app.routia.Inputs;
import java.util.*;
public class InputsTest {
    static int n;static void check(boolean ok){n++;if(!ok)throw new AssertionError("Inputs "+n);}
    static void invalid(String raw,boolean integer,boolean optional){try{Inputs.number(raw,"テスト項目",1,180,integer,optional);throw new AssertionError("Accepted "+raw);}catch(IllegalArgumentException e){check(e.getMessage().contains("テスト項目"));}}
    public static void main(String[] args){check(Inputs.clean(" 　\t\n").isEmpty());check(Inputs.clean("　英語　").equals("英語"));check(Inputs.number("　","目標",0,100,false,true)==null);check(Inputs.number("180","分",1,180,true,false)==180);check(Inputs.number(" 1.5 ","回数",0,10,false,false)==1.5);invalid("",true,false);invalid("　",true,false);invalid("0",true,false);invalid("181",true,false);invalid("1.5",true,false);invalid("NaN",false,true);invalid("Infinity",false,true);invalid("1e309",false,true);invalid("1..2",false,true);System.out.println("PASS "+n+" form-input assertions");}
}
