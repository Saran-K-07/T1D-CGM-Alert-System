package com.example.t1dalert;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.List;

public class MlPredictionChartActivity extends AppCompatActivity {

    private MlPredictionChartView chartView;
    private TextView chartSummary;
    private Spinner windowSpinner;
    private SwitchCompat diffSwitch;
    private List<MlPredictionSeriesStore.Point> allPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ml_prediction_chart);

        SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        allPoints = MlPredictionSeriesStore.readPoints(prefs);

        chartView = findViewById(R.id.chart_view);
        chartSummary = findViewById(R.id.chart_summary);
        windowSpinner = findViewById(R.id.window_spinner);
        diffSwitch = findViewById(R.id.diff_switch);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Last 12", "Last 24", "Last 48", "All"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        windowSpinner.setAdapter(adapter);

        int savedSelection = prefs.getInt(AppPrefs.KEY_ML_CHART_WINDOW_SIZE, 1);
        if (savedSelection < 0 || savedSelection > 3) {
            savedSelection = 1;
        }
        windowSpinner.setSelection(savedSelection);

        windowSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(AppPrefs.KEY_ML_CHART_WINDOW_SIZE, position).apply();
                renderChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                renderChart();
            }
        });

        diffSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> renderChart());

        renderChart();
    }

    private void renderChart() {
        List<MlPredictionSeriesStore.Point> points = slicePoints(windowSpinner.getSelectedItemPosition());
        chartView.setShowDiff(diffSwitch.isChecked());
        chartView.setData(points);
        chartSummary.setText(buildSummary(points));
    }

    private List<MlPredictionSeriesStore.Point> slicePoints(int selection) {
        if (allPoints == null || allPoints.isEmpty()) {
            return new ArrayList<>();
        }
        int desired;
        if (selection == 0) {
            desired = 12;
        } else if (selection == 1) {
            desired = 24;
        } else if (selection == 2) {
            desired = 48;
        } else {
            desired = allPoints.size();
        }
        int start = Math.max(0, allPoints.size() - desired);
        return new ArrayList<>(allPoints.subList(start, allPoints.size()));
    }

    private String buildSummary(List<MlPredictionSeriesStore.Point> points) {
        if (points == null || points.isEmpty()) {
            return getString(R.string.ml_chart_summary_empty);
        }
        int sumAbs = 0;
        int maxAbs = 0;
        for (MlPredictionSeriesStore.Point p : points) {
            int d = Math.abs(p.predictedMgdl - p.actualMgdl);
            sumAbs += d;
            if (d > maxAbs) {
                maxAbs = d;
            }
        }
        int mae = Math.round(sumAbs / (float) points.size());
        return getString(R.string.ml_chart_summary_value, points.size(), mae, maxAbs);
    }
}
