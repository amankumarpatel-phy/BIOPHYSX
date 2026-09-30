package in.aman.biophysx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class WaveformView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Float> samples = new ArrayList<>();
    private float phase = 0;
    private boolean demo = true;

    public WaveformView(Context context) {
        super(context);
        line.setColor(0xFF2C7BE5); line.setStrokeWidth(5f); line.setStyle(Paint.Style.STROKE);
        grid.setColor(0xFFE4EDF5); grid.setStrokeWidth(1f);
        for (int i=0;i<220;i++) samples.add(0.5f);
        setContentDescription("Live physiological waveform");
    }

    public void setDemo(boolean demoMode) { this.demo = demoMode; }

    public void addSample(float value) {
        samples.add(Math.max(-1f, Math.min(1f, value)));
        if (samples.size() > 220) samples.remove(0);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w=getWidth(), h=getHeight(), mid=h*0.55f;
        canvas.drawColor(0xFFFDFEFF);
        for(int i=1;i<8;i++) canvas.drawLine(i*w/8f, 0, i*w/8f, h, grid);
        for(int i=1;i<4;i++) canvas.drawLine(0, i*h/4f, w, i*h/4f, grid);
        Path p=new Path();
        for(int i=0;i<samples.size();i++){
            float x=i*(w/(samples.size()-1f));
            float y=mid - samples.get(i)*(h*0.34f);
            if(i==0) p.moveTo(x,y); else p.lineTo(x,y);
        }
        canvas.drawPath(p,line);
        if(demo){
            phase += 0.11f;
            float v=(float)(0.65*Math.sin(phase*1.05)+0.09*Math.sin(phase*3.2));
            addSample(v);
        }
        postInvalidateDelayed(24);
    }
}
