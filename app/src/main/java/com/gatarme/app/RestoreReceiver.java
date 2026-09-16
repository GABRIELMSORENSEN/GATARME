package com.gatarme.app;

import android.content.*;

public class RestoreReceiver extends BroadcastReceiver {
  public void onReceive(Context c, Intent i) {
    Scheduler.all(c);
    Store.event(c, "Escalas restauradas: " + i.getAction());
  }
}
