package in.aman.biophysx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Soft abstract body/sensor visualization for the Body Lab screen. */
public class BodyLabVisualizerView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase = 0f;
    private float pulse = 0f;
    private float motion = 0f;

    public BodyLabVisualizerView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3f);
        stroke.setColor(0x552C7BE5);
    }

    public void setPulse(double bpm) {
        if (Double.isFinite(bpm) && bpm > 0) pulse = Math.min(1f, Math.max(0f, (float)((bpm - 40.0) / 160.0)));
        else pulse = 0.25f;
        invalidate();
    }

    public void setMotion(float intensity) {
        motion = Math.min(1f, Math.max(0f, intensity));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        canvas.drawColor(0xFFFDFEFF);
        phase += 0.018f;

        // Floating halos.
        halo(canvas, w * 0.17f, h * 0.20f, 55, 0x447FD8FF);
        halo(canvas, w * 0.84f, h * 0.33f, 70, 0x44C7A6FF);
        halo(canvas, w * 0.72f, h * 0.80f, 58, 0x44A9E6D0);

        float cx = w * 0.50f;
        float headY = h * 0.20f;
        float shoulderY = h * 0.36f;
        float hipY = h * 0.63f;

        // Minimal human silhouette, intentionally abstract.
        stroke.setColor(0x442D5F7F);
        stroke.setStrokeWidth(5f);
        canvas.drawCircle(cx, headY, 24f, stroke);
        canvas.drawLine(cx, headY + 24, cx, hipY, stroke);
        canvas.drawLine(cx - 66, shoulderY, cx + 66, shoulderY, stroke);
        canvas.drawLine(cx - 66, shoulderY, cx - 85, h * 0.54f, stroke);
        canvas.drawLine(cx + 66, shoulderY, cx + 85, h * 0.54f, stroke);
        canvas.drawLine(cx, hipY, cx - 48, h * 0.90f, stroke);
        canvas.drawLine(cx, hipY, cx + 48, h * 0.90f, stroke);

        float wave = (float)(1.0 + 0.20 * Math.sin(phase * 8.0 + pulse * 7.0));
        node(canvas, cx, h * 0.43f, 16f * wave, 0xFF2C7BE5);      // heart / PPG
        node(canvas, cx - 58, h * 0.52f, 11f + motion * 8f, 0xFF47A98C); // motion
        node(canvas, cx + 58, h * 0.52f, 10f, 0xFFE58D62);       // temp
        node(canvas, cx, h * 0.78f, 9f, 0xFF8E79D8);             // lower sensor hub

        orbit(canvas, cx, h * 0.43f, 88f, 0x332C7BE5, phase);
        orbit(canvas, cx, h * 0.43f, 122f, 0x2291B8FF, -phase * 0.73f);
        postInvalidateDelayed(32);
    }

    private void halo(Canvas c, float x, float y, float r, int color) {
        fill.setShader(new RadialGradient(x - r * 0.2f, y - r * 0.2f, r, color, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r, fill);
        fill.setShader(null);
    }

    private void node(Canvas c, float x, float y, float r, int color) {
        fill.setColor(color);
        c.drawCircle(x, y, r, fill);
        fill.setColor(0x33FFFFFF);
        c.drawCircle(x - r * 0.30f, y - r * 0.30f, r * 0.35f, fill);
    }

    private void orbit(Canvas c, float cx, float cy, float r, int color, float p) {
        stroke.setColor(color);
        stroke.setStrokeWidth(2f);
        canvasEllipse(c, cx, cy, r, r * 0.33f, p);
    }

    private void canvasEllipse(Canvas c, float cx, float cy, float rx, float ry, float angle) {
        c.save();
        c.rotate((float)Math.toDegrees(angle), cx, cy);
        c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, stroke);
        c.restore();
    }
}
