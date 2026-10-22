package spendwise.ui;

import spendwise.model.Budget;
import spendwise.model.Expense;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public final class TextReportExporter implements ReportExporter {
    public String export(YearMonth month, Budget budget, List<Expense> expenses) {
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder result = new StringBuilder("SpendWise report for ").append(month).append("\n\n");
        result.append("Expenses: ").append(expenses.size()).append("\n");
        for (Expense expense : expenses) {
            total = total.add(expense.getAmount());
            result.append(expense.getDate()).append(" | ").append(expense.getCategory())
                    .append(" | ").append(expense.getNote()).append(" | ")
                    .append(UiUtil.plainMoney(expense.getAmount())).append("\n");
        }
        result.append("\nTotal spending: ").append(UiUtil.plainMoney(total)).append("\n");
        result.append("Budget: ").append(budget == null ? "Budget not set" : UiUtil.plainMoney(budget.getAmount())).append("\n");
        if (budget != null) result.append("Remaining/over budget: ")
                .append(UiUtil.plainMoney(budget.getAmount().subtract(total))).append("\n");
        return result.toString();
    }

    public String getFileDescription() { return "Text report (*.txt)"; }
}
