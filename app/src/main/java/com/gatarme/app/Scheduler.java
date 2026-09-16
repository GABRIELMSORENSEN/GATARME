package com.gatarme.app;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import java.time.*;
import org.json.*;

public final class Scheduler {
  static AlarmManager manager(Context c) {
    return c.getSystemService(AlarmManager.class);
  }

  public static boolean allowed(Context c) {
    return Build.VERSION.SDK_INT < 31 || manager(c).canScheduleExactAlarms();
  }

  static PendingIntent pending(Context c, String id, boolean snooze) {
    Intent i =
        new Intent(c, AlarmReceiver.class)
            .setAction("RING")
            .setData(Uri.parse("gatarme://" + (snooze ? "snooze/" : "alarm/") + id))
            .putExtra("id", id)
            .putExtra("snooze", snooze);
    return PendingIntent.getBroadcast(
        c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  public static void at(Context c, String id, long time, boolean snooze) {
    if (!allowed(c)) return;
    PendingIntent show =
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    manager(c).setAlarmClock(new AlarmManager.AlarmClockInfo(time, show), pending(c, id, snooze));
    Store.event(c, "Agendado " + id + " para " + Instant.ofEpochMilli(time));
  }

  public static void cancel(Context c, String id) {
    manager(c).cancel(pending(c, id, false));
    manager(c).cancel(pending(c, id, true));
    Store.prefs(c).edit().remove("snooze_" + id).apply();
  }

  public static void one(Context c, JSONObject j) {
    try {
      if (j.optBoolean("enabled", true)) {
        long next =
            Store.schedule(j).next(System.currentTimeMillis() + 1000, ZoneId.systemDefault());
        if (next > 0) at(c, j.getString("id"), next, false);
      }
    } catch (Exception e) {
      Store.event(c, "Falha no agendamento: " + e.getMessage());
    }
  }

  public static void all(Context c) {
    JSONArray a = Store.jobs(c);
    for (int i = 0; i < a.length(); i++) {
      JSONObject j = a.optJSONObject(i);
      one(c, j);
      long sn = Store.prefs(c).getLong("snooze_" + j.optString("id"), 0);
      if (sn > System.currentTimeMillis() && j.optBoolean("enabled", true))
        at(c, j.optString("id"), sn, true);
    }
  }
}
