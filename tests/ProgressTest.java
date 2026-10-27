import app.routia.Progress;
import java.time.*;
import java.util.*;
public class ProgressTest {
    static int n;static void check(boolean ok){n++;if(!ok)throw new AssertionError("Progress check "+n);}
    public static void main(String[] args){LocalDate today=LocalDate.of(2026,10,23),created=today.minusYears(4);Set<LocalDate> days=new HashSet<>(List.of(today.minusDays(1),today.minusDays(2),today.minusDays(3)));
        check(Progress.streak(days,today)==3);days.add(today);check(Progress.streak(days,today)==4);check(Progress.best(days)==4);days.remove(today.minusDays(1));check(Progress.streak(days,today)==1);check(Progress.best(days)==2);check(Progress.streak(Set.of(),today)==0);
        Progress.Range r=Progress.range(0,today,created,today);check(r.start().equals(LocalDate.of(2026,10,19)));check(r.end().equals(today));r=Progress.range(1,LocalDate.of(2024,2,15),created,today);check(r.end().equals(LocalDate.of(2024,2,29)));r=Progress.range(2,LocalDate.of(2024,10,3),created,today);check(r.start().equals(LocalDate.of(2024,1,1)));check(r.end().equals(LocalDate.of(2024,12,31)));r=Progress.range(3,today,created,today);check(r.start().equals(created));check(r.end().equals(today));
        check(Progress.shade(false,100d,100,2)==0);check(Progress.shade(true,null,100,1)==1);check(Progress.shade(true,null,100,2)==2);check(Progress.shade(true,0d,100,2)==1);check(Progress.shade(true,25d,100,2)==1);check(Progress.shade(true,26d,100,2)==2);check(Progress.shade(true,75d,100,2)==3);check(Progress.shade(true,100d,100,2)==4);check(Progress.shade(true,500d,100,2)==4);
        System.out.println("PASS "+n+" progress assertions");
    }
}
