package com.example.t1dalert.ML;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.t1dalert.Core.AppConfig;

import java.util.ArrayList;
import java.util.List;

public class MlPredictionChartView extends View {

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint predictedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint actualPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint diffPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<MlPredictionSeriesStore.Point> points = new ArrayList<>();
    private boolean showDiff = false;

    public MlPredictionChartView(Context context) {
        super(context);
        init();
    }

    public MlPredictionChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MlPredictionChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        axisPaint.setColor(Color.parseColor("#222222"));
        axisPaint.setStrokeWidth(3f);

        predictedPaint.setColor(Color.parseColor("#1E88E5"));
        predictedPaint.setStrokeWidth(5f);
        predictedPaint.setStyle(Paint.Style.STROKE);

        actualPaint.setColor(Color.parseColor("#D32F2F"));
        actualPaint.setStrokeWidth(5f);
        actualPaint.setStyle(Paint.Style.STROKE);

        diffPaint.setColor(Color.parseColor("#2E7D32"));
        diffPaint.setStrokeWidth(4f);
        diffPaint.setStyle(Paint.Style.STROKE);

        gridPaint.setColor(Color.parseColor("#DDDDDD"));
        gridPaint.setStrokeWidth(2f);

        textPaint.setColor(Color.parseColor("#111111"));
        textPaint.setTextSize(28f);
    }

    public void setData(List<MlPredictionSeriesStore.Point> points) {
        this.points = points == null ? new ArrayList<>() : points;
        invalidate();
    }

    public void setShowDiff(boolean showDiff) {
        this.showDiff = showDiff;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = 90f;
        float top = 40f;
        float right = getWidth() - 24f;
        float bottom = getHeight() - 70f;

        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);

        for (int i = 1; i <= 4; i++) {
            float gy = top + i * ((bottom - top) / 5f);
            canvas.drawLine(left, gy, right, gy, gridPaint);
        }

        if (points == null || points.size() < 2) {
            canvas.drawText("Not enough data", left, top + 30f, textPaint);
            return;
        }

        List<MlPredictionSeriesStore.Point> alignedPredicted = alignedPredictedPoints(points);
        List<MlPredictionSeriesStore.Point> alignedActual = alignedActualPoints(points);
        if (alignedPredicted.size() < 2 || alignedActual.size() < 2) {
            canvas.drawText("Need more 1h-aligned data", left, top + 30f, textPaint);
            return;
        }

        int minVal = Integer.MAX_VALUE;
        int maxVal = Integer.MIN_VALUE;
        for (MlPredictionSeriesStore.Point p : alignedPredicted) {
            minVal = Math.min(minVal, p.predictedMgdl);
            maxVal = Math.max(maxVal, p.predictedMgdl);
        }
        for (MlPredictionSeriesStore.Point p : alignedActual) {
            minVal = Math.min(minVal, p.actualMgdl);
            maxVal = Math.max(maxVal, p.actualMgdl);
        }
        if (showDiff) {
            for (int i = 0; i < Math.min(alignedPredicted.size(), alignedActual.size()); i++) {
                int d = Math.abs(alignedPredicted.get(i).predictedMgdl - alignedActual.get(i).actualMgdl);
                minVal = Math.min(minVal, d);
                maxVal = Math.max(maxVal, d);
            }
        }
        if (maxVal <= minVal) {
            maxVal = minVal + 1;
        }

        float usableW = right - left;
        float usableH = bottom - top;
        int nPred = alignedPredicted.size();
        int nAct = alignedActual.size();

        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        for (MlPredictionSeriesStore.Point p : alignedPredicted) {
            minTime = Math.min(minTime, p.timeMs);
            maxTime = Math.max(maxTime, p.timeMs);
        }
        for (MlPredictionSeriesStore.Point p : alignedActual) {
            minTime = Math.min(minTime, p.timeMs);
            maxTime = Math.max(maxTime, p.timeMs);
        }
        if (maxTime <= minTime) {
            maxTime = minTime + 1L;
        }

        float prevPredX = -1f;
        float prevPredY = -1f;
        for (int i = 0; i < nPred; i++) {
            MlPredictionSeriesStore.Point p = alignedPredicted.get(i);
            float x = toX(p.timeMs, minTime, maxTime, left, usableW);
            float predY = toY(p.predictedMgdl, minVal, maxVal, top, usableH);
            if (prevPredX >= 0f) {
                canvas.drawLine(prevPredX, prevPredY, x, predY, predictedPaint);
            }
            prevPredX = x;
            prevPredY = predY;
        }

        float prevActX = -1f;
        float prevActualY = -1f;
        for (int i = 0; i < nAct; i++) {
            MlPredictionSeriesStore.Point p = alignedActual.get(i);
            float x = toX(p.timeMs, minTime, maxTime, left, usableW);
            float actY = toY(p.actualMgdl, minVal, maxVal, top, usableH);
            if (prevActX >= 0f) {
                canvas.drawLine(prevActX, prevActualY, x, actY, actualPaint);
            }
            prevActX = x;
            prevActualY = actY;
        }

        if (showDiff) {
            int dCount = Math.min(nPred, nAct);
            float prevDiffX = -1f;
            float prevDiffY = -1f;
            for (int i = 0; i < dCount; i++) {
                int d = Math.abs(alignedPredicted.get(i).predictedMgdl - alignedActual.get(i).actualMgdl);
                float x = toX(alignedPredicted.get(i).timeMs, minTime, maxTime, left, usableW);
                float dY = toY(d, minVal, maxVal, top, usableH);
                if (prevDiffX >= 0f) {
                    canvas.drawLine(prevDiffX, prevDiffY, x, dY, diffPaint);
                }
                prevDiffX = x;
                prevDiffY = dY;
            }
        }

        canvas.drawText("min " + minVal, left, top - 8f, textPaint);
        canvas.drawText("max " + maxVal, right - 140f, top - 8f, textPaint);
    }

    private float toY(int value, int minVal, int maxVal, float top, float usableH) {
        return top + (maxVal - value) * (usableH / (float) (maxVal - minVal));
    }

    private float toX(long timeMs, long minTime, long maxTime, float left, float usableW) {
        return left + ((timeMs - minTime) / (float) (maxTime - minTime)) * usableW;
    }

    private List<MlPredictionSeriesStore.Point> alignedPredictedPoints(List<MlPredictionSeriesStore.Point> source) {
        ArrayList<MlPredictionSeriesStore.Point> out = new ArrayList<>();
        for (MlPredictionSeriesStore.Point p : source) {
            out.add(new MlPredictionSeriesStore.Point(
                    p.timeMs + AppConfig.ML_PREDICTION_HORIZON_MS,
                    p.predictedMgdl,
                    p.actualMgdl
            ));
        }
        return out;
    }

    private List<MlPredictionSeriesStore.Point> alignedActualPoints(List<MlPredictionSeriesStore.Point> source) {
        return new ArrayList<>(source);
    }
}
