package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** A gold crescent and star, drawn with paths so there is no image to ship. */
final class CrescentView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path crescent = new Path();
    private final Path star = new Path();

    CrescentView(Context context, int color) {
        super(context);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float r = Math.min(w, h) * 0.46f, cx = w / 2f, cy = h / 2f;
        Path outer = new Path();
        outer.addCircle(cx, cy, r, Path.Direction.CW);
        Path cut = new Path();
        cut.addCircle(cx + r * 0.42f, cy - r * 0.08f, r * 0.82f, Path.Direction.CW);
        crescent.rewind();
        crescent.op(outer, cut, Path.Op.DIFFERENCE);
        // five-point star to the right of the opening
        star.rewind();
        float sx = cx + r * 0.52f, sy = cy - r * 0.08f, sr = r * 0.2f;
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float rr = (i % 2 == 0) ? sr : sr * 0.42f;
            float x = sx + (float) Math.cos(a) * rr, y = sy + (float) Math.sin(a) * rr;
            if (i == 0) star.moveTo(x, y);
            else star.lineTo(x, y);
        }
        star.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(crescent, paint);
        canvas.drawPath(star, paint);
    }
}
