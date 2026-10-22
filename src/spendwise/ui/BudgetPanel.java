package spendwise.ui;

import spendwise.model.Budget;
import spendwise.model.Expense;
import spendwise.service.BudgetService;
import spendwise.service.ValidationException;
import spendwise.service.ValidationService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.YearMonth;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

public final class BudgetPanel extends JPanel {
    private final MainFrame frame;
    private final JComboBox<Integer> yearBox = new JComboBox<>();
    private final JComboBox<Integer> monthBox = new JComboBox<>();
    private final JTextField amountField = new JTextField(12);
    private final JLabel status = new JLabel();
    private final JProgressBar progress = new JProgressBar(0, 100);

    public BudgetPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        add(UiUtil.heading("Monthly Budget"), BorderLayout.NORTH);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        int thisYear = java.time.Year.now().getValue();
        for (int year = thisYear - 5; year <= thisYear + 1; year++) yearBox.addItem(year);
        for (int month = 1; month <= 12; month++) monthBox.addItem(month);
        monthBox.setSelectedItem(java.time.LocalDate.now().getMonthValue());
        yearBox.setSelectedItem(thisYear);
        form.add(new JLabel("Month:")); form.add(monthBox); form.add(new JLabel("Year:")); form.add(yearBox);
        form.add(new JLabel("Budget (INR):")); form.add(amountField);
        JButton set = new JButton("Set / Update"); JButton remove = new JButton("Remove budget");
        set.addActionListener(e -> saveBudget()); remove.addActionListener(e -> removeBudget());
        form.add(set); form.add(remove);
        JPanel center = new JPanel(new GridLayout(3, 1, 8, 8));
        center.add(form); center.add(status); center.add(progress);
        add(center, BorderLayout.CENTER);
        monthBox.addActionListener(e -> refresh()); yearBox.addActionListener(e -> refresh());
        refresh();
    }

    private YearMonth selectedMonth() { return YearMonth.of((Integer) yearBox.getSelectedItem(), (Integer) monthBox.getSelectedItem()); }

    public void refresh() {
        YearMonth month = selectedMonth();
        Budget budget = frame.findBudget(month);
        if (budget != null) amountField.setText(budget.getAmount().toPlainString()); else amountField.setText("");
        BigDecimal spent = new BudgetService().spent(frame.getExpenses(), month);
        if (budget == null) {
            status.setText("Spent: " + UiUtil.money(spent) + " | Budget not set");
            progress.setValue(0);
        } else {
            BigDecimal percentage = new BudgetService().percentage(budget, spent);
            BigDecimal remaining = budget.getAmount().subtract(spent);
            String ending = remaining.signum() >= 0 ? "Remaining: " + UiUtil.money(remaining)
                    : "Over budget by " + UiUtil.money(remaining.abs());
            status.setText("Spent: " + UiUtil.money(spent) + " | " + ending + " | Used: " + percentage + "%");
            progress.setValue(Math.min(100, Math.max(0, percentage.intValue())));
        }
    }

    private void saveBudget() {
        try {
            BigDecimal amount = ValidationService.parseAmount(amountField.getText(), "Budget");
            frame.setBudget(new Budget(selectedMonth(), amount));
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid budget", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void removeBudget() {
        if (frame.findBudget(selectedMonth()) == null) return;
        if (JOptionPane.showConfirmDialog(this, "Remove this month's budget?", "Confirm removal",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) frame.removeBudget(selectedMonth());
    }
}
