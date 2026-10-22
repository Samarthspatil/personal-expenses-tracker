package spendwise.ui;

import spendwise.model.Budget;
import spendwise.model.Category;
import spendwise.model.Expense;
import spendwise.service.ExpenseService;
import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;

public final class DashboardPanel extends Panel {
    private final MainFrame frame; private final Choice month = new Choice(); private final Label budget = new Label();
    private final Label spent = new Label(); private final Label remaining = new Label(); private final Label count = new Label();
    private final TextArea categories = new TextArea("", 8, 30, TextArea.SCROLLBARS_VERTICAL_ONLY);
    private final List recent = new List(5); private final Label insight = new Label();
    public DashboardPanel(MainFrame frame) {
        this.frame = frame; setLayout(new BorderLayout(8, 8)); Panel top = new Panel(new BorderLayout()); top.add(new Label("Dashboard"), BorderLayout.WEST); top.add(month, BorderLayout.EAST); add(top, BorderLayout.NORTH);
        Panel summaries = new Panel(new GridLayout(1, 4, 6, 6)); summaries.add(budget); summaries.add(spent); summaries.add(remaining); summaries.add(count); add(summaries, BorderLayout.CENTER);
        Panel lower = new Panel(new GridLayout(1, 3, 6, 6)); lower.add(categories); lower.add(recent); lower.add(insight); add(lower, BorderLayout.SOUTH);
        month.addItemListener(new java.awt.event.ItemListener() { public void itemStateChanged(java.awt.event.ItemEvent e) { refresh(); }}); refreshMonths();
    }
    public void refreshMonths() {
        String chosen = month.getSelectedItem(); month.removeAll(); ArrayList<YearMonth> values = new ArrayList<YearMonth>();
        for (Expense e : frame.getExpenses()) if (!values.contains(e.getMonth())) values.add(e.getMonth());
        for (Budget b : frame.getBudgets()) if (!values.contains(b.getMonth())) values.add(b.getMonth());
        if (values.isEmpty()) month.add(YearMonth.now().toString()); else for (YearMonth value : values) month.add(value.toString());
        if (chosen != null) month.select(chosen); refresh();
    }
    private YearMonth selected() { return YearMonth.parse(month.getSelectedItem()); }
    public void refresh() {
        YearMonth selected = selected(); ArrayList<Expense> all = new ArrayList<Expense>(); for (Expense e : frame.getExpenses()) if (e.getMonth().equals(selected)) all.add(e);
        ExpenseService service = new ExpenseService(); java.math.BigDecimal total = service.calculateTotal(frame.getExpenses(), selected); Budget b = frame.findBudget(selected);
        budget.setText("Monthly budget: " + (b == null ? "Budget not set" : UiUtil.money(b.getAmount())));
        spent.setText("Total spent: " + UiUtil.money(total)); count.setText("Expenses: " + all.size());
        remaining.setText(b == null ? "Remaining: -" : (b.getAmount().compareTo(total) >= 0 ? "Remaining: " + UiUtil.money(b.getAmount().subtract(total)) : "Over budget by " + UiUtil.money(total.subtract(b.getAmount()))));
        StringBuilder categoryText = new StringBuilder("Category totals\n");
        Category highest = null; java.math.BigDecimal highestValue = java.math.BigDecimal.ZERO;
        for (Category c : Category.values()) {
            java.math.BigDecimal value = service.calculateTotal(frame.getExpenses(), selected, c);
            if (value.signum() > 0) categoryText.append(c).append(": ").append(UiUtil.money(value)).append("\n");
            if (value.compareTo(highestValue) > 0) { highest = c; highestValue = value; }
        }
        categories.setText(categoryText.toString());
        all.sort(new Comparator<Expense>() {
            public int compare(Expense first, Expense second) { return second.getDate().compareTo(first.getDate()); }
        });
        recent.removeAll(); int shown = 0;
        for (Expense e : all) { if (shown++ == 5) break; recent.add(e.getDate() + " | " + e.getCategory() + " | " + UiUtil.money(e.getAmount())); }
        java.math.BigDecimal previous = service.calculateTotal(frame.getExpenses(), selected.minusMonths(1));
        String comparison = previous.signum() == 0 ? "No previous spending baseline"
                : "Difference from previous month: " + UiUtil.money(total.subtract(previous).abs());
        insight.setText("Insights: " + (all.isEmpty() ? "No expenses yet" : "Highest category: " + highest
                + " | " + comparison + " | Recorded totals so far"));
    }
}
