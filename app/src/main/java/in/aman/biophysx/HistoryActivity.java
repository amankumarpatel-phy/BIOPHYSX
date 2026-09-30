package in.aman.biophysx;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

public class HistoryActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFFF7FBFF);
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ScrollView scroll = new ScrollView(this);
        scroll.setPadding(dp(18), dp(18), dp(18), dp(24));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        TextView back = body("‹  BIOPHYSX", 15, true); back.setOnClickListener(v -> finish());
        page.addView(back);
        TextView title = body("Measurement History", 28, true); page.addView(title, lp(1, 14));
        TextView sub = body("Recent experimental sessions stored only on this device.", 14, false); sub.setTextColor(0xFF5E7285); page.addView(sub, lp(1, 4));

        JSONArray arr = HistoryStore.get(this);
        if (arr.length() == 0) {
            TextView empty = body("No measurements saved yet.\n\nYour first completed session will appear here.", 16, false);
            empty.setTextColor(0xFF5E7285); empty.setGravity(Gravity.CENTER); empty.setPadding(dp(16), dp(50), dp(16), dp(50));
            page.addView(empty, lp(1, 22));
        } else {
            for (int i = 0; i < arr.length(); i++) {
                try {
                    JSONObject o = arr.getJSONObject(i);
                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(dp(16), dp(14), dp(16), dp(14));
                    card.setBackground(round(0xEFFFFFFF, 0xFFDCE7F1, 20));
                    TextView time = body(o.optString("time"), 13, true); time.setTextColor(0xFF6C8093); card.addView(time);
                    TextView values = body("♥  " + o.optString("bpm", "—") + " BPM    ·    🌡  " + o.optString("temp", "—") + " °C", 20, true); card.addView(values, lp(1, 7));
                    TextView q = body(o.optString("quality"), 13, false); q.setTextColor(0xFF5E7285); card.addView(q, lp(1, 4));
                    double score = o.optDouble("signalScore", -1);
                    long durationMs = o.optLong("durationMs", 0);
                    if (score >= 0 || durationMs > 0) {
                        String detail = (score >= 0 ? String.format(java.util.Locale.US, "Signal score %.0f/100", score * 100.0) : "") +
                                (durationMs > 0 ? String.format(java.util.Locale.US, "   ·   %.1f s", durationMs / 1000.0) : "");
                        TextView d = body(detail, 12, false); d.setTextColor(0xFF718497); card.addView(d, lp(1, 3));
                    }
                    page.addView(card, lp(1, 10));
                } catch (Exception ignored) {}
            }
        }
        TextView note = body("BIOPHYSX history is for personal learning and experimental tracking. It is not a clinical record.", 12, false);
        note.setTextColor(0xFF718497); note.setGravity(Gravity.CENTER); note.setPadding(dp(8), dp(22), dp(8), dp(8)); page.addView(note, lp(1, 12));
        TextView footer = body("Made with ❤️ by Aman Kumar Patel", 13, true); footer.setTextColor(0xFF5E7285); footer.setGravity(Gravity.CENTER); page.addView(footer, lp(1, 12));
    }

    private TextView body(String s,float size,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xFF17324D); t.setTypeface(Typeface.create("sans", bold?Typeface.BOLD:Typeface.NORMAL)); return t; }
    private LinearLayout.LayoutParams lp(int w,int top){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(top); return p; }
    private android.graphics.drawable.GradientDrawable round(int fill,int stroke,int r){ android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(r)); g.setStroke(dp(1),stroke); return g; }
    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }
}
