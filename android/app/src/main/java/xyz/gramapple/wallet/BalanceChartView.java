/*
✨ CoonDev • https://dev.coonlink.com/

 ▄█▄    ████▄ ████▄    ▄   ██▄   ▄███▄      ▄
 █▀ ▀▄  █   █ █   █     █  █  █  █▀   ▀      █
 █   ▀  █   █ █   █ ██   █ █   █ ██▄▄   █     █
 █▄  ▄▀ ▀████ ▀████ █ █  █ █  █  █▄   ▄▀ █    █
 ▀███▀              █  █ █ ███▀  ▀███▀    █  █
                    █   ██                 █▐
                                           ▐
*/

package xyz.gramapple.wallet;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class BalanceChartView extends View {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path line = new Path();
    private final Path fill = new Path();
    private double[] values = new double[0];
    private float density;
    public BalanceChartView(Context context) {super(context); init();}
    public BalanceChartView(Context context, AttributeSet attrs) {super(context, attrs); init();}
    public BalanceChartView(Context context, AttributeSet attrs, int defStyleAttr) {super(context, attrs, defStyleAttr); init();}

    private void init() {
        density = getResources().getDisplayMetrics().density;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.2f * density);
        linePaint.setColor(0xFFFFFFFF);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        fillPaint.setStyle(Paint.Style.FILL);
        gridPaint.setColor(0x22FFFFFF);
        gridPaint.setStrokeWidth(density);
    }

    public void setValues(double[] next) {
        if (next == null || next.length == 0) {
            if (values.length == 0) {return;}
            values = new double[0];
            invalidate();
            return;
        }
        int end = next.length;
        double max = 0;
        for (double v : next) {if (v > max) {max = v;}}
        if (max > 0) {
            while (end > 1 && next[end - 1] <= 0d) {end--;}
        }
        double[] trimmed = end == next.length ? next : java.util.Arrays.copyOf(next, end);
        if (java.util.Arrays.equals(values, trimmed)) {return;}
        values = trimmed;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = Math.round(112 * density);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0 || values.length < 2) {return;}
        float padX = 4 * density;
        float padY = 10 * density;
        double min = values[0], max = values[0];
        for (double v : values) {if (v < min) {min = v;} if (v > max) {max = v;}}
        if (max - min < 1e-9) {max = min + 1;}
        float usableW = w - padX * 2f;
        float usableH = h - padY * 2f;
        for (int i = 1; i <= 3; i++) {
            float y = padY + usableH * i / 4f;
            canvas.drawLine(padX, y, w - padX, y, gridPaint);
        }
        line.reset();
        fill.reset();
        for (int i = 0; i < values.length; i++) {
            float x = padX + usableW * i / (values.length - 1f);
            float y = padY + usableH * (float) (1.0 - (values[i] - min) / (max - min));
            if (i == 0) {line.moveTo(x, y); fill.moveTo(x, h - padY); fill.lineTo(x, y);}
            else {line.lineTo(x, y); fill.lineTo(x, y);}
            if (i == values.length - 1) {fill.lineTo(x, h - padY); fill.close();}
        }
        fillPaint.setShader(new LinearGradient(0, padY, 0, h, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawPath(fill, fillPaint);
        fillPaint.setShader(null);
        canvas.drawPath(line, linePaint);
    }
}
