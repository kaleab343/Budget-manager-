package com.example.bagetmamanger2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.widget.Toast;
import android.widget.ImageButton;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.color.MaterialColors;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;

public class BegetManagerFragment extends Fragment {

    private TextView totalBudgetText, biggestExpenseText;
    private TextView hideText, showText; // Hide and Show toggle
    private double totalBudget = 0.0;
    private boolean isHidden = true; // Hidden by default

    private LinearLayout categoriesContainer;
    private MaterialButton addCategoryButton;

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
        categoriesContainer = view.findViewById(R.id.categoriesContainer);
        addCategoryButton = view.findViewById(R.id.addCategoryButton);

        totalBudget = dbHelper.getTotalBudget();

        // Start with total budget hidden
        totalBudgetText.setText("****");
        hideText.setVisibility(View.GONE);
        showText.setVisibility(View.VISIBLE);

        setupHideShowButtons();

        // Add Category button
        if (addCategoryButton != null) {
            addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());
        }

        // Total budget card interactions
        View totalCard = view.findViewById(R.id.totalBegetCard);
        totalCard.setOnClickListener(v -> showTotalInputDialog());
        totalCard.setOnLongClickListener(v -> {
            showTotalBudgetEditDialog();
            return true;
        });

        // Initial render
        renderCategories();
        updateBiggestExpense();
        return view;
    }

    // ---------- UI wiring ----------
    private void setupHideShowButtons() {
        hideText.setOnClickListener(v -> {
            totalBudgetText.setText("****");
            hideText.setVisibility(View.GONE);
            showText.setVisibility(View.VISIBLE);
            isHidden = true;
        });

        showText.setOnClickListener(v -> {
            totalBudgetText.setText(String.format(Locale.getDefault(), "%.2f", totalBudget));
            showText.setVisibility(View.GONE);
            hideText.setVisibility(View.VISIBLE);
            isHidden = false;
        });
    }

    private void renderCategories() {
        if (categoriesContainer == null) return;
        categoriesContainer.removeAllViews();

        List<Map<String, Object>> categories = dbHelper.getCategories();
        for (Map<String, Object> cat : categories) {
            String name = (String) cat.get("name");
            double goal = (double) cat.get("goal");
            double spent = dbHelper.getAmount(name);

            categoriesContainer.addView(createCategoryCard(name, spent, goal));
        }
    }

    private View createCategoryCard(String name, double spent, double goal) {
        // Card
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(12);
        card.setLayoutParams(cardLp);
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(12));

        // Content container
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Row: Title + Amount
        LinearLayout topRow = new LinearLayout(requireContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(requireContext());
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleLp);
        title.setText(name);
        title.setTextSize(16);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);

        TextView amount = new TextView(requireContext());
        amount.setText(formatAmountAndPercent(spent, goal));
        amount.setTextSize(15);

        // 3-dot menu button
        ImageButton menuBtn = new ImageButton(requireContext());
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        menuBtn.setLayoutParams(menuLp);
        menuBtn.setImageResource(android.R.drawable.ic_menu_more);
        menuBtn.setBackground(null);
        menuBtn.setContentDescription("More actions");
        menuBtn.setOnClickListener(v -> showCategoryMenu(v, name));

        topRow.addView(title);
        topRow.addView(amount);
        topRow.addView(menuBtn);

        // Progress bar (only when goal > 0)
        if (goal > 0) {
            LinearProgressIndicator progress = new LinearProgressIndicator(requireContext(), null, com.google.android.material.R.attr.linearProgressIndicatorStyle);
            progress.setIndeterminate(false);
            progress.setTrackCornerRadius(dp(8));
            progress.setTrackThickness(dp(6));
            progress.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            double ratio = spent / goal;
            int percent = (int) Math.round(ratio * 100.0);
            int clampedPercent = Math.max(0, Math.min(100, percent));
            progress.setProgress(clampedPercent);

            TextView progressLabel = new TextView(requireContext());
            progressLabel.setTextSize(12);
            progressLabel.setPadding(0, dp(4), 0, 0);

            if (spent > goal) {
                // Over the goal: color the bar red and show how far off
                int red = ContextCompat.getColor(requireContext(), R.color.md_theme_error);
                int track = ContextCompat.getColor(requireContext(), R.color.md_theme_surfaceVariant);
                progress.setIndicatorColor(red);
                progress.setTrackColor(track);
                progressLabel.setTextColor(red);

                double over = spent - goal;
                double overPct = ((spent - goal) / goal) * 100.0;
                progressLabel.setText(String.format(Locale.getDefault(), "Over by %s (%.1f%% over)", formatCurrency(over), overPct));
            } else {
                // Within goal: use default/on-brand colors, show remaining
                int primary = ContextCompat.getColor(requireContext(), R.color.md_theme_primary);
                int track = ContextCompat.getColor(requireContext(), R.color.md_theme_surfaceVariant);
                int text = ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant);
                progress.setIndicatorColor(primary);
                progress.setTrackColor(track);
                progressLabel.setTextColor(text);

                double remaining = goal - spent;
                progressLabel.setText(String.format(Locale.getDefault(), "Goal: %s — %d%% (Remaining: %s)", formatCurrency(goal), clampedPercent, formatCurrency(remaining)));
            }

            content.addView(progress);
            content.addView(progressLabel);
        }

        // Row: actions
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        actions.setPadding(0, dp(8), 0, 0);

        MaterialButton btnAdd = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnAdd.setText("Add expense");
        btnAdd.setOnClickListener(v -> showExpenseInputDialog(name));

        MaterialButton btnGoal = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnGoal.setText("Set goal");
        btnGoal.setOnClickListener(v -> showSetGoalDialog(name, goal));

        MaterialButton btnHist = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnHist.setText("History");
        btnHist.setOnClickListener(v -> showHistoryDialog(name));

        MaterialButton btnDel = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnDel.setText("Delete");
        btnDel.setOnClickListener(v -> confirmDeleteCategory(name));

        actions.addView(btnAdd);
        actions.addView(spacer(dp(8)));
        actions.addView(btnGoal);
        actions.addView(spacer(dp(8)));
        actions.addView(btnHist);
        actions.addView(spacer(dp(8)));
        actions.addView(btnDel);

        content.addView(topRow);
        content.addView(actions);
        card.addView(content);

        // Simple click to add expense as well
        card.setOnClickListener(v -> showExpenseInputDialog(name));

        return card;
    }

    private View spacer(int widthDp) {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(widthDp, 1));
        return v;
    }

    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }

    // ---------- Dialogs ----------
    private void showAddCategoryDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(8), dp(16), 0);

        final EditText nameInput = new EditText(getContext());
        nameInput.setHint("Category name");
        layout.addView(nameInput);

        final EditText goalInput = new EditText(getContext());
        goalInput.setHint("Monthly goal (optional)");
        goalInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(goalInput);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Category")
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) return;
                    double goal = 0;
                    String g = goalInput.getText().toString().trim();
                    if (!g.isEmpty()) {
                        try { goal = Double.parseDouble(g); } catch (Exception ignored) {}
                    }
                    long id = dbHelper.addCategory(name, goal);
                    if (id == -1) {
                        Toast.makeText(getContext(), "Category already exists", Toast.LENGTH_SHORT).show();
                    }
                    renderCategories();
                    updateBiggestExpense();
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private void confirmDeleteCategory(String name) {
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.dialog_delete_category_title))
                .setMessage(getString(R.string.dialog_delete_category_message, name))
                .setPositiveButton(getString(R.string.action_delete), (d, w) -> {
                    dbHelper.deleteCategory(name);
                    renderCategories();
                    updateBiggestExpense();
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private void showSetGoalDialog(String name, double currentGoal) {
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.getDefault(), "%.2f", currentGoal));

        new AlertDialog.Builder(getContext())
                .setTitle("Set goal for " + name)
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String s = input.getText().toString().trim();
                    double goal = 0;
                    if (!s.isEmpty()) try { goal = Double.parseDouble(s); } catch (Exception ignored) {}
                    dbHelper.updateCategoryGoal(name, goal);
                    renderCategories();
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
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
                renderCategories();
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
        input.setText(String.format(Locale.getDefault(), "%.2f", totalBudget));
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String valStr = input.getText().toString().trim();
            if (!valStr.isEmpty()) {
                double newTotal = Double.parseDouble(valStr);
                totalBudget = newTotal;
                dbHelper.updateTotalBudget(newTotal);
                updateTotalBudgetText();
                renderCategories();
            }
        });

        builder.setNegativeButton("Reset", (dialog, which) -> {
            totalBudget = 0;
            dbHelper.deleteTotalBudget();
            updateTotalBudgetText();
            renderCategories();
            biggestExpenseText.setText("Biggest Expense");
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showExpenseInputDialog(String categoryName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter Expense for " + categoryName);

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String valueStr = input.getText().toString().trim();
            if (!valueStr.isEmpty()) {
                double value = Double.parseDouble(valueStr);

                double current = dbHelper.getTotalBudget();
                if (current <= 0 || value > current) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Insufficient Budget")
                            .setMessage("Please add a total budget or enter a smaller amount.")
                            .setPositiveButton("OK", (d, w) -> d.dismiss())
                            .show();
                    return;
                }

                // Let DB helper subtract from total and insert the expense atomically
                dbHelper.addExpense(categoryName, value);

                // Refresh in-memory total and UI
                totalBudget = dbHelper.getTotalBudget();
                updateTotalBudgetText();

                renderCategories();
                updateBiggestExpense();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showHistoryDialog(String categoryName) {
        List<Map<String, Object>> history = dbHelper.getAllData(categoryName);
        if (history.isEmpty()) {
            new AlertDialog.Builder(getContext())
                    .setTitle("No Data")
                    .setMessage("No entries for " + categoryName)
                    .setPositiveButton("OK", (d, w) -> d.dismiss())
                    .show();
            return;
        }

        ListView listView = new ListView(getContext());
        List<String> displayList = new ArrayList<>();
        for (Map<String, Object> entry : history) {
            displayList.add(String.format(Locale.getDefault(), "%.2f - %s", entry.get("amount"), entry.get("date")));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            int entryId = (int) history.get(position).get("id");
            showEditDeleteDialog(categoryName, entryId, adapter, position, history);
        });

        new AlertDialog.Builder(getContext())
                .setTitle(categoryName + " History")
                .setView(listView)
                .setPositiveButton("Close", (d, w) -> d.dismiss())
                .show();
    }

    private void showEditDeleteDialog(String categoryName, int entryId,
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
                dbHelper.updateData(categoryName, entryId, newAmount);
                adapter.remove(adapter.getItem(position));
                adapter.insert(String.format(Locale.getDefault(), "%.2f - %s", newAmount, history.get(position).get("date")), position);
                adapter.notifyDataSetChanged();
                renderCategories();
                updateBiggestExpense();
            }
        });

        builder.setNegativeButton("Delete", (dialog, which) -> {
            dbHelper.deleteData(categoryName, entryId);
            // Refresh total budget from DB and update UI
            totalBudget = dbHelper.getTotalBudget();
            updateTotalBudgetText();

            adapter.remove(adapter.getItem(position));
            adapter.notifyDataSetChanged();
            renderCategories();
            updateBiggestExpense();
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateBiggestExpense() {
        double max = 0;
        String maxCategory = "";
        List<Map<String, Object>> categories = dbHelper.getCategories();
        for (Map<String, Object> cat : categories) {
            String name = (String) cat.get("name");
            double val = dbHelper.getAmount(name);
            if (val > max) {
                max = val;
                maxCategory = name;
            }
        }
        if (max > 0) {
            biggestExpenseText.setText(String.format(Locale.getDefault(), "%s: %s", maxCategory, formatCurrency(max)));
        } else {
            biggestExpenseText.setText("Biggest Expense");
        }
    }

    private void showCategoryMenu(View anchor, String categoryName) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(getString(R.string.action_delete));
        popup.setOnMenuItemClickListener(item -> {
            if ("Delete".contentEquals(item.getTitle())) {
                confirmDeleteCategory(categoryName);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private String formatCurrency(double amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        try {
            nf.setCurrency(Currency.getInstance("ETB")); // Ethiopian Birr
        } catch (Exception ignored) {}
        return nf.format(amount);
    }

    private String formatAmountAndPercent(double spent, double goal) {
        double percent;
        if (goal > 0) {
            percent = (spent / goal) * 100.0;
        } else if (totalBudget > 0) {
            percent = (spent / (totalBudget + spent)) * 100.0;
        } else {
            percent = 0.0;
        }
        return String.format(Locale.getDefault(), "%s (%.1f%%)", formatCurrency(spent), percent);
    }

    private void updateTotalBudgetText() {
        if (isHidden) {
            totalBudgetText.setText("****");
        } else {
            totalBudgetText.setText(String.format(Locale.getDefault(), "%.2f", totalBudget));
        }
    }
}
