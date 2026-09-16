package com.gatarme.app;

import android.content.*;
import java.time.*;
import java.util.*;
import org.json.*;

public final class Store {
  public static Context ctx(Context c) {
    return c.createDeviceProtectedStorageContext();
  }

  public static android.content.SharedPreferences prefs(Context c) {
    return ctx(c).getSharedPreferences("gatarme", 0);
  }

  public static JSONObject data(Context c) {
    try {
      return new JSONObject(prefs(c).getString("data", "{\"jobs\":[],\"theme\":{}}"));
    } catch (Exception e) {
      return new JSONObject();
    }
  }

  public static JSONArray jobs(Context c) {
    return data(c).optJSONArray("jobs") == null ? new JSONArray() : data(c).optJSONArray("jobs");
  }

  public static JSONObject job(Context c, String id) {
    JSONArray a = jobs(c);
    for (int i = 0; i < a.length(); i++)
      if (a.optJSONObject(i).optString("id").equals(id)) return a.optJSONObject(i);
    return null;
  }

  public static Schedule schedule(JSONObject j) throws Exception {
    Schedule s = new Schedule();
    s.mode = j.getString("mode");
    s.anchor = LocalDate.parse(j.getString("anchor"));
    s.start = LocalTime.parse(j.getString("start"));
    s.work = j.optInt("work", 12);
    s.rest = j.optInt("rest", 36);
    s.duration = j.optInt("duration", 12);
    s.pattern = j.optString("pattern", "10");
    s.weekdays.clear();
    JSONArray w = j.optJSONArray("weekdays");
    if (w != null) for (int i = 0; i < w.length(); i++) s.weekdays.add(w.getInt(i));
    s.offsets.clear();
    JSONArray o = j.getJSONArray("offsets");
    for (int i = 0; i < o.length(); i++) s.offsets.add(o.getInt(i));
    JSONObject dt = j.optJSONObject("dayTimes");
    if (dt != null) {
      Iterator<String> it = dt.keys();
      while (it.hasNext()) {
        String k = it.next();
        s.dayTimes.put(Integer.parseInt(k), LocalTime.parse(dt.getString(k)));
      }
    }
    JSONObject pt = j.optJSONObject("patternTimes");
    if (pt != null) {
      Iterator<String> keys = pt.keys();
      while (keys.hasNext()) {
        String k = keys.next();
        s.patternTimes.put(Integer.parseInt(k), LocalTime.parse(pt.getString(k)));
      }
    }
    JSONArray ex = j.optJSONArray("skip");
    if (ex != null)
      for (int i = 0; i < ex.length(); i++) s.skip.add(LocalDate.parse(ex.getString(i)));
    if (!Arrays.asList("hours", "days", "weekly", "pattern").contains(s.mode)
        || s.work < 1
        || s.rest < 0
        || s.work > 365
        || s.rest > 365
        || s.duration < 1
        || s.duration > 168
        || s.offsets.isEmpty()
        || s.offsets.size() > 12
        || !s.pattern.matches("[01]{1,366}")
        || !s.pattern.contains("1")
        || (s.mode.equals("weekly") && s.weekdays.isEmpty()))
      throw new Exception("Escala inválida");
    if (s.mode.equals("hours") && s.work > 168)
      throw new Exception("Plantão deve ter até 168 horas");
    for (int n : s.offsets)
      if (n < -10080 || n > 10080) throw new Exception("Antecedência máxima: 7 dias");
    return s;
  }

  public static synchronized void save(Context c, String json) throws Exception {
    JSONObject d = new JSONObject(json);
    JSONArray a = d.getJSONArray("jobs");
    Set<String> ids = new HashSet<>();
    if (a.length() > 50) throw new Exception("Limite de 50 escalas");
    for (int i = 0; i < a.length(); i++) {
      JSONObject j = a.getJSONObject(i);
      schedule(j);
      String id = j.getString("id");
      if (!id.matches("[a-zA-Z0-9_-]{1,80}")
          || !ids.add(id)
          || j.optString("name").trim().isEmpty())
        throw new Exception("Nome ou identificador inválido");
    }
    JSONArray old = jobs(c);
    for (int i = 0; i < old.length(); i++) {
      JSONObject prev = old.getJSONObject(i);
      JSONObject replacement = null;
      for (int k = 0; k < a.length(); k++)
        if (a.getJSONObject(k).optString("id").equals(prev.optString("id")))
          replacement = a.getJSONObject(k);
      prev.remove("next");
      if (replacement != null) replacement.remove("next");
      if (replacement == null || !prev.toString().equals(replacement.toString()))
        Scheduler.cancel(c, prev.getString("id"));
    }
    if (!prefs(c).edit().putString("data", d.toString()).commit())
      throw new Exception("Falha ao salvar");
    Scheduler.all(c);
  }

  public static void event(Context c, String s) {
    prefs(c).edit().putString("event", java.time.ZonedDateTime.now() + " · " + s).apply();
    android.util.Log.i("GATARME", s);
  }
}
