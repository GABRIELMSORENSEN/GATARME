package com.gatarme.app;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Pure recurrence engine. Local civil time is preserved across daylight saving transitions. */
public final class Schedule {
  public String mode = "hours", pattern = "10";
  public LocalDate anchor = LocalDate.now();
  public LocalTime start = LocalTime.of(7, 0);
  public int work = 12, rest = 36, duration = 12;
  public Set<Integer> weekdays = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
  public Map<Integer, LocalTime> dayTimes = new HashMap<>();
  public Map<Integer, LocalTime> patternTimes = new HashMap<>();
  public List<Integer> offsets = new ArrayList<>(Arrays.asList(-60));
  public Set<LocalDate> skip = new HashSet<>();

  public List<LocalDateTime> starts(LocalDate from, LocalDate to) {
    List<LocalDateTime> out = new ArrayList<>();
    LocalDate first = from.isBefore(anchor) ? anchor : from;
    if (mode.equals("hours")) {
      LocalDateTime origin = anchor.atTime(start);
      long cycle = work + rest;
      long n =
          Math.max(0, Math.floorDiv(ChronoUnit.HOURS.between(origin, first.atStartOfDay()), cycle));
      for (LocalDateTime t = origin.plusHours(n * cycle);
          !t.toLocalDate().isAfter(to);
          t = t.plusHours(cycle))
        if (!t.toLocalDate().isBefore(first) && !skip.contains(t.toLocalDate())) out.add(t);
    } else
      for (LocalDate d = first; !d.isAfter(to); d = d.plusDays(1)) {
        long days = ChronoUnit.DAYS.between(anchor, d);
        boolean on =
            mode.equals("weekly")
                ? weekdays.contains(d.getDayOfWeek().getValue())
                : mode.equals("days")
                    ? days % (work + rest) < work
                    : pattern.charAt((int) (days % pattern.length())) == '1';
        if (on && !skip.contains(d))
          out.add(
              d.atTime(
                  mode.equals("pattern")
                      ? patternTimes.getOrDefault((int) (days % pattern.length()), start)
                      : dayTimes.getOrDefault(d.getDayOfWeek().getValue(), start)));
      }
    return out;
  }

  public long next(long after, ZoneId zone) {
    LocalDate today = Instant.ofEpochMilli(after).atZone(zone).toLocalDate();
    long best = Long.MAX_VALUE;
    for (LocalDateTime shift : starts(today.minusDays(8), today.plusDays(740)))
      for (int offset : offsets) {
        long candidate = shift.plusMinutes(offset).atZone(zone).toInstant().toEpochMilli();
        if (candidate > after && candidate < best) best = candidate;
      }
    return best == Long.MAX_VALUE ? 0 : best;
  }

  public boolean covers(LocalDate date) {
    for (LocalDateTime t : starts(date.minusDays(8), date))
      if (t.isBefore(date.plusDays(1).atStartOfDay())
          && t.plusHours(mode.equals("hours") ? work : duration).isAfter(date.atStartOfDay()))
        return true;
    return false;
  }
}
