package com.gatarme.app;

import android.content.*;
import org.json.*;

public class AlarmReceiver extends BroadcastReceiver {
  public void onReceive(Context c, Intent i) {
    String id = i.getStringExtra("id");
    JSONObject j = Store.job(c, id);
    if (j == null && !"test".equals(id)) return;
    if (j != null && !j.optBoolean("enabled", true)) return;
    if (j != null && !i.getBooleanExtra("snooze", false)) Scheduler.one(c, j);
    if (i.getBooleanExtra("snooze", false)) Store.prefs(c).edit().remove("snooze_" + id).apply();
    Store.event(c, "DISPARO " + id);
    c.startForegroundService(new Intent(c, AlarmService.class).putExtra("id", id));
  }
}
