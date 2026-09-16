package com.gatarme.app;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.webkit.*;
import android.widget.*;

public class YoutubeActivity extends Activity {
  WebView web;
  EditText link;

  public static String videoId(String url) {
    try {
      Uri u = Uri.parse(url);
      String host = u.getHost();
      if (host == null) return url.matches("[A-Za-z0-9_-]{11}") ? url : "";
      String id = "";
      if (host.equals("youtu.be")) id = u.getLastPathSegment();
      else if (host.equals("youtube.com") || host.endsWith(".youtube.com")) {
        id = u.getQueryParameter("v");
        if (id == null && (u.getPath().startsWith("/shorts/") || u.getPath().startsWith("/embed/")))
          id = u.getLastPathSegment();
      }
      return id != null && id.matches("[A-Za-z0-9_-]{11}") ? id : "";
    } catch (Exception e) {
      return "";
    }
  }

  public void onCreate(Bundle b) {
    super.onCreate(b);
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(12, 12, 12, 8);
    l.setBackgroundColor(0xff09090b);
    TextView title = new TextView(this);
    title.setText("YouTube · escolha um vídeo");
    title.setTextColor(-1);
    title.setTextSize(22);
    l.addView(title);
    link = new EditText(this);
    link.setTextColor(-1);
    link.setHintTextColor(0xffaaaaaa);
    link.setHint("Pesquisar ou colar link do YouTube");
    l.addView(link);
    Button search = new Button(this);
    search.setText("Buscar / abrir link");
    l.addView(search);
    Button select = new Button(this);
    select.setText("Usar este vídeo no alarme");
    l.addView(select);
    TextView help = new TextView(this);
    help.setText(
        "Reprodução online em tela desbloqueada. Um som local continua disponível se o vídeo não"
            + " iniciar.");
    help.setTextColor(0xffbbbbbb);
    l.addView(help);
    web = new WebView(this);
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(true);
    web.getSettings().setAllowFileAccess(false);
    web.setWebViewClient(
        new WebViewClient() {
          public boolean shouldOverrideUrlLoading(WebView w, WebResourceRequest r) {
            String host = r.getUrl().getHost();
            return !"https".equals(r.getUrl().getScheme())
                || host == null
                || !(host.equals("youtube.com")
                    || host.endsWith(".youtube.com")
                    || host.equals("accounts.google.com"));
          }
        });
    l.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
    l.setOnApplyWindowInsetsListener(
        (v, i) -> {
          v.setPadding(
              i.getSystemWindowInsetLeft(),
              i.getSystemWindowInsetTop(),
              i.getSystemWindowInsetRight(),
              i.getSystemWindowInsetBottom());
          return i.consumeSystemWindowInsets();
        });
    setContentView(l);
    web.loadUrl("https://m.youtube.com");
    search.setOnClickListener(
        v -> {
          String text = link.getText().toString().trim(), id = videoId(text);
          web.loadUrl(
              id.isEmpty()
                  ? "https://m.youtube.com/results?search_query=" + Uri.encode(text)
                  : "https://m.youtube.com/watch?v=" + id);
        });
    select.setOnClickListener(
        v -> {
          String id = videoId(web.getUrl());
          if (id.isEmpty()) id = videoId(link.getText().toString().trim());
          if (id.isEmpty()) {
            Toast.makeText(this, "Abra um vídeo ou cole o link dele", Toast.LENGTH_LONG).show();
            return;
          }
          setResult(RESULT_OK, new Intent().putExtra("video", id));
          finish();
        });
  }

  protected void onDestroy() {
    if (web != null) web.destroy();
    super.onDestroy();
  }
}
