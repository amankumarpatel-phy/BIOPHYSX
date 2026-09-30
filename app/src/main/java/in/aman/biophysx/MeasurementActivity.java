package in.aman.biophysx;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class MeasurementActivity extends Activity implements BleManager.Listener {
    private static final int REQ_BT = 43;
    private static final long SESSION_MS = 30_000L;
    private static final long STABILIZE_MS = 5_000L;

    private BleManager ble;
    private WaveformView waveform;
    private TextView status, instruction, pulse, signal, timer, temp, phase;
    private ProgressBar progress;
    private Button connect;
    private long sessionStart = 0L;
    private boolean running = false;
    private boolean fingerPresent = false;
    private boolean connected = false;
    private boolean finalized = false;
    private double bestTemp = Double.NaN;
    private final Deque<Double> recentPpg = new ArrayDeque<>();
    private final List<Double> bpmSamples = new ArrayList<>();
    private final PpgSignalEngine signalEngine = new PpgSignalEngine(80);
    private PpgSignalEngine.Result lastQuality = new PpgSignalEngine.Result("Place finger", 0, 0, 0, 0);
    private final Handler handler = new Handler();

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(0xFFF7FBFF);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ble = new BleManager(this, this);
        build();
    }

    private void build() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFFF7FBFF);
        root.addView(new FloatingBackgroundView(this), new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(18), dp(18), dp(18), dp(20));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        TextView back = body("‹  BIOPHYSX", 15, true);
        back.setOnClickListener(v -> finish());
        page.addView(back, lp(1, 0));

        TextView title = title("Pulse Measurement", 29, true);
        title.setPadding(0, dp(14), 0, 0);
        page.addView(title, lp(1, 0));
        TextView sub = body("A guided optical measurement. Follow the simple prompts below.", 15, false);
        sub.setTextColor(0xFF5E7285);
        page.addView(sub, lp(1, 6));

        LinearLayout state = card(0xEFFFFFFF);
        status = body("●  Sensor not connected", 14, true);
        status.setTextColor(0xFF2C7BE5);
        state.addView(status, lp(1, 0));
        instruction = body("Connect the BIOPHYSX sensor. The app will guide you from finger placement to capture.", 14, false);
        instruction.setTextColor(0xFF5E7285);
        state.addView(instruction, lp(1, 8));
        phase = body("1  CONNECT SENSOR", 12, true);
        phase.setTextColor(0xFF6C8093);
        phase.setPadding(0, dp(10), 0, 0);
        state.addView(phase);
        page.addView(state, lp(1, 18));

        LinearLayout primary = new LinearLayout(this);
        primary.setGravity(Gravity.CENTER);
        primary.setPadding(dp(18), dp(20), dp(18), dp(20));
        primary.setBackground(round(0xF0EAF4FF, 0xFFD4E5F7, 28));
        page.addView(primary, lp(1, 14));

        pulse = title("--", 50, true);
        pulse.setGravity(Gravity.CENTER);
        primary.addView(pulse, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView bpmLabel = body("PULSE", 13, true);
        bpmLabel.setTextColor(0xFF5E7285);
        labels.addView(bpmLabel);
        TextView unit = body("BPM", 13, false);
        unit.setTextColor(0xFF5E7285);
        labels.addView(unit);
        primary.addView(labels, new LinearLayout.LayoutParams(dp(80), -2));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        signal = body("Signal: waiting", 14, true);
        signal.setTextColor(0xFF5E7285);
        statusRow.addView(signal, new LinearLayout.LayoutParams(0, -2, 1));
        timer = body("00:30", 17, true);
        statusRow.addView(timer);
        page.addView(statusRow, lp(1, 10));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgress(0);
        page.addView(progress, lp(1, 8));

        LinearLayout waveBox = card(0xEFFFFFFF);
        TextView w = body("LIVE OPTICAL SIGNAL", 13, true);
        w.setTextColor(0xFF6C8093);
        waveBox.addView(w, lp(1, 0));
        waveform = new WaveformView(this);
        waveform.setDemo(false);
        waveBox.addView(waveform, new LinearLayout.LayoutParams(-1, dp(190)));
        page.addView(waveBox, lp(1, 14));

        LinearLayout mini = new LinearLayout(this);
        mini.setOrientation(LinearLayout.HORIZONTAL);
        temp = metric("CONTACT TEMP", "-- °C");
        mini.addView(temp, new LinearLayout.LayoutParams(0, -2, 1));
        TextView physics = metric("PHYSICS", "See why");
        physics.setOnClickListener(v -> showPhysics());
        mini.addView(physics, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(mini, lp(1, 12));

        connect = button("Connect & Start Measurement");
        connect.setOnClickListener(v -> startSensor());
        page.addView(connect, lp(1, 14));

        TextView guide = body("The best measurement comes from a steady hand, gentle finger contact and a stable sensor position.", 12, false);
        guide.setTextColor(0xFF718497);
        guide.setGravity(Gravity.CENTER);
        guide.setPadding(dp(8), dp(12), dp(8), dp(2));
        page.addView(guide, lp(1, 0));

        TextView note = body("Measurements are experimental and not a medical diagnosis. Contact temperature is not core body temperature.", 12, false);
        note.setTextColor(0xFF718497);
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(8), dp(18), dp(8), dp(4));
        page.addView(note, lp(1, 0));

        TextView footer = body("Made with ❤️ by Aman Kumar Patel", 13, true);
        footer.setTextColor(0xFF5E7285);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, lp(1, 16));
    }

    private TextView metric(String label, String value) {
        TextView t = body(label + "\n" + value, 14, true);
        t.setTextColor(0xFF17324D);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(14), dp(8), dp(14));
        t.setBackground(round(0xEFFFFFFF, 0xFFDCE7F1, 18));
        return t;
    }

    private LinearLayout card(int fill) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.setBackground(round(fill, 0xFFDCE7F1, 22));
        return c;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(0xFF17324D);
        b.setMinHeight(dp(58));
        b.setBackground(round(0xEAF4FF, 0xFFBFD9F4, 19));
        return b;
    }

    private void startSensor() {
        if (running) return;
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQ_BT);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_BT);
            return;
        }
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter a = bm.getAdapter();
        if (a == null) { status.setText("●  Bluetooth is not supported"); return; }
        if (!a.isEnabled()) { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); return; }
        resetSession();
        ble.startScan();
        connect.setEnabled(false);
        connect.setText("Looking for BIOPHYSX sensor…");
        phase.setText("1  CONNECT SENSOR");
        instruction.setText("Power the sensor hub. Once connected, place one finger gently on the optical sensor.");
    }

    private void resetSession() {
        recentPpg.clear();
        bpmSamples.clear();
        signalEngine.clear();
        bestTemp = Double.NaN;
        lastQuality = new PpgSignalEngine.Result("Place finger", 0, 0, 0, 0);
        sessionStart = 0L;
        running = false;
        fingerPresent = false;
        connected = false;
        finalized = false;
        progress.setProgress(0);
        timer.setText("00:30");
        pulse.setText("--");
        temp.setText("CONTACT TEMP\n-- °C");
        signal.setText("Signal: waiting");
        signal.setTextColor(0xFF5E7285);
        waveform.clear();
    }

    private void beginSession() {
        if (running) return;
        running = true;
        sessionStart = System.currentTimeMillis();
        finalized = false;
        handler.post(progressTick);
    }

    private final Runnable progressTick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long elapsed = System.currentTimeMillis() - sessionStart;
            long left = Math.max(0, SESSION_MS - elapsed);
            int p = (int)Math.min(1000, (elapsed * 1000L) / SESSION_MS);
            progress.setProgress(p);
            timer.setText(String.format(Locale.US, "00:%02d", (left + 999) / 1000));

            if (!fingerPresent) {
                phase.setText("2  PLACE FINGER");
                instruction.setText("Place one finger gently on the optical sensor and keep your hand relaxed.");
            } else if (elapsed < STABILIZE_MS) {
                phase.setText("3  STABILIZING");
                instruction.setText("Good. Keep your finger still while the optical signal settles.");
            } else {
                phase.setText("4  CAPTURING");
                instruction.setText("Capture in progress. Stay relaxed and keep the sensor position steady.");
            }

            if (elapsed >= SESSION_MS) {
                finishSession();
            } else {
                handler.postDelayed(this, 120);
            }
        }
    };

    private void finishSession() {
        if (finalized) return;
        finalized = true;
        running = false;
        handler.removeCallbacks(progressTick);
        progress.setProgress(1000);
        timer.setText("DONE");
        connect.setEnabled(true);
        connect.setText("Measure Again");
        phase.setText("5  COMPLETE");

        double bpm = median(bpmSamples);
        String quality = lastQuality.label;
        boolean usable = signalEngine.size() >= 20 && lastQuality.score >= 0.40 && fingerPresent;
        if (!usable) quality = "No strong optical capture";

        long duration = SESSION_MS;
        String time = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(new Date());
        if (usable) {
            HistoryStore.add(this, time,
                    Double.isFinite(bpm) ? String.format(Locale.US, "%.0f", bpm) : "—",
                    Double.isFinite(bestTemp) ? String.format(Locale.US, "%.1f", bestTemp) : "—",
                    quality,
                    lastQuality.score,
                    signalEngine.size(),
                    duration);
        }

        instruction.setText(usable
                ? "Your session is complete. The summary below describes the captured signal; it does not assess health."
                : "The optical signal was not consistently strong enough for a clean summary. Try again with steadier finger contact.");

        Intent i = new Intent(this, MeasurementResultActivity.class);
        if (Double.isFinite(bpm)) i.putExtra("bpm", bpm);
        if (Double.isFinite(bestTemp)) i.putExtra("temp", bestTemp);
        i.putExtra("score", lastQuality.score);
        i.putExtra("samples", signalEngine.size());
        i.putExtra("duration_ms", duration);
        i.putExtra("quality", quality);
        startActivity(i);
    }

    private double median(List<Double> values) {
        if (values.isEmpty()) return Double.NaN;
        ArrayList<Double> copy = new ArrayList<>(values);
        Collections.sort(copy);
        int n = copy.size();
        if ((n & 1) == 1) return copy.get(n / 2);
        return (copy.get(n / 2 - 1) + copy.get(n / 2)) / 2.0;
    }

    @Override public void onStatus(String s) {
        runOnUiThread(() -> {
            status.setText("●  " + s);
            String low = s.toLowerCase(Locale.US);
            if (low.contains("sensor connected")) {
                connected = true;
                connect.setText("Sensor connected — place finger");
                phase.setText("2  PLACE FINGER");
                instruction.setText("Place one finger gently on the optical sensor. The measurement timer begins when the sensor stream is ready.");
            }
            if (low.contains("live sensor stream ready")) beginSession();
            if (low.contains("disconnected")) {
                connected = false;
                running = false;
                handler.removeCallbacks(progressTick);
                connect.setEnabled(true);
                connect.setText("Reconnect Sensor");
                phase.setText("1  CONNECT SENSOR");
            }
        });
    }

    @Override public void onFingerState(boolean present) {
        runOnUiThread(() -> {
            fingerPresent = present;
            if (!running) return;
            if (!present) {
                signal.setText("Signal: place finger on sensor");
                signal.setTextColor(0xFF8B5E3C);
            }
        });
    }

    @Override public void onData(Double bpm, Double temperature, Double rr, Double ppg) {
        runOnUiThread(() -> {
            if (bpm != null && bpm > 35 && bpm < 220 && fingerPresent) {
                bpmSamples.add(bpm);
                while (bpmSamples.size() > 24) bpmSamples.remove(0);
                pulse.setText(String.format(Locale.US, "%.0f", median(bpmSamples)));
            }
            if (temperature != null && temperature > -20 && temperature < 60) {
                bestTemp = temperature;
                temp.setText(String.format(Locale.US, "CONTACT TEMP\n%.1f °C", temperature));
            }
            if (ppg != null) {
                signalEngine.add(ppg);
                recentPpg.addLast(ppg);
                while (recentPpg.size() > 80) recentPpg.removeFirst();
                waveform.addSample(normalize(ppg));
                lastQuality = signalEngine.evaluate(fingerPresent);
                signal.setText("Signal: " + lastQuality.label);
                signal.setTextColor(lastQuality.score >= 0.48 ? 0xFF237B5B : 0xFF718497);
            }
        });
    }

    private float normalize(double value) {
        if (recentPpg.isEmpty()) return 0f;
        double mean = 0;
        for (double x : recentPpg) mean += x;
        mean /= recentPpg.size();
        double maxDev = 1;
        for (double x : recentPpg) maxDev = Math.max(maxDev, Math.abs(x - mean));
        return (float)Math.max(-1, Math.min(1, (value - mean) / (4.0 * maxDev)));
    }

    private void showPhysics() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("The Physics behind PPG")
                .setMessage("The optical sensor shines light into tissue and measures changes in returned light. Blood volume changes during each heartbeat, creating a repeating optical signal. BIOPHYSX processes that signal to estimate pulse rate.\n\nThe signal-quality score describes the captured optical waveform. It does not describe health status or diagnose disease.")
                .setPositiveButton("Got it", null).show();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(progressTick);
        if (ble != null) ble.stop();
        super.onDestroy();
    }

    private TextView title(String s, float size, boolean bold) { return body(s,size,bold); }
    private TextView body(String s, float size, boolean bold) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xFF17324D); t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL)); return t; }
    private LinearLayout.LayoutParams lp(int w,int top){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(top); return p; }
    private android.graphics.drawable.GradientDrawable round(int fill,int stroke,int r){ android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(r)); g.setStroke(dp(1),stroke); return g; }
    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }

    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==REQ_BT){ boolean ok=results.length>0; for(int r:results) ok &= r==PackageManager.PERMISSION_GRANTED; if(ok) startSensor(); else status.setText("●  Bluetooth permission was declined"); }
    }
}
