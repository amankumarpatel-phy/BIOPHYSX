package in.aman.biophysx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

public class FloatingBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float t = 0f;

    public FloatingBackgroundView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        canvas.drawColor(0xFFF7FBFF);
        drawOrb(canvas, w*0.12f, h*(0.16f + 0.025f*(float)Math.sin(t*0.7)), 95, 0x447FD8FF);
        drawOrb(canvas, w*0.88f, h*(0.22f + 0.03f*(float)Math.cos(t*0.6)), 120, 0x44C7A6FF);
        drawOrb(canvas, w*0.72f, h*(0.75f + 0.025f*(float)Math.sin(t*0.8)), 140, 0x44A9E6D0);
        drawOrb(canvas, w*0.20f, h*(0.82f + 0.03f*(float)Math.cos(t*0.5)), 85, 0x44FFD1B8);
        t += 0.016f;
        postInvalidateDelayed(16);
    }

    private void drawOrb(Canvas c, float x, float y, float r, int color) {
        paint.setShader(new RadialGradient(x-r*0.2f, y-r*0.2f, r, color, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r, paint);
        paint.setShader(null);
    }
}
