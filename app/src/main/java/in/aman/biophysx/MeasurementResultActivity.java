package in.aman.biophysx;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/** Final, user-friendly experimental session summary. */
public class MeasurementResultActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFFF7FBFF);
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        double bpm = getIntent().getDoubleExtra("bpm", Double.NaN);
        double temp = getIntent().getDoubleExtra("temp", Double.NaN);
        double score = getIntent().getDoubleExtra("score", 0.0);
        int samples = getIntent().getIntExtra("samples", 0);
        long duration = getIntent().getLongExtra("duration_ms", 0L);
        String quality = getIntent().getStringExtra("quality");
        if (quality == null) quality = "No usable optical signal";

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFFF7FBFF);
        root.addView(new FloatingBackgroundView(this), new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setPadding(dp(18), dp(18), dp(18), dp(24));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        TextView back = body("‹  BIOPHYSX", 15, true);
        back.setOnClickListener(v -> finish());
        page.addView(back);

        TextView title = body("Measurement Complete", 29, true);
        title.setPadding(0, dp(14), 0, 0);
        page.addView(title);
        TextView sub = body("A clear summary of this experimental session.", 15, false);
        sub.setTextColor(0xFF5E7285);
        page.addView(sub, lp(1, 5));

        LinearLayout hero = card(0xF2ECF7FF);
        TextView pulseLabel = body("PULSE", 13, true);
        pulseLabel.setTextColor(0xFF6C8093);
        pulseLabel.setGravity(Gravity.CENTER);
        hero.addView(pulseLabel);
        TextView pulseValue = body(Double.isFinite(bpm) ? String.format(Locale.US, "%.0f", bpm) : "—", 58, true);
        pulseValue.setGravity(Gravity.CENTER);
        pulseValue.setTextColor(0xFF17324D);
        hero.addView(pulseValue, lp(1, 2));
        TextView unit = body("BPM", 15, true);
        unit.setTextColor(0xFF5E7285);
        unit.setGravity(Gravity.CENTER);
        hero.addView(unit, lp(1, 0));
        page.addView(hero, lp(1, 18));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.addView(metric("SIGNAL QUALITY", quality, 0xFFE8FBF3), lp(1, 8));
        grid.addView(metric("CONTACT TEMPERATURE", Double.isFinite(temp) ? String.format(Locale.US, "%.1f °C", temp) : "—", 0xFFFFF1EA), lp(1, 8));
        grid.addView(metric("SIGNAL SCORE", String.format(Locale.US, "%.0f / 100", score * 100.0), 0xFFF1EDFF), lp(1, 8));
        grid.addView(metric("CAPTURE", String.format(Locale.US, "%.1f s  ·  %d samples", duration / 1000.0, samples), 0xFFEAF4FF), lp(1, 8));
        page.addView(grid, lp(1, 8));

        LinearLayout physics = card(0xF2FFFFFF);
        TextView ph = body("SEE THE PHYSICS", 13, true);
        ph.setTextColor(0xFF6C8093);
        physics.addView(ph);
        TextView pm = body("The optical sensor shines light into tissue. Blood-volume changes during each heartbeat slightly change the measured light. BIOPHYSX analyses that repeating optical signal to estimate pulse rate.", 14, false);
        pm.setTextColor(0xFF5E7285);
        pm.setPadding(0, dp(8), 0, 0);
        physics.addView(pm);
        page.addView(physics, lp(1, 14));

        TextView note = body("Experimental measurement only. A signal score describes the captured optical signal quality; it does not describe health status. Contact temperature is not core body temperature.", 12, false);
        note.setTextColor(0xFF718497);
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(8), dp(18), dp(8), dp(8));
        page.addView(note, lp(1, 4));

        Button done = button("Back to BIOPHYSX");
        done.setOnClickListener(v -> finish());
        page.addView(done, lp(1, 12));

        TextView footer = body("Made with ❤️ by Aman Kumar Patel", 13, true);
        footer.setTextColor(0xFF5E7285);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, lp(1, 18));
    }

    private LinearLayout metric(String label, String value, int fill) {
        LinearLayout c = card(fill);
        TextView l = body(label, 12, true); l.setTextColor(0xFF6C8093); c.addView(l);
        TextView v = body(value, 20, true); v.setPadding(0, dp(5), 0, 0); c.addView(v);
        return c;
    }
    private LinearLayout card(int fill) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16),dp(14),dp(16),dp(14)); c.setBackground(round(fill,0xFFDCE7F1,22)); return c;
    }
    private Button button(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(14); b.setAllCaps(false); b.setTextColor(0xFF17324D); b.setMinHeight(dp(58)); b.setBackground(round(0xEAF4FF,0xFFBFD9F4,19)); return b; }
    private TextView body(String s,float size,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xFF17324D); t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL)); return t; }
    private LinearLayout.LayoutParams lp(int w,int top){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(top); return p; }
    private android.graphics.drawable.GradientDrawable round(int fill,int stroke,int r){ android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(r)); g.setStroke(dp(1),stroke); return g; }
    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }
}
