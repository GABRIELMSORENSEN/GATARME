package com.gatarme.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import org.json.*;

public class RingActivity extends Activity {
  int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  VideoView video;
  WebView youtube;
  boolean resumed;
  Handler handler = new Handler();
  long heartbeat = 0;
  BroadcastReceiver update =
      new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
          if (AlarmService.current.isEmpty()) finish();
          else draw();
        }
      };

  @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
  public void onCreate(Bundle b) {
    super.onCreate(b);
    setShowWhenLocked(true);
    setTurnScreenOn(true);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    if (Build.VERSION.SDK_INT >= 33)
      registerReceiver(
          update, new IntentFilter("com.gatarme.UPDATE"), Context.RECEIVER_NOT_EXPORTED);
    else registerReceiver(update, new IntentFilter("com.gatarme.UPDATE"));
    if (AlarmService.current.isEmpty()) {
      finish();
      return;
    }
    draw();
    handler.postDelayed(watchdog, 2000);
  }

  final Runnable watchdog =
      new Runnable() {
        public void run() {
          if (System.currentTimeMillis() - heartbeat > 5000) AlarmService.backup(true);
          handler.postDelayed(this, 2000);
        }
      };

  void draw() {
    if (video != null) video.stopPlayback();
    if (youtube != null) {
      youtube.destroy();
      youtube = null;
    }
    JSONObject j = Store.job(this, AlarmService.current);
    if (j == null) j = new JSONObject();
    FrameLayout frame = new FrameLayout(this);
    int accent = 0xffff304f;
    try {
      JSONObject t = Store.data(this).optJSONObject("theme");
      if (t != null) accent = Color.parseColor(t.optString("accent", "#ff304f"));
    } catch (Exception ignored) {
    }
    frame.setBackgroundColor(0xff09090b);
    String bg = j.optString("background"), type = j.optString("backgroundType");
    File file = new File(Store.ctx(this).getFilesDir(), bg);
    if (!bg.isEmpty() && file.isFile()) {
      if (type.equals("video")) {
        video = new VideoView(this);
        frame.addView(video, new FrameLayout.LayoutParams(-1, -1));
        video.setVideoURI(Uri.fromFile(file));
        video.setOnPreparedListener(
            p -> {
              p.setLooping(true);
              p.setVolume(0, 0);
              video.start();
              Store.event(this, "VIDEO_ATIVO " + AlarmService.current);
            });
      } else {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        android.graphics.BitmapFactory.Options options =
            new android.graphics.BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(file.getPath(), options);
        options.inSampleSize = Math.max(1, Math.max(options.outWidth, options.outHeight) / 1600);
        options.inJustDecodeBounds = false;
        image.setImageBitmap(android.graphics.BitmapFactory.decodeFile(file.getPath(), options));
        frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
      }
    }
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setGravity(Gravity.CENTER);
    content.setPadding(dp(24), dp(24), dp(24), dp(16));
    content.setBackgroundColor(0x99000000);
    frame.addView(content, new FrameLayout.LayoutParams(-1, -1));
    text(content, "G A T A R M E", 15, 0xffdddddd);
    TextClock clock = new TextClock(this);
    clock.setFormat12Hour("HH:mm");
    clock.setFormat24Hour("HH:mm");
    clock.setTextSize(72);
    clock.setTextColor(-1);
    clock.setGravity(Gravity.CENTER);
    content.addView(clock);
    text(content, j.optString("name", "Teste do despertador"), 26, -1);
    text(content, j.optString("message", "Seu próximo plantão começa com você."), 17, 0xffeeeeee);
    String yt = j.optString("youtube");
    boolean hasYoutube = yt.matches("[a-zA-Z0-9_-]{11}");
    KeyguardManager keyguard = getSystemService(KeyguardManager.class);
    if (hasYoutube && keyguard.isKeyguardLocked()) {
      Button unlock = new Button(this);
      unlock.setText("Desbloquear para reproduzir YouTube");
      content.addView(unlock);
      unlock.setOnClickListener(
          v ->
              keyguard.requestDismissKeyguard(
                  this,
                  new KeyguardManager.KeyguardDismissCallback() {
                    public void onDismissSucceeded() {
                      draw();
                    }
                  }));
    }
    if (hasYoutube && !keyguard.isKeyguardLocked()) {
      youtube = new WebView(this);
      youtube.setBackgroundColor(0xff000000);
      youtube.getSettings().setJavaScriptEnabled(true);
      youtube.getSettings().setMediaPlaybackRequiresUserGesture(false);
      youtube.addJavascriptInterface(
          new Object() {
            @JavascriptInterface
            public void playing() {
              heartbeat = System.currentTimeMillis();
              if (resumed
                  && !getSystemService(KeyguardManager.class).isKeyguardLocked()
                  && getSystemService(android.media.AudioManager.class)
                          .getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                      > 0) AlarmService.backup(false);
            }

            @JavascriptInterface
            public void waiting() {
              AlarmService.backup(true);
            }
          },
          "AudioSafety");
      String html =
          "<html><meta name='viewport' content='width=device-width'><body"
              + " style='margin:0;background:black'><div id='p'></div><script"
              + " src='https://www.youtube.com/iframe_api'></script><script>var p;function"
              + " onYouTubeIframeAPIReady(){p=new"
              + " YT.Player('p',{width:'100%',height:'230',videoId:'"
              + yt
              + "',playerVars:{origin:'https://com.gatarme.app',autoplay:1,playsinline:1,loop:1,playlist:'"
              + yt
              + "'},events:{onStateChange:function(e){if(e.data!==1)AudioSafety.waiting()},onError:function(){AudioSafety.waiting()}}});setInterval(function(){if(p&&p.getPlayerState&&p.getPlayerState()===1&&!p.isMuted()&&p.getVolume()>0)AudioSafety.playing();else"
              + " AudioSafety.waiting()},1000)}</script></body></html>";
      youtube.loadDataWithBaseURL("https://com.gatarme.app", html, "text/html", "UTF-8", null);
      content.addView(youtube, new LinearLayout.LayoutParams(-1, dp(230)));
    }
    View space = new View(this);
    content.addView(space, new LinearLayout.LayoutParams(1, 0, 1));
    Button snooze = new Button(this);
    snooze.setText(
        "Adiar " + j.optInt("snooze", 5) + (j.optInt("snooze", 5) == 1 ? " minuto" : " minutos"));
    content.addView(snooze, new LinearLayout.LayoutParams(-1, dp(64)));
    snooze.setOnClickListener(v -> act("snooze"));
    SwipeStopView stop = new SwipeStopView(this, accent, () -> act("stop"));
    LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(-1, dp(80));
    stopParams.topMargin = dp(16);
    content.addView(stop, stopParams);
    text(content, "Sua escala continua ativa para os próximos dias.", 13, 0xffdddddd);
    frame.setOnApplyWindowInsetsListener(
        (v, i) -> {
          v.setPadding(
              i.getSystemWindowInsetLeft(),
              i.getSystemWindowInsetTop(),
              i.getSystemWindowInsetRight(),
              i.getSystemWindowInsetBottom());
          return i.consumeSystemWindowInsets();
        });
    setContentView(frame);
  }

  void text(LinearLayout l, String s, int size, int color) {
    TextView v = new TextView(this);
    v.setText(s);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setGravity(Gravity.CENTER);
    v.setPadding(0, 8, 0, 8);
    l.addView(v);
  }

  void act(String a) {
    startService(new Intent(this, AlarmService.class).setAction(a));
  }

  protected void onResume() {
    super.onResume();
    resumed = true;
    if (youtube != null) youtube.onResume();
  }

  protected void onPause() {
    resumed = false;
    AlarmService.backup(true);
    if (youtube != null) {
      youtube.evaluateJavascript("if(p&&p.pauseVideo)p.pauseVideo()", null);
      youtube.onPause();
    }
    super.onPause();
  }

  protected void onDestroy() {
    unregisterReceiver(update);
    handler.removeCallbacksAndMessages(null);
    if (video != null) video.stopPlayback();
    if (youtube != null) youtube.destroy();
    AlarmService.backup(true);
    super.onDestroy();
  }

  public void onBackPressed() {
    moveTaskToBack(true);
  }
}
