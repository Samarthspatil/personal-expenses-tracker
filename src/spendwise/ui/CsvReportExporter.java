package spendwise.ui;

import spendwise.model.Budget;
import spendwise.model.Expense;
import java.time.YearMonth;
import java.util.List;

public final class CsvReportExporter implements ReportExporter {
    public String export(YearMonth month, Budget budget, List<Expense> expenses) {
        StringBuilder result = new StringBuilder("Date,Category,Note,Amount_INR\n");
        for (Expense expense : expenses) {
            result.append(csv(expense.getDate().toString())).append(",")
                    .append(csv(expense.getCategory().getDisplayName())).append(",")
                    .append(csv(expense.getNote())).append(",")
                    .append(UiUtil.plainMoney(expense.getAmount())).append("\n");
        }
        return result.toString();
    }

    private String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public String getFileDescription() { return "CSV report (*.csv)"; }
}
