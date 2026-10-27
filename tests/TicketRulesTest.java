import app.routia.*;
import java.time.*;
import java.util.*;
public class TicketRulesTest {
    static int n;static void check(boolean b){n++;if(!b)throw new AssertionError("Ticket rule "+n);}
    public static void main(String[] args){
        LocalDate d=LocalDate.of(2026,10,10);
        Set<LocalDate> done=new HashSet<>(Set.of(d.minusDays(4),d.minusDays(3),d));
        List<LocalDate> planned=TicketRules.plan(d.minusDays(2),d,done,Set.of());
        check(planned.equals(List.of(d.minusDays(2),d.minusDays(1))));
        Set<LocalDate> tickets=new HashSet<>(planned);
        check(Progress.streak(done,tickets,d)==3);check(Progress.best(done,tickets)==3);check(Progress.streak(done,d)==1);
        check(TicketRules.plan(d.minusDays(2),d,done,tickets).isEmpty());
        done.remove(d);check(Progress.streak(done,tickets,d)==2);check(done.size()==2);
        check(TicketRules.plan(d.minusDays(4),d,Set.of(),Set.of()).isEmpty());
        check(TicketRules.plan(d.minusDays(2),d,Set.of(d.minusDays(4)),Set.of()).isEmpty());
        Set<LocalDate> old=Set.of(d.minusDays(5));planned=TicketRules.plan(d.minusDays(4),d,old,Set.of());
        check(planned.equals(List.of(d.minusDays(4),d.minusDays(3))));check(Progress.streak(old,new HashSet<>(planned),d)==0);
        LocalDate october=LocalDate.of(2026,10,1);Set<LocalDate> boundary=Set.of(october.minusDays(2),october.plusDays(1));
        planned=TicketRules.plan(october.minusDays(1),october.plusDays(1),boundary,Set.of());
        check(planned.equals(List.of(october.minusDays(1),october)));check(Progress.streak(boundary,new HashSet<>(planned),october.plusDays(1))==2);
        check(TicketRules.plan(d,d,Set.of(d.minusDays(1)),Set.of()).isEmpty());
        check(Progress.streak(Set.of(d.minusDays(2)),Set.of(d.minusDays(1)),d)==1);
        check(Progress.best(Set.of(d.minusDays(4),d.minusDays(2),d),Set.of(d.minusDays(3),d.minusDays(1)))==3);
        check(Progress.best(Set.of(),Set.of(d.minusDays(1)))==0);
        check(Progress.streak(Set.of(),Set.of(d.minusDays(1)),d)==0);
        check(TicketRules.plan(october,october.plusDays(2),Set.of(october.minusDays(2)),Set.of(october.minusDays(1))).size()==2);
        check(TicketRules.plan(d.minusDays(1),d,Set.of(d.minusDays(2),d.minusDays(1)),Set.of()).isEmpty());
        check(TicketRules.plan(d,d.plusDays(2),Set.of(d),Set.of()).equals(List.of(d.plusDays(1))));
        check(TicketRules.plan(d,d.plusDays(1),Set.of(d),Set.of()).isEmpty());
        check(TicketRules.plan(d.minusDays(3),d.plusDays(2),Set.of(d),Set.of()).equals(List.of(d.plusDays(1))));
        check(TicketRules.plan(d,d.plusDays(3),Set.of(),Set.of()).isEmpty());
        System.out.println("PASS "+n+" automatic-ticket/streak assertions");
    }
}
