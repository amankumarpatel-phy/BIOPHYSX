package in.aman.biophysx;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BleManager {
    public static final UUID SERVICE_UUID = UUID.fromString("6f6e1200-0c7a-4a66-92e8-0b6e8f6a7001");
    public static final UUID DATA_UUID = UUID.fromString("6f6e1201-0c7a-4a66-92e8-0b6e8f6a7001");
    private final Context context;
    private final Listener listener;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private String buffer="";

    public interface Listener {
        void onStatus(String s);
        void onData(Double bpm, Double temp, Double rr, Double ppg);
        default void onFingerState(boolean present) { }
    }
    public BleManager(Context c, Listener l){ context=c; listener=l; }

    public void startScan(){
        BluetoothManager bm=(BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter=bm.getAdapter();
        if(adapter==null || !adapter.isEnabled()){ listener.onStatus("Bluetooth is off"); return; }
        if(Build.VERSION.SDK_INT>=31 && context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED){ listener.onStatus("Bluetooth permission needed"); return; }
        scanner=adapter.getBluetoothLeScanner();
        listener.onStatus("Looking for BIOPHYSX sensor…");
        ScanFilter filter=new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE_UUID)).build();
        ScanSettings settings=new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanner.startScan(java.util.Collections.singletonList(filter), settings, scanCallback);
    }

    public void stop(){
        try { if(scanner!=null) scanner.stopScan(scanCallback); } catch(Exception ignored) {}
        try { if(gatt!=null) gatt.close(); } catch(Exception ignored) {}
        gatt=null;
    }

    private final ScanCallback scanCallback=new ScanCallback(){
        @Override public void onScanResult(int callbackType, ScanResult result){
            try { scanner.stopScan(this); } catch(Exception ignored) {}
            BluetoothDevice d=result.getDevice();
            listener.onStatus("Connecting to "+(d.getName()==null?"BIOPHYSX sensor":d.getName())+"…");
            gatt=d.connectGatt(context,false,gattCallback);
        }
        @Override public void onScanFailed(int errorCode){ listener.onStatus("Scan failed: "+errorCode); }
    };

    private final BluetoothGattCallback gattCallback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int status,int newState){
            if(newState==android.bluetooth.BluetoothProfile.STATE_CONNECTED){
                listener.onStatus("Sensor connected"); g.discoverServices();
            } else if(newState==android.bluetooth.BluetoothProfile.STATE_DISCONNECTED){ listener.onStatus("Sensor disconnected"); }
        }
        @Override public void onServicesDiscovered(BluetoothGatt g,int status){
            BluetoothGattService s=g.getService(SERVICE_UUID);
            if(s==null){ listener.onStatus("BIOPHYSX service not found"); return; }
            BluetoothGattCharacteristic c=s.getCharacteristic(DATA_UUID);
            if(c==null){ listener.onStatus("Sensor data channel not found"); return; }
            g.setCharacteristicNotification(c,true);
            BluetoothGattDescriptor d=c.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if(d!=null){ d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); g.writeDescriptor(d); }
            listener.onStatus("Live sensor stream ready");
        }
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){ handle(c.getValue()); }
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value){ handle(value); }
    };

    private void handle(byte[] bytes){
        buffer += new String(bytes, StandardCharsets.UTF_8);
        String[] lines=buffer.split("\\n");
        if(!buffer.endsWith("\n")) buffer=lines.length>0?lines[lines.length-1]:buffer; else buffer="";
        for(int i=0;i<lines.length-(buffer.isEmpty()?0:1);i++) parse(lines[i].trim());
    }
    private void parse(String line){
        try{
            Double bpm=null,temp=null,rr=null,ppg=null;
            Boolean fingerPresent=null;
            String[] parts=line.split(",");
            if(parts.length==4 && !parts[0].contains("=")){
                ppg=Double.parseDouble(parts[0]); bpm=Double.parseDouble(parts[1]); temp=Double.parseDouble(parts[2]); rr=Double.parseDouble(parts[3]);
            } else {
                for(String part:parts){
                    String[] kv=part.split("=",2);
                    if(kv.length!=2) continue;
                    if(kv[0].equalsIgnoreCase("PPG")||kv[0].equalsIgnoreCase("IR")) ppg=Double.parseDouble(kv[1]);
                    if(kv[0].equalsIgnoreCase("PULSE")||kv[0].equalsIgnoreCase("BPM")) bpm=Double.parseDouble(kv[1]);
                    if(kv[0].equalsIgnoreCase("TEMP")) temp=Double.parseDouble(kv[1]);
                    if(kv[0].equalsIgnoreCase("RR")) rr=Double.parseDouble(kv[1]);
                    if(kv[0].equalsIgnoreCase("FINGER")) fingerPresent = Double.parseDouble(kv[1]) > 0.5;
                }
            }
            if (fingerPresent != null) listener.onFingerState(fingerPresent);
            listener.onData(bpm,temp,rr,ppg);
        } catch(Exception ignored) {}
    }
}
