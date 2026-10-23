package com.gatarme.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

/** A deliberate drag, with a separate accessible action for screen readers. */
@android.annotation.SuppressLint("ViewConstructor")
public final class SwipeStopView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final int accent;
  private final Runnable stop;
  private float progress, downX;
  private int pointer = -1;
  private boolean completed;

  public SwipeStopView(Context context, int accent, Runnable stop) {
    super(context);
    this.accent = accent;
    this.stop = stop;
    setFocusable(true);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    setContentDescription("Arraste para a direita para parar o alarme");
  }

  private float dp(float value) {
    return value * getResources().getDisplayMetrics().density;
  }

  private float radius() { return Math.min(dp(32), getHeight() / 2f - dp(8)); }
  private float start() { return dp(8) + radius(); }
  private float distance() { return Math.max(1, getWidth() - 2 * start()); }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float cy = getHeight() / 2f, r = radius();
    paint.setColor(0xe6222229);
    canvas.drawRoundRect(0, 0, getWidth(), getHeight(), cy, cy, paint);
    paint.setColor(0xfffafafa);
    paint.setAlpha(Math.round(255 * (1 - progress)));
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTextSize(Math.min(dp(16), (getWidth() - 2 * r - dp(24)) / 13));
    canvas.drawText("Arraste para parar", getWidth() / 2f + r / 2,
        cy - (paint.ascent() + paint.descent()) / 2, paint);
    paint.setColor(accent);
    float cx = start() + distance() * progress;
    canvas.drawCircle(cx, cy, r, paint);
    paint.setColor(0xffffffff);
    paint.setStrokeWidth(dp(3));
    paint.setStrokeCap(Paint.Cap.ROUND);
    canvas.drawLine(cx - dp(5), cy - dp(8), cx + dp(4), cy, paint);
    canvas.drawLine(cx + dp(4), cy, cx - dp(5), cy + dp(8), paint);
  }

  // A regular touch click must never dismiss; accessibility has its own explicit action.
  @android.annotation.SuppressLint("ClickableViewAccessibility")
  @Override public boolean onTouchEvent(MotionEvent event) {
    if (completed) return true;
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        if (Math.abs(event.getX() - start()) > radius() + dp(8)) return false;
        pointer = event.getPointerId(0);
        downX = event.getX();
        getParent().requestDisallowInterceptTouchEvent(true);
        return true;
      case MotionEvent.ACTION_MOVE:
        int index = event.findPointerIndex(pointer);
        if (index < 0) { reset(); return true; }
        progress = Math.max(0, Math.min(1, (event.getX(index) - downX) / distance()));
        invalidate();
        return true;
      case MotionEvent.ACTION_UP:
        if (pointer != -1 && progress >= .85f) complete();
        else reset();
        getParent().requestDisallowInterceptTouchEvent(false);
        return true;
      case MotionEvent.ACTION_POINTER_UP:
        if (event.getPointerId(event.getActionIndex()) == pointer) reset();
        return true;
      case MotionEvent.ACTION_CANCEL:
        reset();
        return true;
      default:
        return true;
    }
  }

  private void reset() { pointer = -1; progress = 0; invalidate(); }

  private void complete() {
    if (completed) return;
    completed = true;
    progress = 1;
    invalidate();
    performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
    stop.run();
  }

  @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(info);
    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(
        AccessibilityNodeInfo.ACTION_CLICK, "Parar este alarme"));
  }

  @Override public boolean performAccessibilityAction(int action, Bundle args) {
    if (action == AccessibilityNodeInfo.ACTION_CLICK) { complete(); return true; }
    return super.performAccessibilityAction(action, args);
  }
}
