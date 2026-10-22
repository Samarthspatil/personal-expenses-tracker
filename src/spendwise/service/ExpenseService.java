package spendwise.service;

import spendwise.model.Category;
import spendwise.model.Expense;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ExpenseService {
    public BigDecimal calculateTotal(List<Expense> expenses, YearMonth month) {
        return expenses.stream().filter(e -> e.getMonth().equals(month))
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotal(List<Expense> expenses, YearMonth month, Category category) {
        return expenses.stream().filter(e -> e.getMonth().equals(month) && e.getCategory() == category)
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<Category, BigDecimal> categoryTotals(List<Expense> expenses, YearMonth month) {
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            totals.put(category, calculateTotal(expenses, month, category));
        }
        return totals;
    }

    public List<Expense> forMonth(List<Expense> expenses, YearMonth month) {
        return expenses.stream().filter(e -> e.getMonth().equals(month)).collect(Collectors.toCollection(ArrayList::new));
    }
}
