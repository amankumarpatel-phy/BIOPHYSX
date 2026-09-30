package in.aman.biophysx;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

public final class HistoryStore {
    private static final String PREF = "biophysx_history";
    private static final String KEY = "sessions";
    private HistoryStore() {}

    public static void add(Context context, String time, String bpm, String temp, String quality) {
        add(context, time, bpm, temp, quality, 0.0, 0, 0L);
    }

    public static void add(Context context, String time, String bpm, String temp, String quality,
                           double signalScore, int samples, long durationMs) {
        try {
            android.content.SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray old = new JSONArray(p.getString(KEY, "[]"));
            JSONArray out = new JSONArray();
            JSONObject n = new JSONObject();
            n.put("time", time);
            n.put("bpm", bpm);
            n.put("temp", temp);
            n.put("quality", quality);
            n.put("signalScore", signalScore);
            n.put("samples", samples);
            n.put("durationMs", durationMs);
            out.put(n);
            for (int i = 0; i < Math.min(old.length(), 19); i++) out.put(old.getJSONObject(i));
            p.edit().putString(KEY, out.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static JSONArray get(Context context) {
        try {
            return new JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]"));
        } catch (Exception e) { return new JSONArray(); }
    }
}
