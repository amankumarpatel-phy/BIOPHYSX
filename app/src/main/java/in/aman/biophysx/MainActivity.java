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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity implements BleManager.Listener {
    private final int REQ_BT=42;
    private BleManager ble;
    private TextView statusText, pulseValue, tempValue, rrValue, forceValue, modeHint;
    private WaveformView waveform;
    private boolean science=false;
    private double ppgBaseline=0.0;
    private double ppgScale=1.0;
    private boolean ppgInitialized=false;

    @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(0xFFF7FBFF); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); build(); }

    private void build(){
        ble=new BleManager(this,this);
        FrameLayout root=new FrameLayout(this); root.setBackgroundColor(0xFFF7FBFF);
        root.addView(new FloatingBackgroundView(this),new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setPadding(dp(18),dp(18),dp(18),dp(20));
        LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(page,new ScrollView.LayoutParams(-1,-2)); root.addView(scroll,new FrameLayout.LayoutParams(-1,-1)); setContentView(root);

        TextView brand=title("BIOPHYSX",30,true); page.addView(brand,lp(1,0));
        TextView tagline=body("Your Body. Your Signals. Your Physics.",15,false); tagline.setTextColor(0xFF5E7285); page.addView(tagline,lp(1,6));

        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(14),dp(10),dp(14),dp(10)); top.setBackground(round(0xEFFFFFFF,0xFFDCE7F1,18));
        statusText=body("●  Demo mode — sensor not connected",14,true); statusText.setTextColor(0xFF2C7BE5); top.addView(statusText,new LinearLayout.LayoutParams(0,-2,1));
        Switch sw=new Switch(this); sw.setText("Science"); sw.setTextSize(13); sw.setChecked(false); sw.setOnCheckedChangeListener((button,checked)->{science=checked; modeHint.setText(checked?"Advanced view: signal processing and Physics details are visible.":"Simple view: the essentials are front and centre.");});
        top.addView(sw); page.addView(top,lp(1,16));

        modeHint=body("Simple view: the essentials are front and centre.",13,false); modeHint.setTextColor(0xFF5E7285); modeHint.setPadding(dp(4),dp(10),dp(4),0); page.addView(modeHint,lp(1,0));

        page.addView(section("LIVE BODY DASHBOARD"),lp(1,20));
        LinearLayout cards=new LinearLayout(this); cards.setOrientation(LinearLayout.VERTICAL); page.addView(cards,lp(1,8));
        pulseValue=metricCard(cards,"❤️","PULSE","-- BPM","Detected from tiny optical changes related to blood flow.",0xFFEAF4FF);
        tempValue=metricCard(cards,"🌡","TEMPERATURE","-- °C","Live sensor reading. Not a clinical diagnosis.",0xFFFFF1EA);
        rrValue=metricCard(cards,"🫁","BREATHING","-- / min","Respiratory rate can be estimated from a suitable sensor.",0xFFE8FBF3);
        forceValue=metricCard(cards,"✋","FORCE","Demo","Optional load-cell / grip module.",0xFFF1EDFF);

        page.addView(section("LIVE SIGNAL"),lp(1,20));
        LinearLayout waveBox=new LinearLayout(this); waveBox.setOrientation(LinearLayout.VERTICAL); waveBox.setPadding(dp(14),dp(14),dp(14),dp(10)); waveBox.setBackground(round(0xEFFFFFFF,0xFFDCE7F1,22)); page.addView(waveBox,lp(1,8));
        TextView wLabel=body("PPG / physiological waveform",16,true); waveBox.addView(wLabel,lp(1,0));
        waveform=new WaveformView(this); waveBox.addView(waveform,new LinearLayout.LayoutParams(-1,dp(190)));
        TextView demo=body("Demo waveform is shown until a BIOPHYSX sensor is connected.",12,false); demo.setTextColor(0xFF5E7285); waveBox.addView(demo,lp(1,0));

        page.addView(section("EXPLORE"),lp(1,20));
        HorizontalScrollView hs=new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false); LinearLayout chips=new LinearLayout(this); chips.setPadding(0,0,0,4); String[] names={"See the Physics","Start Measurement","Sensor Setup","Body Lab","Sensor Guide","History"}; for(String n:names){ Button x=softButton(n); chips.addView(x,new LinearLayout.LayoutParams(dp(180),dp(60))); LinearLayout.LayoutParams m=(LinearLayout.LayoutParams)x.getLayoutParams(); m.setMargins(0,0,dp(10),0); x.setLayoutParams(m); if(n.startsWith("See")) x.setOnClickListener(v->showPhysics()); if(n.startsWith("Start")) x.setOnClickListener(v->openMeasurement()); if(n.startsWith("Sensor Setup")) x.setOnClickListener(v->openSensorSetup()); if(n.startsWith("Body")) x.setOnClickListener(v->openBodyLab()); if(n.equals("Sensor Guide")) x.setOnClickListener(v->showSensorGuide()); if(n.startsWith("History")) x.setOnClickListener(v->openHistory()); }
        hs.addView(chips); page.addView(hs,lp(1,8));

        TextView scienceNote=body("BIOPHYSX provides educational and experimental measurements. It is not a medical diagnosis or emergency-care tool.",12,false); scienceNote.setTextColor(0xFF718497); scienceNote.setGravity(Gravity.CENTER); scienceNote.setPadding(dp(8),dp(22),dp(8),dp(8)); page.addView(scienceNote,lp(1,0));
        TextView footer=body("Made with ❤️ by Aman Kumar Patel",13,true); footer.setTextColor(0xFF5E7285); footer.setGravity(Gravity.CENTER); page.addView(footer,lp(1,18));
    }


    private void openMeasurement(){ startActivity(new android.content.Intent(this, MeasurementActivity.class)); }
    private void openHistory(){ startActivity(new android.content.Intent(this, HistoryActivity.class)); }
    private void openBodyLab(){ startActivity(new android.content.Intent(this, BodyLabActivity.class)); }
    private void openSensorSetup(){ startActivity(new android.content.Intent(this, SensorSetupActivity.class)); }
    private void showPhysics(){
        android.app.AlertDialog d=new android.app.AlertDialog.Builder(this).setTitle("The Physics behind PPG").setMessage("Light interacts with tissue. As blood volume changes with each heartbeat, the detected optical signal changes slightly. BIOPHYSX can process that repeating signal to estimate pulse rate.\n\nScience View exposes filtering, sampling and frequency analysis for learners and researchers.").setPositiveButton("Got it",null).create(); d.show();
    }
    private void showSensorGuide(){
        new android.app.AlertDialog.Builder(this).setTitle("BIOPHYSX Sensor Guide").setMessage("1. Turn Bluetooth on.\n2. Power the BIOPHYSX sensor hub.\n3. Place the PPG sensor on a finger.\n4. Tap Start Measurement.\n5. Keep still while the live signal stabilizes.\n\nThe first hardware prototype can use an ESP32 + optical pulse sensor + temperature sensor + IMU.").setPositiveButton("Close",null).show();
    }
    private void startSensor(){
        if(Build.VERSION.SDK_INT>=31){
            if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){ requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},REQ_BT); return; }
        } else if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){ requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQ_BT); return; }
        BluetoothManager bm=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE); BluetoothAdapter a=bm.getAdapter();
        if(a==null){statusText.setText("●  Bluetooth is not supported on this phone"); return;}
        if(!a.isEnabled()){startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); return;}
        waveform.setDemo(false); ble.startScan();
    }
    @Override public void onStatus(String s){ runOnUiThread(()->statusText.setText("●  "+s)); }
    @Override public void onData(Double bpm,Double temp,Double rr,Double ppg){
        runOnUiThread(()->{
            if(bpm!=null) pulseValue.setText(String.format(Locale.US,"%.0f BPM",bpm));
            if(temp!=null) tempValue.setText(String.format(Locale.US,"%.1f °C",temp));
            if(rr!=null && rr>0) rrValue.setText(String.format(Locale.US,"%.0f / min",rr));
            if(ppg!=null){
                double sample=ppg;
                if(!ppgInitialized){ ppgBaseline=sample; ppgScale=1.0; ppgInitialized=true; }
                ppgBaseline = 0.985*ppgBaseline + 0.015*sample;
                double ac = sample - ppgBaseline;
                ppgScale = 0.995*ppgScale + 0.005*Math.max(1.0, Math.abs(ac));
                float normalized=(float)Math.max(-1.0, Math.min(1.0, ac/(6.0*ppgScale)));
                waveform.addSample(normalized);
            }
        });
    }
    @Override protected void onDestroy(){ if(ble!=null) ble.stop(); super.onDestroy(); }

    private TextView metricCard(LinearLayout parent,String icon,String label,String value,String sub,int fill){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16),dp(14),dp(16),dp(14)); c.setBackground(round(fill,0xFFDCE7F1,22)); parent.addView(c,lp(1,8));
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); c.addView(row,lp(1,0));
        TextView i=title(icon,22,false); i.setGravity(Gravity.CENTER); row.addView(i,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout labels=new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL); TextView l=body(label,13,true); TextView v=body(value,27,true); v.setTextColor(0xFF17324D); labels.addView(l); labels.addView(v); row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        TextView s=body(sub,13,false); s.setTextColor(0xFF5E7285); s.setPadding(dp(42),dp(5),0,0); c.addView(s,lp(1,0));
        return v;
    }
    private TextView section(String s){ TextView t=body(s,12,true); t.setTextColor(0xFF6C8093); t.setLetterSpacing(0.12f); return t; }
    private Button softButton(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(13); b.setAllCaps(false); b.setTextColor(0xFF17324D); b.setGravity(Gravity.CENTER); b.setBackground(round(0xEFFFFFFF,0xFFDCE7F1,18)); b.setMinHeight(dp(56)); return b; }
    private TextView title(String s,float size,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xFF17324D); t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL)); return t; }
    private TextView body(String s,float size,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xFF17324D); t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL)); return t; }
    private LinearLayout.LayoutParams lp(int w,int top){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w==1? -1 : dp(w),-2); p.topMargin=dp(top); return p; }
    private android.graphics.drawable.GradientDrawable round(int fill,int stroke,int r){ android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(r)); g.setStroke(dp(1),stroke); return g; }
    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){ super.onRequestPermissionsResult(request,permissions,results); if(request==REQ_BT){ boolean ok=true; for(int r:results) ok &= r==PackageManager.PERMISSION_GRANTED; if(ok) startSensor(); else statusText.setText("●  Bluetooth permission was declined"); } }
}
