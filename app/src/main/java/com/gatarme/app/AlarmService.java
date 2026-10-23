package com.gatarme.app;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.provider.Settings;
import java.util.*;
import org.json.*;

public class AlarmService extends Service {
  public static final String CHANNEL = "wake_v1";
  public static String current = "";
  static AlarmService instance;

  public static void backup(boolean audible) {
    AlarmService s = instance;
    if (s != null && s.player != null)
      try {
        s.player.setVolume(audible ? 1 : 0, audible ? 1 : 0);
      } catch (Exception ignored) {
      }
  }

  MediaPlayer player;
  Vibrator vibrator;
  PowerManager.WakeLock lock;
  LinkedHashSet<String> queue = new LinkedHashSet<>();

  public void onCreate() {
    super.onCreate();
    instance = this;
    NotificationManager nm = getSystemService(NotificationManager.class);
    NotificationChannel channel =
        new NotificationChannel(CHANNEL, "Alarmes de plantão", NotificationManager.IMPORTANCE_HIGH);
    channel.setSound(null, null);
    channel.setDescription("Despertador com tela cheia e som persistente");
    channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
    nm.createNotificationChannel(channel);
    lock =
        getSystemService(PowerManager.class)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GATARME:ring");
    lock.acquire(12 * 60 * 60 * 1000L);
  }

  PendingIntent action(String a) {
    return PendingIntent.getService(
        this,
        a.hashCode(),
        new Intent(this, AlarmService.class).setAction(a),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  public int onStartCommand(Intent in, int flags, int startId) {
    if (in != null && ("stop".equals(in.getAction()) || "snooze".equals(in.getAction()))) {
      if ("snooze".equals(in.getAction())) {
        JSONObject j = Store.job(this, current);
        int mins = j == null ? 5 : j.optInt("snooze", 5);
        long when = System.currentTimeMillis() + mins * 60000L;
        Store.prefs(this).edit().putLong("snooze_" + current, when).apply();
        Scheduler.at(this, current, when, true);
      }
      queue.remove(current);
      stopSound();
      if (queue.isEmpty()) {
        current = "";
        Store.prefs(this).edit().remove("ringing").apply();
        sendBroadcast(new Intent("com.gatarme.UPDATE").setPackage(getPackageName()));
        stopSelf();
        return START_NOT_STICKY;
      }
    } else {
      String id = in == null ? Store.prefs(this).getString("ringing", "") : in.getStringExtra("id");
      if (id != null && !id.isEmpty()) queue.add(id);
    }
    if (queue.isEmpty()) {
      stopSelf();
      return START_NOT_STICKY;
    }
    String id = queue.iterator().next();
    boolean changed = !id.equals(current) || player == null;
    current = id;
    Store.prefs(this).edit().putString("ringing", id).apply();
    JSONObject j = Store.job(this, id);
    String name = j == null ? "Teste do despertador" : j.optString("name");
    Intent full =
        new Intent(this, RingActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pi =
        PendingIntent.getActivity(
            this, 7, full, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification n =
        new Notification.Builder(this, CHANNEL)
            .setSmallIcon(com.gatarme.app.R.drawable.ic_notification)
            .setContentTitle(name)
            .setContentText("Seu plantão está chegando · toque para abrir")
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .addAction(0, "Adiar", action("snooze"))
            .addAction(0, "Abrir alarme", pi)
            .build();
    startForeground(41, n);
    if (changed) {
      stopSound();
      play(j);
      // Full-screen notifications become banners while the phone is unlocked.
      // The user-authorized overlay permission also permits this activity launch.
      if (Settings.canDrawOverlays(this) || MainActivity.visible) {
        try {
          startActivity(full);
        } catch (RuntimeException e) {
          Store.event(this, "Tela do alarme: " + e.getMessage());
        }
      }
      sendBroadcast(new Intent("com.gatarme.UPDATE").setPackage(getPackageName()));
    }
    return START_STICKY;
  }

  void play(JSONObject j) {
    String audio = j == null ? "" : j.optString("audio");
    try {
      player = new MediaPlayer();
      player.setAudioAttributes(
          new AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ALARM)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build());
      if (!audio.isEmpty() && new java.io.File(Store.ctx(this).getFilesDir(), audio).isFile())
        player.setDataSource(new java.io.File(Store.ctx(this).getFilesDir(), audio).getPath());
      else player.setDataSource(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
      player.setLooping(true);
      player.prepare();
      player.start();
      Store.event(this, "AUDIO_ATIVO " + current);
    } catch (Exception e) {
      try {
        if (player != null) player.release();
        player = new MediaPlayer();
        player.setAudioAttributes(
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        android.content.res.AssetFileDescriptor afd = getAssets().openFd("fallback.wav");
        player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        afd.close();
        player.setLooping(true);
        player.prepare();
        player.start();
        Store.event(this, "AUDIO_RESERVA " + current);
      } catch (Exception ex) {
        Store.event(this, "Falha de áudio: " + ex);
      }
    }
    vibrator = getSystemService(Vibrator.class);
    if (j == null || j.optBoolean("vibrate", true))
      vibrator.vibrate(
          VibrationEffect.createWaveform(new long[] {0, 700, 350, 700, 800}, 0),
          new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
  }

  void stopSound() {
    if (player != null) {
      player.release();
      player = null;
    }
    if (vibrator != null) vibrator.cancel();
  }

  public void onDestroy() {
    instance = null;
    stopSound();
    if (lock != null && lock.isHeld()) lock.release();
    current = "";
    super.onDestroy();
  }

  public IBinder onBind(Intent i) {
    return null;
  }
}
