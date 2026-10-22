package spendwise.ui;

import spendwise.model.Expense;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public final class ExpenseTableModel extends AbstractTableModel {
    private final String[] columns = {"Date", "Category", "Note", "Amount"};
    private List<Expense> expenses = new ArrayList<>();

    public void setExpenses(List<Expense> expenses) {
        this.expenses = new ArrayList<>(expenses);
        fireTableDataChanged();
    }
    public Expense getExpenseAt(int row) { return expenses.get(row); }
    public int getRowCount() { return expenses.size(); }
    public int getColumnCount() { return columns.length; }
    public String getColumnName(int column) { return columns[column]; }
    public Object getValueAt(int row, int column) {
        Expense e = expenses.get(row);
        return switch (column) {
            case 0 -> e.getDate();
            case 1 -> e.getCategory().getDisplayName();
            case 2 -> e.getNote();
            case 3 -> e.getAmount();
            default -> "";
        };
    }
    public Class<?> getColumnClass(int column) {
        return column == 0 ? java.time.LocalDate.class : column == 3 ? java.math.BigDecimal.class : String.class;
    }
}
