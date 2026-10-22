package spendwise.ui;

import spendwise.model.Budget;
import spendwise.service.BudgetService;
import spendwise.service.ValidationException;
import spendwise.service.ValidationService;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.math.BigDecimal;
import java.time.YearMonth;

public final class BudgetPanel extends Panel {
    private final MainFrame frame; private final Choice year = new Choice(); private final Choice month = new Choice();
    private final TextField amount = new TextField(12); private final Label status = new Label();
    public BudgetPanel(MainFrame frame) {
        this.frame = frame; setLayout(new BorderLayout(8, 8)); Panel form = new Panel(new FlowLayout(FlowLayout.LEFT));
        int current = java.time.Year.now().getValue(); for (int i = current - 5; i <= current + 1; i++) year.add(Integer.toString(i));
        for (int i = 1; i <= 12; i++) month.add(Integer.toString(i)); year.select(Integer.toString(current)); month.select(Integer.toString(java.time.LocalDate.now().getMonthValue()));
        form.add(new Label("Month:")); form.add(month); form.add(new Label("Year:")); form.add(year); form.add(new Label("Budget INR:")); form.add(amount);
        Button save = new Button("Set / Update"); Button remove = new Button("Remove");
        save.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { save(); }});
        remove.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { remove(); }});
        form.add(save); form.add(remove);
        add(form, BorderLayout.NORTH); add(status, BorderLayout.CENTER);
        month.addItemListener(new java.awt.event.ItemListener() { public void itemStateChanged(java.awt.event.ItemEvent e) { refresh(); }});
        year.addItemListener(new java.awt.event.ItemListener() { public void itemStateChanged(java.awt.event.ItemEvent e) { refresh(); }}); refresh();
    }
    private YearMonth selectedMonth() { return YearMonth.of(Integer.parseInt(year.getSelectedItem()), Integer.parseInt(month.getSelectedItem())); }
    public void refresh() {
        YearMonth selected = selectedMonth(); Budget budget = frame.findBudget(selected); if (budget != null) amount.setText(budget.getAmount().toPlainString());
        else amount.setText(""); BigDecimal spent = new BudgetService().spent(frame.getExpenses(), selected);
        if (budget == null) status.setText("Spent: " + UiUtil.money(spent) + " | Budget not set");
        else { BigDecimal remaining = budget.getAmount().subtract(spent); status.setText("Spent: " + UiUtil.money(spent) + " | " + (remaining.signum() >= 0 ? "Remaining: " + UiUtil.money(remaining) : "Over budget by " + UiUtil.money(remaining.abs()))); }
    }
    private void save() { try { frame.setBudget(new Budget(selectedMonth(), ValidationService.parseAmount(amount.getText(), "Budget"))); } catch (ValidationException e) { Dialogs.error(frame, e.getMessage(), null); } }
    private void remove() { if (frame.findBudget(selectedMonth()) != null && Dialogs.confirm(frame, "Remove this budget?", "Confirm removal")) frame.removeBudget(selectedMonth()); }
}
