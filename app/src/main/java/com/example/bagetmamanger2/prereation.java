package com.example.bagetmamanger2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class prereation extends AppCompatActivity {

    private BudgetDbHelper dbHelper;
    private LineChart chart;
    private TextView statusBar;

    private List<Map<String, Object>> expenses;

    private final String[] categories = {
            "Relationship", "Fitness", "Tech", "Social", "Unexpected",
            "Transport", "DayTravel", "Church"
    };

    private Button btnR, btnF, btnT, btnS, btnU, btnTr, btnDT, btnCH;
    private Button selectedButton = null; // tracks the active one

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

        // Buttons
        btnR = findViewById(R.id.btnR);
        btnF = findViewById(R.id.btnF);
        btnT = findViewById(R.id.btnT);
        btnS = findViewById(R.id.btnS);
        btnU = findViewById(R.id.btnU);
        btnTr = findViewById(R.id.btnTr);
        btnDT = findViewById(R.id.btnDT);
        btnCH = findViewById(R.id.btnCH);

        // Load all data on startup
        showDataFromDatabase(null);

        // Attach listeners
        setupButton(btnR, "Relationship");
        setupButton(btnF, "Fitness");
        setupButton(btnT, "Tech");
        setupButton(btnS, "Social");
        setupButton(btnU, "Unexpected");
        setupButton(btnTr, "Transport");
        setupButton(btnDT, "DayTravel");
        setupButton(btnCH, "Church");

        // Chart click: show selected expense info
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (expenses != null && e.getX() < expenses.size()) {
                    Map<String, Object> exp = expenses.get((int) e.getX());
                    String category = (String) exp.get("category");
                    double amount = (double) exp.get("amount");
                    String date = (String) exp.get("date");

                    statusBar.setText(String.format("Spent %.2f on %s (%s)", amount, category, date));
                }
            }

            @Override
            public void onNothingSelected() {
                statusBar.setText("Selected point and result");
            }
        });
    }

    private void setupButton(Button button, String category) {
        button.setOnClickListener(v -> {
            // reset previous selected button
            if (selectedButton != null) {
                selectedButton.setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
            }
            selectedButton = button;
            button.setBackgroundTintList(getColorStateList(android.R.color.holo_green_dark));

            showDataFromDatabase(category);
        });
    }

    private void showDataFromDatabase(String filterCategory) {
        if (filterCategory == null) {
            // Load all
            expenses = dbHelper.getAllExpenses();
            statusBar.setText("Showing all expenses");
        } else {
            // Load only selected category
            expenses = dbHelper.getExpensesByCategory(filterCategory);
            statusBar.setText("Showing " + filterCategory + " expenses");
        }

        if (expenses == null || expenses.isEmpty()) {
            chart.clear();
            statusBar.setText("No data found");
            return;
        }

        List<Entry> entries = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> exp : expenses) {
            double amount = (double) exp.get("amount");
            entries.add(new Entry(index++, (float) amount));
        }

        LineDataSet dataSet = new LineDataSet(entries,
                (filterCategory == null ? "All Categories" : filterCategory) + " Over Time");
        dataSet.setColor(getColor(android.R.color.holo_red_dark));
        dataSet.setValueTextColor(getColor(android.R.color.black));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);

        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setText("Time vs Expenditure");
        chart.animateY(1000);
        chart.invalidate();
    }
}
