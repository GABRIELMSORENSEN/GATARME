import com.gatarme.app.Schedule;
import java.time.*;
import java.util.*;

public class ScheduleTest {
  static int count = 0;
  static ZoneId zone = ZoneId.of("America/Cuiaba");

  static void check(boolean b, String name) {
    count++;
    if (!b) throw new AssertionError(name);
  }

  static long at(String s) {
    return LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli();
  }

  static Schedule job() {
    Schedule s = new Schedule();
    s.anchor = LocalDate.of(2026, 9, 15);
    s.start = LocalTime.of(7, 0);
    return s;
  }

  public static void main(String[] args) {
    Schedule a = job();
    check(a.next(at("2026-09-15T00:00"), zone) == at("2026-09-15T06:00"), "first 12x36");
    check(a.next(at("2026-09-15T06:00"), zone) == at("2026-09-17T06:00"), "dismiss continues");
    Schedule b = job();
    b.anchor = b.anchor.plusDays(1);
    b.start = LocalTime.of(19, 0);
    check(b.next(at("2026-09-15T06:00"), zone) == at("2026-09-16T18:00"), "independent second job");
    a.offsets = Arrays.asList(-90, -30, 0);
    check(a.next(at("2026-09-15T05:30"), zone) == at("2026-09-15T06:30"), "multiple alarms");
    a.skip.add(a.anchor);
    check(a.next(at("2026-09-15T00:00"), zone) == at("2026-09-17T05:30"), "skip holiday");
    a = job();
    a.start = LocalTime.of(19, 0);
    check(a.covers(LocalDate.of(2026, 9, 16)), "overnight calendar");
    check(!a.covers(LocalDate.of(2026, 9, 14)), "before anchor");
    a = job();
    a.work = 24;
    a.rest = 48;
    check(a.next(at("2026-09-15T06:00"), zone) == at("2026-09-18T06:00"), "24x48");
    a = job();
    a.work = 12;
    a.rest = 24;
    check(a.next(at("2026-09-15T06:00"), zone) == at("2026-09-16T18:00"), "36h cycle changes time");
    a = job();
    a.mode = "days";
    a.work = 6;
    a.rest = 1;
    check(a.starts(a.anchor, a.anchor.plusDays(13)).size() == 12, "6x1 two weeks");
    a.mode = "pattern";
    a.pattern = "111100";
    check(a.starts(a.anchor, a.anchor.plusDays(11)).size() == 8, "pattern");
    a = job();
    a.mode = "weekly";
    a.weekdays = new HashSet<>(Arrays.asList(1, 3, 5));
    a.dayTimes.put(3, LocalTime.of(9, 0));
    check(a.next(at("2026-09-15T00:00"), zone) == at("2026-09-16T08:00"), "weekday-specific time");
    a = job();
    a.start = LocalTime.of(0, 20);
    check(
        a.next(at("2026-09-14T00:00"), zone) == at("2026-09-14T23:20"),
        "alarm before first shift date");
    a = job();
    a.anchor = LocalDate.of(2024, 2, 28);
    check(a.next(at("2024-02-28T06:00"), zone) == at("2024-03-01T06:00"), "leap year");
    a = job();
    a.anchor = LocalDate.of(2026, 3, 7);
    ZoneId ny = ZoneId.of("America/New_York");
    long start = LocalDateTime.of(2026, 3, 7, 6, 0).atZone(ny).toInstant().toEpochMilli();
    check(
        a.next(start, ny)
            == LocalDateTime.of(2026, 3, 9, 6, 0).atZone(ny).toInstant().toEpochMilli(),
        "DST preserves civil time");
    for (int[] p :
        new int[][] {
          {12, 36}, {12, 24}, {12, 48}, {24, 24}, {24, 48}, {24, 72}, {24, 96}, {36, 12}, {48, 96}
        }) {
      a = job();
      a.work = p[0];
      a.rest = p[1];
      long last = at("2026-09-14T00:00");
      for (int i = 0; i < 100; i++) {
        long n = a.next(last, zone);
        check(n > last, "monotonic recurring alarms");
        last = n;
      }
    }
    a = job();
    a.mode = "pattern";
    a.pattern = "1100";
    a.patternTimes.put(1, LocalTime.of(19, 0));
    check(
        a.next(at("2026-09-15T06:00"), zone) == at("2026-09-16T18:00"),
        "rotating day and night shifts");
    System.out.println("PASS: " + count + " recurrence assertions");
  }
}
