package com.gatarme.app;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.webkit.*;
import java.io.*;
import java.time.*;
import org.json.*;

public class MainActivity extends Activity {
  WebView web;
  String mediaKind = "audio";

  public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(0xff09090b);
    web = new WebView(this);
    web.setBackgroundColor(0xff09090b);
    android.widget.FrameLayout root = new android.widget.FrameLayout(this);
    root.setBackgroundColor(0xff09090b);
    root.addView(web);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          v.setPadding(
              insets.getSystemWindowInsetLeft(),
              insets.getSystemWindowInsetTop(),
              insets.getSystemWindowInsetRight(),
              insets.getSystemWindowInsetBottom());
          return insets.consumeSystemWindowInsets();
        });
    setContentView(root);
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(true);
    web.getSettings().setAllowFileAccess(false);
    web.getSettings().setAllowContentAccess(false);
    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
    web.addJavascriptInterface(new Bridge(), "Native");
    web.setWebViewClient(
        new WebViewClient() {
          @Override
          public void onPageFinished(WebView view, String url) {
            if (!Store.prefs(MainActivity.this).getBoolean("onboarded", false)) {
              Store.prefs(MainActivity.this).edit().putBoolean("onboarded", true).apply();
              view.evaluateJavascript("tab('settings')", null);
              if (Build.VERSION.SDK_INT >= 33
                  && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                      != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, 88);
            }
          }

          public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
            return !"app.gatarme.local".equals(r.getUrl().getHost());
          }

          public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
            try {
              Uri u = r.getUrl();
              if (!"app.gatarme.local".equals(u.getHost()))
                return new WebResourceResponse(
                    "text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
              String p = u.getPath().substring(1);
              if (p.contains("..")) throw new IOException();
              if (p.startsWith("media/")) {
                String name = p.substring(6);
                File f = new File(Store.ctx(MainActivity.this).getFilesDir(), name);
                String type = name.endsWith(".mp4") ? "video/mp4" : "image/*";
                return new WebResourceResponse(type, null, new FileInputStream(f));
              }
              return new WebResourceResponse(
                  p.endsWith(".js")
                      ? "application/javascript"
                      : p.endsWith(".css")
                          ? "text/css"
                          : p.endsWith(".svg") ? "image/svg+xml" : "text/html",
                  "UTF-8",
                  getAssets().open(p.isEmpty() ? "index.html" : p));
            } catch (Exception e) {
              return new WebResourceResponse(
                  "text/plain",
                  "UTF-8",
                  404,
                  "Not found",
                  null,
                  new ByteArrayInputStream(new byte[0]));
            }
          }
        });
    web.loadUrl("https://app.gatarme.local/index.html");
    Scheduler.all(this);
  }

  protected void onResume() {
    super.onResume();
    if (web != null) web.evaluateJavascript("window.refresh&&refresh()", null);
  }

  void call(String fn, JSONObject o) {
    runOnUiThread(
        () -> web.evaluateJavascript("window." + fn + "&&" + fn + "(" + o.toString() + ")", null));
  }

  void message(String s) {
    runOnUiThread(
        () -> android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_LONG).show());
  }

  public class Bridge {
    @JavascriptInterface
    public String state() {
      try {
        JSONObject d = Store.data(MainActivity.this);
        JSONObject status = new JSONObject();
        NotificationManager nm = getSystemService(NotificationManager.class);
        status.put("exact", Scheduler.allowed(MainActivity.this));
        status.put("notifications", nm.areNotificationsEnabled());
        NotificationChannel ch = nm.getNotificationChannel(AlarmService.CHANNEL);
        status.put(
            "channel", ch == null || ch.getImportance() >= NotificationManager.IMPORTANCE_HIGH);
        status.put("fullscreen", Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent());
        status.put(
            "battery",
            getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName()));
        status.put("dnd", nm.isNotificationPolicyAccessGranted());
        status.put(
            "volume",
            getSystemService(AudioManager.class).getStreamVolume(AudioManager.STREAM_ALARM));
        status.put(
            "event", Store.prefs(MainActivity.this).getString("event", "Nenhum disparo ainda"));
        d.put("status", status);
        JSONArray a = d.getJSONArray("jobs");
        for (int i = 0; i < a.length(); i++) {
          JSONObject j = a.getJSONObject(i);
          j.put(
              "next",
              j.optBoolean("enabled", true)
                  ? Store.schedule(j).next(System.currentTimeMillis(), ZoneId.systemDefault())
                  : 0);
        }
        return d.toString();
      } catch (Exception e) {
        return "{}";
      }
    }

    @JavascriptInterface
    public String save(String json) {
      try {
        Store.save(MainActivity.this, json);
        return "ok";
      } catch (Exception e) {
        return e.getMessage();
      }
    }

    @JavascriptInterface
    public String calendar(String month) {
      try {
        LocalDate first = LocalDate.parse(month + "-01");
        JSONArray out = new JSONArray(), jobs = Store.jobs(MainActivity.this);
        for (int day = 1; day <= first.lengthOfMonth(); day++) {
          LocalDate date = first.withDayOfMonth(day);
          JSONObject cell = new JSONObject().put("date", date.toString());
          JSONArray ids = new JSONArray();
          for (int i = 0; i < jobs.length(); i++) {
            JSONObject j = jobs.getJSONObject(i);
            if (Store.schedule(j).covers(date)) ids.put(j.getString("id"));
          }
          out.put(cell.put("jobs", ids));
        }
        return out.toString();
      } catch (Exception e) {
        return "[]";
      }
    }

    @JavascriptInterface
    public void test() {
      runOnUiThread(
          () -> {
            if (!Scheduler.allowed(MainActivity.this)) {
              message("Permita alarmes exatos antes de testar");
              return;
            }
            Scheduler.at(MainActivity.this, "test", System.currentTimeMillis() + 15000, false);
            message("Teste em 15 segundos. Bloqueie a tela para verificar.");
          });
    }

    @JavascriptInterface
    public void permission(String kind) {
      runOnUiThread(
          () -> {
            try {
              Intent i;
              switch (kind) {
                case "notifications":
                  if (Build.VERSION.SDK_INT >= 33
                      && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                          != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, 88);
                    return;
                  }
                  i =
                      new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                          .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                  break;
                case "exact":
                  if (Build.VERSION.SDK_INT < 31) return;
                  i =
                      new Intent(
                          Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                          Uri.parse("package:" + getPackageName()));
                  break;
                case "fullscreen":
                  if (Build.VERSION.SDK_INT < 34) return;
                  i =
                      new Intent(
                          Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                          Uri.parse("package:" + getPackageName()));
                  break;
                case "battery":
                  i =
                      new Intent(
                          Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                          Uri.parse("package:" + getPackageName()));
                  break;
                case "dnd":
                  i = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                  break;
                case "volume":
                  getSystemService(AudioManager.class)
                      .adjustStreamVolume(
                          AudioManager.STREAM_ALARM,
                          AudioManager.ADJUST_SAME,
                          AudioManager.FLAG_SHOW_UI);
                  return;
                default:
                  i =
                      new Intent(
                          Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                          Uri.parse("package:" + getPackageName()));
              }
              startActivity(i);
            } catch (Exception e) {
              message("Abra Ajustes > Aplicativos > GATARME: " + e.getMessage());
            }
          });
    }

    @JavascriptInterface
    public void media(String kind) {
      if (!java.util.Arrays.asList("audio", "image", "video").contains(kind)) return;
      runOnUiThread(
          () -> {
            mediaKind = kind;
            Intent i =
                new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .setType(kind + "/*")
                    .addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, 61);
          });
    }

    @JavascriptInterface
    public void youtube() {
      runOnUiThread(
          () -> startActivityForResult(new Intent(MainActivity.this, YoutubeActivity.class), 62));
    }
  }

  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null) return;
    if (request == 62) {
      try {
        call("onYoutube", new JSONObject().put("id", data.getStringExtra("video")));
      } catch (Exception ignored) {
      }
      return;
    }
    if (request != 61 || data.getData() == null) return;
    final Uri uri = data.getData();
    final String kind = mediaKind;
    message("Importando mídia para manter acesso offline?");
    new Thread(
            () -> {
              File target = null;
              try {
                String ext =
                    kind.equals("audio") ? ".audio" : kind.equals("video") ? ".mp4" : ".image";
                String name = "media_" + java.util.UUID.randomUUID() + ext;
                target = new File(Store.ctx(this).getFilesDir(), name);
                try (InputStream in = getContentResolver().openInputStream(uri);
                    OutputStream out = new FileOutputStream(target)) {
                  byte[] buf = new byte[65536];
                  int n;
                  long count = 0;
                  while ((n = in.read(buf)) != -1) {
                    count += n;
                    if (count > 200L * 1024 * 1024)
                      throw new IOException("Limite de 200 MB por arquivo");
                    out.write(buf, 0, n);
                  }
                }
                String label = uri.getLastPathSegment();
                try (android.database.Cursor c =
                    getContentResolver()
                        .query(
                            uri,
                            new String[] {android.provider.OpenableColumns.DISPLAY_NAME},
                            null,
                            null,
                            null)) {
                  if (c != null && c.moveToFirst()) label = c.getString(0);
                }
                call(
                    "onMedia",
                    new JSONObject().put("kind", kind).put("file", name).put("name", label));
              } catch (Exception e) {
                if (target != null) target.delete();
                message("Falha ao importar: " + e.getMessage());
              }
            },
            "media-import")
        .start();
  }

  @Override
  public void onBackPressed() {
    web.evaluateJavascript("window.goBack&&goBack()", null);
  }
}
