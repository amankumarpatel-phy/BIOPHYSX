package in.aman.biophysx;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

/**
 * Live multi-signal Body Lab: BLE PPG/temperature + phone accelerometer motion.
 * Measurements are experimental and should not be presented as medical diagnosis.
 */
public class BodyLabActivity extends Activity implements BleManager.Listener, SensorEventListener {
    private static final int REQ_BT = 61;

    private final Handler handler = new Handler();
    private BleManager ble;
    private SensorManager sensorManager;
    private Sensor accel;
    private BodyLabVisualizerView visualizer;
    private WaveformView waveform;
    private TextView status, quality, scienceHint;
    private LinearLayout pulse, temp, breath, motion;
    private boolean science = false;
    private double lastBpm = Double.NaN;
    private double lastTemp = Double.NaN;
    private double lastRr = Double.NaN;
    private double motionEma = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFFF7FBFF);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ble = new BleManager(this, this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        build();
    }

    private void build() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFFF7FBFF);
        root.addView(new FloatingBackgroundView(this),
                new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(18), dp(16), dp(18), dp(20));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        TextView title = title("BODY LAB", 28, true);
        page.addView(title, lp(0));

        TextView sub = body("Live signals from your connected body sensors", 14, false);
        sub.setTextColor(0xFF5E7285);
        page.addView(sub, lp(5));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(8), dp(10), dp(8));
        bar.setBackground(round(0xEFFFFFFF, 0xFFDCE7F1, 18));

        status = body("●  Sensor not connected", 14, true);
        status.setTextColor(0xFF2C7BE5);
        bar.addView(status, new LinearLayout.LayoutParams(0, -2, 1));

        Switch sw = new Switch(this);
        sw.setText("Science");
        sw.setTextSize(13);
        sw.setOnCheckedChangeListener((button, checked) -> {
            science = checked;
            scienceHint.setText(checked
                    ? "Advanced view: sampling, signal quality and motion physics are visible."
                    : "Simple view: only the essentials are shown.");
        });
        bar.addView(sw);
        page.addView(bar, lp(14));

        scienceHint = body("Simple view: only the essentials are shown.", 12, false);
        scienceHint.setTextColor(0xFF718497);
        page.addView(scienceHint, lp(7));

        visualizer = new BodyLabVisualizerView(this);
        page.addView(visualizer, new LinearLayout.LayoutParams(-1, dp(300)));

        page.addView(section("LIVE BODY SIGNALS"), lp(4));

        pulse = metric("❤️", "PULSE", "-- BPM", 0xFFEAF4FF);
        temp = metric("🌡", "CONTACT TEMPERATURE", "-- °C", 0xFFFFF1EA);
        breath = metric("🫁", "BREATHING", "-- / min", 0xFFE8FBF3);
        motion = metric("🌀", "MOTION", "Still", 0xFFF1EDFF);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.VERTICAL);
        metrics.addView(pulse, lp(7));
        metrics.addView(temp, lp(7));
        metrics.addView(breath, lp(7));
        metrics.addView(motion, lp(7));
        page.addView(metrics, lp(8));

        quality = body("Signal quality: waiting for optical data", 13, true);
        quality.setTextColor(0xFF718497);
        quality.setPadding(dp(12), dp(12), dp(12), dp(12));
        quality.setBackground(round(0xEFFFFFFF, 0xFFDCE7F1, 18));
        page.addView(quality, lp(12));

        LinearLayout waveBox = new LinearLayout(this);
        waveBox.setOrientation(LinearLayout.VERTICAL);
        waveBox.setPadding(dp(14), dp(14), dp(14), dp(10));
        waveBox.setBackground(round(0xEFFFFFFF, 0xFFDCE7F1, 22));

        TextView waveTitle = body("LIVE OPTICAL SIGNAL", 15, true);
        waveBox.addView(waveTitle, lp(0));

        waveform = new WaveformView(this);
        waveform.setDemo(false);
        waveBox.addView(waveform, new LinearLayout.LayoutParams(-1, dp(170)));
        page.addView(waveBox, lp(12));

        Button connect = button("Connect BIOPHYSX Sensor");
        connect.setOnClickListener(v -> connectSensor());
        page.addView(connect, lp(14));

        Button physics = button("See the Physics");
        physics.setOnClickListener(v -> showPhysics());
        page.addView(physics, lp(8));

        TextView note = body(
                "BIOPHYSX is an educational and experimental sensing platform. Contact temperature is not core body temperature, and the app is not a medical diagnosis tool.",
                12, false);
        note.setTextColor(0xFF718497);
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(8), dp(18), dp(8), dp(3));
        page.addView(note, lp(0));

        TextView footer = body("Made with ❤️ by Aman Kumar Patel", 13, true);
        footer.setTextColor(0xFF5E7285);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, lp(16));
    }

    private LinearLayout metric(String icon, String label, String value, int fill) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(fill, 0xFFDCE7F1, 20));

        TextView ic = title(icon, 20, false);
        ic.setGravity(Gravity.CENTER);
        card.addView(ic, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView lab = body(label, 12, true);
        lab.setTextColor(0xFF5E7285);

        TextView val = body(value, 24, true);
        val.setTextColor(0xFF17324D);

        texts.addView(lab, new LinearLayout.LayoutParams(-1, -2));
        texts.addView(val, new LinearLayout.LayoutParams(-1, -2));

        card.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        card.setTag(val);
        return card;
    }

    private TextView valueView(LinearLayout card) {
        return (TextView) card.getTag();
    }

    private void connectSensor() {
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                }, REQ_BT);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, REQ_BT);
            return;
        }

        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bm.getAdapter();

        if (adapter == null) {
            status.setText("●  Bluetooth is not supported");
            return;
        }

        if (!adapter.isEnabled()) {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            return;
        }

        waveform.setDemo(false);
        ble.startScan();
    }

    @Override
    public void onStatus(String s) {
        runOnUiThread(() -> status.setText("●  " + s));
    }

    @Override
    public void onData(Double bpm, Double temperature, Double rr, Double ppg) {
        runOnUiThread(() -> {
            if (bpm != null && bpm > 0) {
                lastBpm = bpm;
                valueView(pulse).setText(String.format(Locale.US, "%.0f BPM", bpm));
                visualizer.setPulse(bpm);
            }

            if (temperature != null && temperature > -20 && temperature < 60) {
                lastTemp = temperature;
                valueView(temp).setText(String.format(Locale.US, "%.1f °C", temperature));
            }

            if (rr != null && rr > 0) {
                lastRr = rr;
                valueView(breath).setText(String.format(Locale.US, "%.0f / min", rr));
            }

            if (ppg != null) {
                waveform.addSample(normalizePpg(ppg));
                quality.setText("Signal quality: optical data received");
                quality.setTextColor(0xFF237B5B);
            }
        });
    }

    private double ppgMean = 0.0;
    private double ppgDev = 1.0;

    private float normalizePpg(double sample) {
        ppgMean = ppgMean == 0.0
                ? sample
                : 0.985 * ppgMean + 0.015 * sample;

        double ac = sample - ppgMean;
        ppgDev = 0.995 * ppgDev + 0.005 * Math.max(1.0, Math.abs(ac));

        return (float) Math.max(-1.0, Math.min(1.0, ac / (6.0 * ppgDev)));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        double magnitude = Math.sqrt(
                event.values[0] * event.values[0]
                        + event.values[1] * event.values[1]
                        + event.values[2] * event.values[2]);

        double dynamic = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);
        motionEma = 0.85 * motionEma + 0.15 * dynamic;

        float intensity = (float) Math.min(1.0, motionEma / 4.0);
        visualizer.setMotion(intensity);

        if (intensity < 0.08f) {
            valueView(motion).setText("Still");
        } else if (intensity < 0.25f) {
            valueView(motion).setText("Gentle");
        } else {
            valueView(motion).setText("Active");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No special handling required.
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams lp(int topMarginDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(topMarginDp);
        return p;
    }

    private TextView title(String text, float size, boolean bold) {
        TextView t = body(text, size, bold);
        t.setTextColor(0xFF17324D);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private TextView body(String text, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTypeface(android.graphics.Typeface.create(
                "sans",
                bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        return t;
    }

    private TextView section(String text) {
        TextView t = body(text, 12, true);
        t.setTextColor(0xFF5E7285);
        return t;
    }

    private android.graphics.drawable.GradientDrawable round(int fill, int strokeColor, int radiusDp) {
        android.graphics.drawable.GradientDrawable g =
                new android.graphics.drawable.GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), strokeColor);
        return g;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(0xFF17324D);
        b.setMinHeight(dp(58));
        b.setBackground(round(0xFFEAF4FF, 0xFFBFD9F4, 19));
        return b;
    }

    private void showPhysics() {
        String msg =
                "PULSE / PPG\n"
                        + "Light interacts with tissue. Blood-volume changes alter the detected optical signal. "
                        + "BIOPHYSX processes the repeating signal to estimate pulse rate.\n\n"
                        + "MOTION\n"
                        + "The phone accelerometer measures acceleration along three axes. "
                        + "BIOPHYSX compares the measured magnitude with gravity to show a simple movement level.\n\n"
                        + "SCIENCE VIEW\n"
                        + "The advanced screen exposes the signal-processing layer without forcing technical detail on everyday users.";

        new android.app.AlertDialog.Builder(this)
                .setTitle("The Physics behind Body Lab")
                .setMessage(msg)
                .setPositiveButton("Got it", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (ble != null) {
            ble.stop();
        }
        super.onDestroy();
    }
}
