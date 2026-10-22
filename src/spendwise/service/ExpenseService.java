package spendwise.service;

import spendwise.model.Category;
import spendwise.model.Expense;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ExpenseService {
    public BigDecimal calculateTotal(List<Expense> expenses, YearMonth month) {
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : expenses) if (expense.getMonth().equals(month)) total = total.add(expense.getAmount());
        return total;
    }

    public BigDecimal calculateTotal(List<Expense> expenses, YearMonth month, Category category) {
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : expenses) if (expense.getMonth().equals(month) && expense.getCategory() == category) total = total.add(expense.getAmount());
        return total;
    }

    public Map<Category, BigDecimal> categoryTotals(List<Expense> expenses, YearMonth month) {
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            totals.put(category, calculateTotal(expenses, month, category));
        }
        return totals;
    }

    public List<Expense> forMonth(List<Expense> expenses, YearMonth month) {
        List<Expense> result = new ArrayList<Expense>();
        for (Expense expense : expenses) if (expense.getMonth().equals(month)) result.add(expense);
        return result;
    }
}
