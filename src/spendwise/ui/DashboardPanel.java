package spendwise.ui;

import spendwise.model.Budget;
import spendwise.model.Category;
import spendwise.model.Expense;
import spendwise.service.BudgetService;
import spendwise.service.ExpenseService;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public final class DashboardPanel extends JPanel {
    private final MainFrame frame;
    private final JComboBox<YearMonth> monthBox = new JComboBox<>();
    private final JLabel summary = new JLabel();
    private final JLabel insights = new JLabel();
    private final JTable categories = new JTable();
    private final JLabel recent = new JLabel();

    public DashboardPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JPanel top = new JPanel(new BorderLayout());
        top.add(UiUtil.heading("Dashboard"), BorderLayout.WEST);
        top.add(monthBox, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(2, 1, 8, 8));
        center.add(summary); center.add(new JScrollPane(categories));
        add(center, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.add(recent); bottom.add(insights); add(bottom, BorderLayout.SOUTH);
        monthBox.addActionListener(e -> refresh());
        refreshMonths();
    }

    public void refreshMonths() {
        YearMonth chosen = (YearMonth) monthBox.getSelectedItem();
        monthBox.removeAllItems();
        for (Expense expense : frame.getExpenses()) if (monthBox.getItemCount() == 0 || !contains(expense.getMonth())) monthBox.addItem(expense.getMonth());
        for (Budget budget : frame.getBudgets()) if (!contains(budget.getMonth())) monthBox.addItem(budget.getMonth());
        if (monthBox.getItemCount() == 0) monthBox.addItem(YearMonth.now());
        if (chosen != null) monthBox.setSelectedItem(chosen);
        refresh();
    }
    private boolean contains(YearMonth value) {
        for (int i = 0; i < monthBox.getItemCount(); i++) if (value.equals(monthBox.getItemAt(i))) return true;
        return false;
    }
    public YearMonth selectedMonth() { return (YearMonth) monthBox.getSelectedItem(); }

    public void refresh() {
        YearMonth month = selectedMonth(); if (month == null) return;
        ExpenseService expenses = new ExpenseService();
        BigDecimal spent = expenses.calculateTotal(frame.getExpenses(), month);
        Budget budget = frame.findBudget(month);
        String budgetText = budget == null ? "Budget not set" : UiUtil.money(budget.getAmount());
        String balance = budget == null ? "" : budget.getAmount().subtract(spent).signum() >= 0
                ? "Remaining " + UiUtil.money(budget.getAmount().subtract(spent))
                : "Over budget by " + UiUtil.money(spent.subtract(budget.getAmount()));
        Map<Category, BigDecimal> totals = expenses.categoryTotals(frame.getExpenses(), month);
        Category largest = totals.entrySet().stream().max(Map.Entry.comparingByValue()).filter(e -> e.getValue().signum() > 0)
                .map(Map.Entry::getKey).orElse(null);
        summary.setText("<html><b>Total spending:</b> " + UiUtil.money(spent) + " &nbsp; <b>Budget:</b> " + budgetText
                + " &nbsp; <b>" + balance + "</b><br><b>Largest category:</b> "
                + (largest == null ? "No expenses yet" : largest.getDisplayName()) + "</html>");
        Object[][] rows = totals.entrySet().stream().filter(e -> e.getValue().signum() > 0)
                .map(e -> new Object[]{e.getKey().getDisplayName(), e.getValue()}).toArray(Object[][]::new);
        categories.setModel(new javax.swing.table.DefaultTableModel(rows, new Object[]{"Category", "Total"}) {
            public Class<?> getColumnClass(int column) { return column == 1 ? BigDecimal.class : String.class; }
            public boolean isCellEditable(int row, int column) { return false; }
        });
        List<Expense> latest = expenses.forMonth(frame.getExpenses(), month);
        latest.sort(Comparator.comparing(Expense::getDate).reversed());
        StringBuilder text = new StringBuilder("<html><b>Five most recent:</b> ");
        if (latest.isEmpty()) text.append("No expenses yet");
        else latest.stream().limit(5).forEach(e -> text.append(e.getDate()).append(" ").append(e.getCategory()).append(" ")
                .append(UiUtil.money(e.getAmount())).append("<br>"));
        recent.setText(text.append("</html>").toString());
        BigDecimal previous = expenses.calculateTotal(frame.getExpenses(), month.minusMonths(1));
        int activeDays = 0;
        for (Expense expense : latest) {
            boolean alreadyCounted = false;
            for (Expense other : latest) {
                if (other == expense) break;
                if (other.getDate().equals(expense.getDate())) alreadyCounted = true;
            }
            if (!alreadyCounted) activeDays++;
        }
        BigDecimal average = activeDays == 0 ? BigDecimal.ZERO
                : spent.divide(BigDecimal.valueOf(activeDays), 2, java.math.RoundingMode.HALF_UP);
        String comparison = previous.signum() == 0 ? "No previous spending baseline"
                : "Difference from " + month.minusMonths(1) + ": " + UiUtil.money(spent.subtract(previous).abs());
        insights.setText("<html><b>Insights:</b> Average on spending days: " + UiUtil.money(average)
                + " &nbsp; | &nbsp; " + comparison + " (totals recorded so far)</html>");
    }
}
