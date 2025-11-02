package com.example.bagetmamanger2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BegetManagerFragment extends Fragment {

    private TextView totalBudgetText, biggestExpenseText;
    private TextView hideText, showText; // Hide and Show toggle
    private double totalBudget = 0.0;
    private boolean isHidden = true; // ✅ Hidden by default

    private CardView[] cards;
    private TextView[] cardTexts;
    private String[] categories = {
            "Relationship", "Fitness", "Tech", "Social", "Unexpected",
            "Transport", "DayTravel", "Church"
    };

    private BudgetDbHelper dbHelper;

    public static BegetManagerFragment newInstance() {
        return new BegetManagerFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_beget_manager, container, false);

        dbHelper = new BudgetDbHelper(getContext());

        totalBudgetText = view.findViewById(R.id.totalBegetText);
        biggestExpenseText = view.findViewById(R.id.biggestExpenseText);
        hideText = view.findViewById(R.id.hideText);
        showText = view.findViewById(R.id.showText);

        totalBudget = dbHelper.getTotalBudget();

        // ✅ Start with total budget hidden
        totalBudgetText.setText("****");
        hideText.setVisibility(View.GONE);
        showText.setVisibility(View.VISIBLE);

        setupHideShowButtons();

        int[] cardResIds = {
                R.id.relationshipCard, R.id.fitnessCard, R.id.techCard, R.id.socialCard,
                R.id.unexpectedCard, R.id.transportCard, R.id.dayTravelCard, R.id.churchDonationCard
        };

        String[] cardTextIds = {
                "relationshipInput", "fitnessInput", "techInput", "socialInput",
                "unexpectedInput", "transportInput", "dayTravelInput", "churchDonationInput"
        };

        cards = new CardView[categories.length];
        cardTexts = new TextView[categories.length];

        for (int i = 0; i < categories.length; i++) {
            int index = i;
            cards[i] = view.findViewById(cardResIds[i]);
            int textResId = getResources().getIdentifier(cardTextIds[i], "id", requireContext().getPackageName());
            cardTexts[i] = view.findViewById(textResId);

            double savedAmount = dbHelper.getAmount(categories[i]);
            updateCardText(index, savedAmount);

            cards[i].setOnClickListener(v -> showExpenseInputDialog(index));
            cards[i].setOnLongClickListener(v -> {
                showHistoryDialog(index);
                return true;
            });
        }

        CardView totalCard = view.findViewById(R.id.totalBegetCard);
        totalCard.setOnClickListener(v -> showTotalInputDialog());
        totalCard.setOnLongClickListener(v -> {
            showTotalBudgetEditDialog();
            return true;
        });

        updateBiggestExpense();
        return view;
    }

    // 🔹 Setup Hide/Show Buttons
    private void setupHideShowButtons() {
        hideText.setOnClickListener(v -> {
            totalBudgetText.setText("****");
            hideText.setVisibility(View.GONE);
            showText.setVisibility(View.VISIBLE);
            isHidden = true;
        });

        showText.setOnClickListener(v -> {
            totalBudgetText.setText(String.format("%.2f", totalBudget));
            showText.setVisibility(View.GONE);
            hideText.setVisibility(View.VISIBLE);
            isHidden = false;
        });
    }

    private void showTotalInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter Total Budget");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String value = input.getText().toString().trim();
            if (!value.isEmpty()) {
                double addedValue = Double.parseDouble(value);
                totalBudget += addedValue;
                dbHelper.addToTotalBudget(addedValue);
                updateTotalBudgetText();
                resetCategoryTexts();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showTotalBudgetEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Total Budget");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format("%.2f", totalBudget));
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String valStr = input.getText().toString().trim();
            if (!valStr.isEmpty()) {
                double newTotal = Double.parseDouble(valStr);
                totalBudget = newTotal;
                dbHelper.updateTotalBudget(newTotal);
                updateTotalBudgetText();
                resetCategoryTexts();
            }
        });

        builder.setNegativeButton("Reset", (dialog, which) -> {
            totalBudget = 0;
            dbHelper.deleteTotalBudget();
            updateTotalBudgetText();
            resetCategoryTexts();
            biggestExpenseText.setText("Biggest Expense");
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // ✅ Refresh category cards
    private void resetCategoryTexts() {
        for (int i = 0; i < categories.length; i++) {
            updateCardText(i, dbHelper.getAmount(categories[i]));
        }
        updateBiggestExpense();
    }

    // ✅ Add expense and subtract from total
    private void showExpenseInputDialog(int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter Expense for " + categories[index]);

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String valueStr = input.getText().toString().trim();
            if (!valueStr.isEmpty() && totalBudget > 0) {
                double value = Double.parseDouble(valueStr);

                if (value > totalBudget) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Insufficient Budget")
                            .setMessage("You don't have enough remaining budget!")
                            .setPositiveButton("OK", (d, w) -> d.dismiss())
                            .show();
                    return;
                }

                totalBudget -= value;
                dbHelper.updateTotalBudget(totalBudget);
                updateTotalBudgetText();

                dbHelper.addExpense(categories[index], value);
                updateCardText(index, dbHelper.getAmount(categories[index]));
                updateBiggestExpense();
            } else if (totalBudget <= 0) {
                new AlertDialog.Builder(getContext())
                        .setTitle("No Budget")
                        .setMessage("Please add a total budget before adding expenses.")
                        .setPositiveButton("OK", (d, w) -> d.dismiss())
                        .show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // ✅ Show expense history
    private void showHistoryDialog(int index) {
        List<Map<String, Object>> history = dbHelper.getAllData(categories[index]);
        if (history.isEmpty()) {
            new AlertDialog.Builder(getContext())
                    .setTitle("No Data")
                    .setMessage("No entries for " + categories[index])
                    .setPositiveButton("OK", (d, w) -> d.dismiss())
                    .show();
            return;
        }

        ListView listView = new ListView(getContext());
        List<String> displayList = new ArrayList<>();
        for (Map<String, Object> entry : history) {
            displayList.add(String.format("%.2f - %s", entry.get("amount"), entry.get("date")));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            int entryId = (int) history.get(position).get("id");
            showEditDeleteDialog(index, entryId, adapter, position, history);
        });

        new AlertDialog.Builder(getContext())
                .setTitle(categories[index] + " History")
                .setView(listView)
                .setPositiveButton("Close", (d, w) -> d.dismiss())
                .show();
    }

    private void showEditDeleteDialog(int categoryIndex, int entryId,
                                      ArrayAdapter<String> adapter, int position,
                                      List<Map<String, Object>> history) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit or Delete Entry");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(history.get(position).get("amount")));
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String valStr = input.getText().toString().trim();
            if (!valStr.isEmpty()) {
                double newAmount = Double.parseDouble(valStr);
                dbHelper.updateData(categories[categoryIndex], entryId, newAmount);
                updateCardText(categoryIndex, dbHelper.getAmount(categories[categoryIndex]));
                adapter.remove(adapter.getItem(position));
                adapter.insert(String.format("%.2f - %s", newAmount, history.get(position).get("date")), position);
                adapter.notifyDataSetChanged();
                updateBiggestExpense();
            }
        });

        builder.setNegativeButton("Delete", (dialog, which) -> {
            dbHelper.deleteData(categories[categoryIndex], entryId);
            updateCardText(categoryIndex, dbHelper.getAmount(categories[categoryIndex]));
            adapter.remove(adapter.getItem(position));
            adapter.notifyDataSetChanged();
            updateBiggestExpense();
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateBiggestExpense() {
        double max = 0;
        String maxCategory = "";
        for (int i = 0; i < categories.length; i++) {
            double val = dbHelper.getAmount(categories[i]);
            if (val > max) {
                max = val;
                maxCategory = categories[i];
            }
        }
        if (max > 0) {
            biggestExpenseText.setText(String.format("%s: %.2f", maxCategory, max));
        } else {
            biggestExpenseText.setText("Biggest Expense");
        }
    }

    private void updateCardText(int index, double amount) {
        double percent = totalBudget > 0 ? (amount / (totalBudget + amount)) * 100 : 0.0;
        cardTexts[index].setText(String.format("%.2f (%.1f%%)", amount, percent));
    }

    private void updateTotalBudgetText() {
        if (isHidden) {
            totalBudgetText.setText("****");
        } else {
            totalBudgetText.setText(String.format("%.2f", totalBudget));
        }
    }
}
