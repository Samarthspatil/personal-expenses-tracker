package spendwise.ui;

import spendwise.model.Budget;
import spendwise.model.Expense;
import java.time.YearMonth;
import java.util.List;

public interface ReportExporter {
    String export(YearMonth month, Budget budget, List<Expense> expenses);
    String getFileDescription();
}
