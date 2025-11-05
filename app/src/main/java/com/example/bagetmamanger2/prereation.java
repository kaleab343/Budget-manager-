package com.example.bagetmamanger2;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.color.MaterialColors;
import androidx.core.content.ContextCompat;
import android.graphics.Color;
import java.text.NumberFormat;
import java.util.Currency;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class prereation extends AppCompatActivity {

    private BudgetDbHelper dbHelper;
    private LineChart chart;
    private TextView statusBar;

    private List<Map<String, Object>> expenses;

    // Dynamic category filter chips
    private com.google.android.material.chip.ChipGroup categoryGroup;
    private String selectedCategory = null; // null means all

    // Granularity & date range
    private ChipGroup granularityGroup;
    private Chip chipDay, chipMonth, chipYear;
    private MaterialButton btnCustomRange;

    private String startDate; // yyyy-MM-dd
    private String endDate;   // yyyy-MM-dd

    private enum Granularity { DAY, MONTH, YEAR }
    private Granularity granularity = Granularity.DAY;

    private final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_prereation);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new BudgetDbHelper(this);
        chart = findViewById(R.id.lineChart);
        statusBar = findViewById(R.id.statusBar);

        // Setup toolbar consistent with app
        com.google.android.material.appbar.MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            setSupportActionBar(topAppBar);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Analytics");
        }

        // Dynamic category chips
        categoryGroup = findViewById(R.id.categoryGroup);
        buildCategoryChips();

        // Granularity chips & custom range
        granularityGroup = findViewById(R.id.granularityGroup);
        chipDay = findViewById(R.id.chipDay);
        chipMonth = findViewById(R.id.chipMonth);
        chipYear = findViewById(R.id.chipYear);
        btnCustomRange = findViewById(R.id.btnCustomRange);

        if (granularityGroup != null) {
            granularityGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                if (id == R.id.chipDay) granularity = Granularity.DAY;
                else if (id == R.id.chipMonth) granularity = Granularity.MONTH;
                else if (id == R.id.chipYear) granularity = Granularity.YEAR;
                refreshData();
            });
        }

        if (btnCustomRange != null) {
            btnCustomRange.setOnClickListener(v -> pickCustomRange());
        }

        // Default date range = last 30 days
        Calendar cal = Calendar.getInstance();
        Date end = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        Date start = cal.getTime();
        startDate = DATE_FMT.format(start);
        endDate = DATE_FMT.format(end);

        // Improve chart styling
        styleChart();

        // Initial load
        refreshData();

        // Chart click: show selected point info
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int xIndex = (int) e.getX();
                if (xIndex >= 0 && xIndex < labels.size()) {
                    String label = labels.get(xIndex);
                    String cat = (selectedCategory == null ? getString(R.string.chip_all) : selectedCategory);
                    statusBar.setText(String.format(Locale.getDefault(), "%s — %s: %s", cat, label, formatCurrency(e.getY())));
                }
            }

            @Override
            public void onNothingSelected() {
                statusBar.setText("Selected point and result");
            }
        });
    }

    private void buildCategoryChips() {
        if (categoryGroup == null) return;
        categoryGroup.removeAllViews();

        // Load categories from DB
        List<Map<String, Object>> cats = dbHelper.getCategories();
        List<String> names = new ArrayList<>();
        for (Map<String, Object> c : cats) names.add((String) c.get("name"));
        // If current selection no longer exists, reset to All
        if (selectedCategory != null && !names.contains(selectedCategory)) {
            selectedCategory = null;
        }

        // Add an "All" chip
        Chip allChip = new Chip(this);
        allChip.setText("All");
        allChip.setCheckable(true);
        allChip.setChecked(selectedCategory == null);
        allChip.setOnClickListener(v -> {
            selectedCategory = null;
            refreshData();
        });
        categoryGroup.addView(allChip);

        // Build chips from DB categories
        for (String name : names) {
            Chip chip = new Chip(this);
            chip.setText(name);
            chip.setCheckable(true);
            chip.setChecked(name.equals(selectedCategory));
            chip.setOnClickListener(v -> {
                selectedCategory = chip.isChecked() ? name : null;
                if (chip.isChecked()) {
                    // uncheck others
                    for (int i = 0; i < categoryGroup.getChildCount(); i++) {
                        Chip other = (Chip) categoryGroup.getChildAt(i);
                        if (other != chip) other.setChecked(false);
                    }
                }
                refreshData();
            });
            categoryGroup.addView(chip);
        }
    }

    private void styleChart() {
        chart.setNoDataText("No data available");
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        // X Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(0xFF333333);

        // Y Axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(0x33AAAAAA);
        leftAxis.setTextColor(0xFF333333);
        chart.getAxisRight().setEnabled(false);

        // Legend
        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
    }

    private List<String> labels = new ArrayList<>();

    private void refreshData() {
        // Load from DB depending on date range and category
        if (startDate != null && endDate != null) {
            expenses = dbHelper.getExpensesBetween(selectedCategory, startDate, endDate);
            statusBar.setText(String.format(Locale.getDefault(), "Showing %s from %s to %s",
                    selectedCategory == null ? "all" : selectedCategory, startDate, endDate));
        } else if (selectedCategory == null) {
            expenses = dbHelper.getAllExpenses();
            statusBar.setText("Showing all expenses");
        } else {
            expenses = dbHelper.getExpensesByCategory(selectedCategory);
            statusBar.setText("Showing " + selectedCategory + " expenses");
        }

        if (expenses == null || expenses.isEmpty()) {
            chart.clear();
            chart.invalidate();
            return;
        }

        // Group by granularity
        Map<String, Float> grouped = groupExpenses(expenses, granularity);

        // Build labels and entries in order
        labels = new ArrayList<>(grouped.keySet());
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            entries.add(new Entry(i, grouped.get(labels.get(i))));
        }

        LineDataSet dataSet = new LineDataSet(entries,
                (selectedCategory == null ? "All Categories" : selectedCategory));
        int primary = ContextCompat.getColor(this, R.color.md_theme_primary);
        int onSurfaceVar = ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant);
        dataSet.setColor(primary);
        dataSet.setValueTextColor(onSurfaceVar);
        dataSet.setLineWidth(2.2f);
        dataSet.setCircleRadius(3.5f);
        dataSet.setDrawCircles(true);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(60);
        dataSet.setFillColor(primary);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // X axis labels
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setLabelRotationAngle(315f);
        chart.animateY(800);
        chart.invalidate();
    }

    private Map<String, Float> groupExpenses(List<Map<String, Object>> list, Granularity g) {
        // Use LinkedHashMap to keep insertion order after sorting keys
        Map<String, Float> map = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>();

        for (Map<String, Object> exp : list) {
            String dateStr = (String) exp.get("date");
            double amount = (double) exp.get("amount");
            String key = bucketKey(dateStr, g);
            if (key == null) continue;
            if (!map.containsKey(key)) {
                map.put(key, 0f);
                keys.add(key);
            }
            map.put(key, map.get(key) + (float) amount);
        }

        // Sort keys chronologically by parsing representative date
        Collections.sort(keys, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                Date da = parseBucketKey(a, g);
                Date db = parseBucketKey(b, g);
                if (da == null || db == null) return a.compareTo(b);
                return da.compareTo(db);
            }
        });

        // Build ordered map
        Map<String, Float> ordered = new LinkedHashMap<>();
        for (String k : keys) {
            ordered.put(k, map.get(k));
        }
        return ordered;
    }

    private String bucketKey(String dateStr, Granularity g) {
        try {
            Date d = DATE_FMT.parse(dateStr);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            switch (g) {
                case DAY:
                    return DATE_FMT.format(d); // yyyy-MM-dd
                case MONTH:
                    return String.format(Locale.getDefault(), "%04d-%02d",
                            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
                case YEAR:
                    return String.format(Locale.getDefault(), "%04d", c.get(Calendar.YEAR));
            }
        } catch (ParseException ignored) {}
        return null;
    }

    private Date parseBucketKey(String key, Granularity g) {
        try {
            switch (g) {
                case DAY:
                    return DATE_FMT.parse(key);
                case MONTH:
                    return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(key);
                case YEAR:
                    return new SimpleDateFormat("yyyy", Locale.getDefault()).parse(key);
            }
        } catch (ParseException ignored) {}
        return null;
    }

    private String formatCurrency(float amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        try { nf.setCurrency(Currency.getInstance("ETB")); } catch (Exception ignored) {}
        return nf.format(amount);
    }

    private void pickCustomRange() {
        // pick start, then end
        final Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog startPicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar c1 = Calendar.getInstance();
            c1.set(year, month, dayOfMonth, 0, 0, 0);
            startDate = DATE_FMT.format(c1.getTime());

            DatePickerDialog endPicker = new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                Calendar c2 = Calendar.getInstance();
                c2.set(year2, month2, dayOfMonth2, 23, 59, 59);
                endDate = DATE_FMT.format(c2.getTime());
                refreshData();
            }, y, m, d);
            endPicker.setTitle("Select end date");
            endPicker.show();
        }, y, m, d);
        startPicker.setTitle("Select start date");
        startPicker.show();
    }
}
