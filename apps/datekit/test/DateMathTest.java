import com.factory.datekit.DateMath;
import java.time.LocalDate;
public class DateMathTest {
 static int count;
 static LocalDate d(String s){return LocalDate.parse(s);}
 static void eq(Object actual,Object expected){count++;if(!actual.equals(expected))throw new AssertionError(actual+" != "+expected);}
 static void rejects(Runnable r){count++;try{r.run();throw new AssertionError("accepted invalid input");}catch(IllegalArgumentException expected){}}
 public static void main(String[] args){
 eq(DateMath.difference(d("2024-02-28"),d("2024-03-01")),2L);
 eq(DateMath.difference(d("2023-02-28"),d("2023-03-01")),1L);
 eq(DateMath.difference(d("2024-03-01"),d("2024-02-28")),-2L);
 eq(DateMath.difference(d("2026-01-01"),d("2026-01-01")),0L);
 eq(DateMath.inclusive(d("2024-03-01"),d("2024-02-28")),3L);
 eq(DateMath.inclusive(d("2026-01-01"),d("2026-01-01")),1L);
 eq(DateMath.offset(d("2024-02-28"),1),d("2024-02-29"));
 eq(DateMath.offset(d("2024-01-01"),-1),d("2023-12-31"));
 eq(DateMath.offset(d("2026-12-31"),1),d("2027-01-01"));
 eq(DateMath.offset(d("2026-01-01"),0),d("2026-01-01"));
 eq(DateMath.dday(d("2026-01-01"),d("2026-01-02")),"D-1");
 eq(DateMath.dday(d("2026-01-02"),d("2026-01-01")),"D+1");
 eq(DateMath.dday(d("2026-01-01"),d("2026-01-01")),"D-Day");
 rejects(()->DateMath.offset(d("1900-01-01"),-1));
 rejects(()->DateMath.offset(d("9999-12-31"),1));
 rejects(()->DateMath.offset(d("2026-01-01"),Long.MAX_VALUE));
 rejects(()->DateMath.difference(d("1899-12-31"),d("2026-01-01")));
 for(int year=1900;year<=2100;year++){
 LocalDate a=LocalDate.of(year,1,1),b=a.plusYears(1);
 eq(DateMath.difference(a,b),(long)a.lengthOfYear());
 eq(DateMath.offset(a,a.lengthOfYear()),b);
 }
 System.out.println(count+" DateKit checks passed");
 }
}
