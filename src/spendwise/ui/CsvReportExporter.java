package spendwise.ui;

import spendwise.model.Budget;
import spendwise.model.Expense;
import java.time.YearMonth;
import java.util.List;

public final class CsvReportExporter implements ReportExporter {
    public static String line(Expense expense) {
        return csv(expense.getDate().toString()) + "," + csv(expense.getCategory().getDisplayName()) + ","
                + csv(expense.getNote()) + "," + UiUtil.plainMoney(expense.getAmount()) + ","
                + Boolean.toString(expense.isEssential()) + "," + csv(expense.getPaymentMethod().getDisplayName()) + "\n";
    }
    public String export(YearMonth month, Budget budget, List<Expense> expenses) {
        StringBuilder result = new StringBuilder("Date,Category,Note,Amount_INR,Essential,Payment_Method\n");
        for (Expense expense : expenses) result.append(line(expense));
        return result.toString();
    }

    private static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public String getFileDescription() { return "CSV report (*.csv)"; }
}
