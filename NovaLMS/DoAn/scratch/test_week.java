import java.time.LocalDate;
import java.time.temporal.WeekFields;

public class TestWeek {
    public static void main(String[] args) {
        int year = 2026;
        int week = 17;
        LocalDate weekStart = LocalDate.of(year, 1, 1)
                .with(WeekFields.ISO.dayOfWeek(), 1)
                .plusWeeks(week - 1);
        System.out.println("Week " + week + " start: " + weekStart);
    }
}
