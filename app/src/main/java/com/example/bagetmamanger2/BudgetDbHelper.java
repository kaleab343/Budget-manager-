package com.example.bagetmamanger2;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetDbHelper extends SQLiteOpenHelper {
    private static final String PREFS_NAME = "BudgetPrefs";
    private static final String LAST_RESET_MONTH = "lastResetMonth";

    private static final String DATABASE_NAME = "budget_manager.db";
    private static final int DATABASE_VERSION = 4;

    private static final String TABLE_EXPENSES = "expenses";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_MONTH = "month"; // Added month column

    private static final String TABLE_TOTAL = "total_budget";
    private static final String COLUMN_TOTAL_AMOUNT = "total_amount";

    // Categories and goals
    private static final String TABLE_CATEGORIES = "categories";
    private static final String COLUMN_CATEGORY_ID = "id";
    private static final String COLUMN_CATEGORY_NAME = "name";
    private static final String COLUMN_CATEGORY_GOAL = "goal";

    private final Context context;

    public BudgetDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createExpenses = "CREATE TABLE " + TABLE_EXPENSES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_AMOUNT + " REAL, " +
                COLUMN_DATE + " TEXT," +
                COLUMN_MONTH + " INTEGER)"; // Include month in the table
        db.execSQL(createExpenses);

        String createTotal = "CREATE TABLE " + TABLE_TOTAL + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TOTAL_AMOUNT + " REAL)";
        db.execSQL(createTotal);

        String createCategories = "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CATEGORY_NAME + " TEXT UNIQUE, " +
                COLUMN_CATEGORY_GOAL + " REAL DEFAULT 0)";
        db.execSQL(createCategories);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOTAL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOTAL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    // ✅ Add expense
    public void addExpense(String category, double amount) {
        addExpense(category, amount, getCurrentDate());
    }

    public void addExpense(String category, double amount, String date) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 1️⃣ Insert expense record
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_AMOUNT, amount);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_MONTH, getCurrentMonth()); // Store the month
        db.insert(TABLE_EXPENSES, null, values);

        // 2️⃣ Subtract amount from total budget
        double currentTotal = getTotalBudget();
        double newTotal = currentTotal - amount;
        updateTotalBudget(newTotal);

        db.close();
    }

    // ✅ Get total for a category
    public double getAmount(String category) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_AMOUNT + ") as total FROM " + TABLE_EXPENSES +
                " WHERE " + COLUMN_CATEGORY + "=?", new String[]{category});

        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
        }
        cursor.close();
        db.close();
        return total;
    }

    // ✅ Get all data for a category
    public List<Map<String, Object>> getAllData(String category) {
        List<Map<String, Object>> data = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EXPENSES + " WHERE " + COLUMN_CATEGORY + "=? ORDER BY " + COLUMN_ID + " DESC", new String[]{category});

        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> entry = new HashMap<>();
                entry.put("id", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                entry.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                entry.put("date", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                data.add(entry);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return data;
    }

    // ✅ Get all expenses
    public List<Map<String, Object>> getAllExpenses() {
        List<Map<String, Object>> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EXPENSES + " ORDER BY " + COLUMN_ID + " ASC", null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> expense = new HashMap<>();
                expense.put("id", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                expense.put("category", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                expense.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                expense.put("date", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                expenses.add(expense);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return expenses;
    }

    // ✅ Filter expenses by category
    public List<Map<String, Object>> getExpensesByCategory(String category) {
        List<Map<String, Object>> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_EXPENSES +
                        " WHERE " + COLUMN_CATEGORY + "=? ORDER BY " + COLUMN_DATE + " ASC",
                new String[]{category}
        );

        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> expense = new HashMap<>();
                expense.put("id", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                expense.put("category", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                expense.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                expense.put("date", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                expenses.add(expense);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return expenses;
    }

    // ✅ Filter by date range (optional category)
    public List<Map<String, Object>> getExpensesBetween(String category, String startDate, String endDate) {
        List<Map<String, Object>> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_EXPENSES + " WHERE " + COLUMN_DATE + ">=? AND " + COLUMN_DATE + "<=?" +
                (category != null ? (" AND " + COLUMN_CATEGORY + "=?") : "") +
                " ORDER BY " + COLUMN_DATE + " ASC";
        String[] args = category != null ? new String[]{startDate, endDate, category} : new String[]{startDate, endDate};
        Cursor cursor = db.rawQuery(sql, args);
        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> expense = new HashMap<>();
                expense.put("id", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                expense.put("category", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                expense.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                expense.put("date", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                expenses.add(expense);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return expenses;
    }

    // ✅ Update / Delete entry
    public void updateData(String category, int id, double newAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AMOUNT, newAmount);
        db.update(TABLE_EXPENSES, values, COLUMN_ID + "=? AND " + COLUMN_CATEGORY + "=?", new String[]{String.valueOf(id), category});
        db.close();
    }

    // ✅ Delete data (add deleted amount back to total automatically)
    public void deleteData(String category, int id) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Get the amount of the expense being deleted
        double deletedAmount = 0;
        try (Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_AMOUNT + " FROM " + TABLE_EXPENSES +
                        " WHERE " + COLUMN_ID + "=? AND " + COLUMN_CATEGORY + "=?",
                new String[]{String.valueOf(id), category})) {

            if (cursor.moveToFirst()) {
                deletedAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT));
            }
        }

        // Delete the expense
        db.delete(TABLE_EXPENSES, COLUMN_ID + "=? AND " + COLUMN_CATEGORY + "=?",
                new String[]{String.valueOf(id), category});

        // Add deleted amount back to total budget safely
        double currentTotal = getTotalBudget();
        double newTotal = currentTotal + deletedAmount;
        updateTotalBudget(newTotal);

        db.close();
    }

    // ✅ Total Budget methods
    public double getTotalBudget() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_TOTAL_AMOUNT + " FROM " + TABLE_TOTAL + " LIMIT 1", null);

        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_AMOUNT));
        }

        cursor.close();
        db.close();
        return total;
    }

    public void updateTotalBudget(double newTotal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TOTAL_AMOUNT, newTotal);

        // Ensure only 1 row exists
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TOTAL, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();

        if (count == 0) {
            db.insert(TABLE_TOTAL, null, values);
        } else {
            db.update(TABLE_TOTAL, values, null, null);
        }

        db.close();
    }

    public void addToTotalBudget(double amount) {
        double current = getTotalBudget();
        updateTotalBudget(current + amount);
    }

    public void deleteTotalBudget() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TOTAL, null, null);
        db.close();
    }

    // ✅ Utility
    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private int getCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.MONTH); // Month is 0-indexed
    }

    // ✅ Check and Reset Expenses
    public void checkAndResetExpenses() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int lastResetMonth = prefs.getInt(LAST_RESET_MONTH, -1); // -1 means never reset

        int currentMonth = getCurrentMonth();

        if (currentMonth != lastResetMonth) {
            // Reset all category expenses
            resetCategoryExpenses();

            // Update the last reset month
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(LAST_RESET_MONTH, currentMonth);
            editor.apply();
        }
    }

    private void resetCategoryExpenses() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, null, null);
        db.close();
    }

    // ---------- Categories CRUD ----------
    public long addCategory(String name, double goal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, name);
        values.put(COLUMN_CATEGORY_GOAL, goal);
        long id = db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return id;
    }

    public boolean deleteCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Sum the category expenses to refund using the same connection
            double sum = 0;
            try (Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_EXPENSES + " WHERE " + COLUMN_CATEGORY + "=?", new String[]{name})) {
                if (c.moveToFirst()) {
                    sum = c.getDouble(0);
                }
            }

            // Delete related expenses
            db.delete(TABLE_EXPENSES, COLUMN_CATEGORY + "=?", new String[]{name});
            // Delete the category
            int rows = db.delete(TABLE_CATEGORIES, COLUMN_CATEGORY_NAME + "=?", new String[]{name});

            // Refund the sum back to total budget within the same transaction/connection
            double current = 0;
            try (Cursor tc = db.rawQuery("SELECT " + COLUMN_TOTAL_AMOUNT + " FROM " + TABLE_TOTAL + " LIMIT 1", null)) {
                if (tc.moveToFirst()) current = tc.getDouble(0);
            }
            ContentValues v = new ContentValues();
            v.put(COLUMN_TOTAL_AMOUNT, current + sum);
            // Ensure single-row table behavior
            int updated = db.update(TABLE_TOTAL, v, null, null);
            if (updated == 0) {
                db.insert(TABLE_TOTAL, null, v);
            }

            db.setTransactionSuccessful();
            return rows > 0;
        } finally {
            try { db.endTransaction(); } catch (Exception ignored) {}
            db.close();
        }
    }

    public List<Map<String, Object>> getCategories() {
        List<Map<String, Object>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_GOAL + " FROM " + TABLE_CATEGORIES + " ORDER BY " + COLUMN_CATEGORY_NAME + " ASC", null);
        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> m = new HashMap<>();
                m.put("name", cursor.getString(0));
                m.put("goal", cursor.getDouble(1));
                list.add(m);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // Seed a standard set of categories if there are none
    public void seedDefaultCategoriesIfEmpty() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CATEGORIES, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        if (count > 0) { db.close(); return; }

        String[] defaults = new String[]{
                "Relationship",
                "Fitness",
                "Tech",
                "Social",
                "Unexpected",
                "Transport",
                "DayTravel",
                "Church"
        };
        for (String name : defaults) {
            ContentValues v = new ContentValues();
            v.put(COLUMN_CATEGORY_NAME, name);
            v.put(COLUMN_CATEGORY_GOAL, 0);
            db.insertWithOnConflict(TABLE_CATEGORIES, null, v, SQLiteDatabase.CONFLICT_IGNORE);
        }
        db.close();
    }

    public double getCategoryGoal(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + COLUMN_CATEGORY_GOAL + " FROM " + TABLE_CATEGORIES + " WHERE " + COLUMN_CATEGORY_NAME + "=? LIMIT 1", new String[]{name});
        double goal = 0;
        if (c.moveToFirst()) goal = c.getDouble(0);
        c.close();
        db.close();
        return goal;
    }

    public void updateCategoryGoal(String name, double goal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_CATEGORY_GOAL, goal);
        db.update(TABLE_CATEGORIES, v, COLUMN_CATEGORY_NAME + "=?", new String[]{name});
        db.close();
    }
}