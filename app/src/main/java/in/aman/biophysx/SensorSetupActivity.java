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

/** Guided, age-friendly BLE sensor onboarding and optical signal check. */
public class SensorSetupActivity extends Activity implements BleManager.Listener {
    private static final int REQ_BT = 72;
    private BleManager ble;
    private TextView connectionValue, fingerValue, signalValue, scienceText, readyCard;
    private Button scanButton, continueButton;
    private boolean science = false, connected = false, finger = false;
    private final PpgSignalEngine engine = new PpgSignalEngine(60);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
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
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(18), dp(18), dp(18), dp(26));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        TextView title = text("SENSOR SETUP", 28, true);
        page.addView(title, lp(1, 0));
        TextView sub = text("A gentle guide from connection to a clean optical signal", 14, false);
        sub.setTextColor(0xFF5E7285); page.addView(sub, lp(1, 5));

        LinearLayout mode = rowCard();
        connectionValue = text("●  Not connected", 14, true); connectionValue.setTextColor(0xFF2C7BE5);
        mode.addView(connectionValue, new LinearLayout.LayoutParams(0, -2, 1));
        Switch sw = new Switch(this); sw.setText("Science"); sw.setTextSize(13);
        sw.setOnCheckedChangeListener((b, checked) -> { science = checked; scienceText.setText(checked ?
                "Science Mode: BLE state, optical-signal metrics and packet-level details are shown here."
                : "Simple Mode: follow the large steps and let BIOPHYSX guide you."); });
        mode.addView(sw); page.addView(mode, lp(1, 14));

        scienceText = text("Simple Mode: follow the large steps and let BIOPHYSX guide you.", 12, false);
        scienceText.setTextColor(0xFF718497); page.addView(scienceText, lp(1, 7));

        page.addView(section("THREE THINGS TO DO"), lp(1, 18));
        page.addView(stepCard("1", "Turn on Bluetooth", "Your phone needs Bluetooth to talk to the BIOPHYSX sensor hub."), lp(1, 8));
        page.addView(stepCard("2", "Power the sensor", "Use the ESP32 sensor hub with the MAX30102 optical module."), lp(1, 8));
        page.addView(stepCard("3", "Place one finger gently", "Keep your finger still on the optical sensor while the signal settles."), lp(1, 8));

        page.addView(section("LIVE CHECK"), lp(1, 20));
        fingerValue = statusCard("FINGER CONTACT", "Waiting");
        signalValue = statusCard("OPTICAL SIGNAL", "Waiting");
        page.addView(fingerValue, lp(1, 8)); page.addView(signalValue, lp(1, 8));

        scanButton = button("Find BIOPHYSX Sensor"); scanButton.setOnClickListener(v -> startScan());
        page.addView(scanButton, lp(1, 15));
        continueButton = button("Continue to Measurement"); continueButton.setEnabled(false); continueButton.setAlpha(0.55f);
        continueButton.setOnClickListener(v -> startActivity(new Intent(this, MeasurementActivity.class)));
        page.addView(continueButton, lp(1, 8));

        readyCard = text("✓  READY FOR MEASUREMENT\nConnect the sensor and place a finger gently to unlock the next step.", 14, true);
        readyCard.setTextColor(0xFF17324D); readyCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        readyCard.setBackground(round(0xEFFFFFFF, 0xFFDCE7F1, 20)); page.addView(readyCard, lp(1, 16));

        TextView note = text("Experimental/educational measurements only. Contact temperature is not core body temperature, and BIOPHYSX is not a diagnostic or emergency-care device.", 12, false);
        note.setTextColor(0xFF718497); note.setGravity(Gravity.CENTER); note.setPadding(dp(8), dp(18), dp(8), dp(4)); page.addView(note, lp(1, 0));
        TextView footer = text("Made with ❤️ by Aman Kumar Patel", 13, true); footer.setTextColor(0xFF5E7285); footer.setGravity(Gravity.CENTER); page.addView(footer, lp(1, 16));
    }

    private LinearLayout stepCard(String number, String heading, String description) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.HORIZONTAL); card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13)); card.setBackground(round(0xEFFFFFFF, 0xFFDCE7F1, 20));
        TextView n = text(number, 22, true); n.setGravity(Gravity.CENTER); n.setTextColor(0xFF2C7BE5); card.addView(n, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        TextView h = text(heading, 15, true), d = text(description, 12, false); d.setTextColor(0xFF5E7285);
        body.addView(h); body.addView(d, lp(1, 3)); card.addView(body, new LinearLayout.LayoutParams(0, -2, 1)); return card;
    }

    private TextView statusCard(String label, String value) {
        TextView t = text(label + "\n" + value, 14, true); t.setTextColor(0xFF17324D);
        t.setPadding(dp(16), dp(14), dp(16), dp(14)); t.setBackground(round(0xEFFFFFFF, 0xFFDCE7F1, 20)); return t;
    }
    private LinearLayout rowCard() { LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(14),dp(8),dp(10),dp(8)); r.setBackground(round(0xEFFFFFFF,0xFFDCE7F1,18)); return r; }

    private void startScan() {
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQ_BT); return;
            }
        } else if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_BT); return;
        }
        BluetoothManager bm = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE); BluetoothAdapter adapter = bm.getAdapter();
        if (adapter == null) { updateConnection("●  Bluetooth not supported", false); return; }
        if (!adapter.isEnabled()) { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); return; }
        scanButton.setEnabled(false); scanButton.setText("Looking for sensor…");
        updateConnection("●  Searching for BIOPHYSX sensor…", false); ble.startScan();
    }

    private void updateConnection(String s, boolean ok) { connectionValue.setText(s); connectionValue.setTextColor(ok ? 0xFF237B5B : 0xFF2C7BE5); }

    @Override public void onStatus(String s) {
        runOnUiThread(() -> {
            String lower = s.toLowerCase(Locale.US); boolean ok = lower.contains("connected") || lower.contains("stream ready");
            connected = connected || ok; updateConnection("●  " + s, ok || connected);
            if (lower.contains("disconnected") || lower.contains("failed") || lower.contains("not found")) { connected = false; scanButton.setEnabled(true); scanButton.setText("Find BIOPHYSX Sensor"); }
            if (lower.contains("live sensor stream ready")) { scanButton.setEnabled(false); scanButton.setText("Sensor connected"); }
            refreshReadyState();
        });
    }

    @Override public void onFingerState(boolean present) {
        runOnUiThread(() -> { finger = present; fingerValue.setText("FINGER CONTACT\n" + (present ? "Detected — keep still" : "Place finger on sensor")); fingerValue.setTextColor(present ? 0xFF237B5B : 0xFF8B5E3C); refreshReadyState(); });
    }

    @Override public void onData(Double bpm, Double temperature, Double rr, Double ppg) {
        if (ppg == null) return;
        runOnUiThread(() -> {
            engine.add(ppg); PpgSignalEngine.Result r = engine.evaluate(finger);
            String text = science ? String.format(Locale.US, "OPTICAL SIGNAL\n%s  •  score %.0f%%\nΔ range %.0f", r.label, r.score*100.0, r.amplitude) : "OPTICAL SIGNAL\n" + r.label;
            signalValue.setText(text);
            signalValue.setTextColor(r.score >= 0.48 ? 0xFF237B5B : (r.score >= 0.22 ? 0xFF8B6B3A : 0xFF718497));
            refreshReadyState();
        });
    }

    private void refreshReadyState() {
        boolean ready = connected && finger; continueButton.setEnabled(ready); continueButton.setAlpha(ready ? 1f : 0.55f);
        readyCard.setText(ready ? "✓  READY\nSensor connected and optical contact detected. Continue to the guided measurement."
                : "✓  READY FOR MEASUREMENT\nConnect the sensor and place a finger gently to unlock the next step.");
        readyCard.setTextColor(ready ? 0xFF237B5B : 0xFF17324D);
    }

    @Override protected void onDestroy() { if (ble != null) ble.stop(); super.onDestroy(); }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) { super.onRequestPermissionsResult(request,permissions,results); if(request==REQ_BT){ boolean ok=results.length>0; for(int r:results) ok &= r==PackageManager.PERMISSION_GRANTED; if(ok) startScan(); else updateConnection("●  Bluetooth permission declined",false); } }

    private TextView text(String s,float size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(0xFF17324D);t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private TextView section(String s){TextView t=text(s,12,true);t.setTextColor(0xFF5E7285);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(0xFF17324D);b.setMinHeight(dp(58));b.setBackground(round(0xEAF4FF,0xFFBFD9F4,19));return b;}
    private LinearLayout.LayoutParams lp(int w,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);return p;}
    private android.graphics.drawable.GradientDrawable round(int fill,int stroke,int radius){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
