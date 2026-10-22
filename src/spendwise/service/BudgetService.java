package spendwise.service;

import spendwise.model.Budget;
import spendwise.model.Expense;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

public final class BudgetService {
    public BigDecimal spent(List<Expense> expenses, YearMonth month) {
        return new ExpenseService().calculateTotal(expenses, month);
    }

    public BigDecimal remaining(Budget budget, BigDecimal spent) {
        return budget == null ? BigDecimal.ZERO : budget.getAmount().subtract(spent);
    }

    public BigDecimal percentage(Budget budget, BigDecimal spent) {
        if (budget == null || budget.getAmount().signum() == 0) return BigDecimal.ZERO;
        return spent.multiply(BigDecimal.valueOf(100)).divide(budget.getAmount(), 2, RoundingMode.HALF_UP);
    }
}
