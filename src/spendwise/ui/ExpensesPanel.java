package spendwise.ui;

import spendwise.model.Category;
import spendwise.model.Expense;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Scrollbar;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;

public final class ExpensesPanel extends Panel {
    private final MainFrame frame;
    private final List list = new List(15);
    private final TextArea details = new TextArea("", 5, 35, TextArea.SCROLLBARS_VERTICAL_ONLY);
    private final TextField search = new TextField(14);
    private final Choice month = new Choice();
    private final Choice category = new Choice();
    private final Checkbox essentialOnly = new Checkbox("Essential only");
    private final Scrollbar minimumAmount = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 10001);
    private final Label threshold = new Label("Minimum: " + UiUtil.money(BigDecimal.ZERO));
    private final Label summary = new Label();
    private final ArrayList<Expense> displayed = new ArrayList<Expense>();
    public ExpensesPanel(MainFrame frame) {
        this.frame = frame; setLayout(new BorderLayout(8, 8));
        Panel filters = new Panel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new Label("Search note:")); filters.add(search); filters.add(new Label("Month:")); filters.add(month);
        filters.add(new Label("Category:")); filters.add(category);
        filters.add(essentialOnly); filters.add(new Label("Minimum amount (INR):")); filters.add(minimumAmount); filters.add(threshold);
        Button clear = new Button("Clear Filters"); clear.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { search.setText(""); month.select(0); category.select(0); refresh(); }});
        filters.add(clear); add(filters, BorderLayout.NORTH);
        list.addItemListener(new java.awt.event.ItemListener() { public void itemStateChanged(java.awt.event.ItemEvent e) { showDetails(); }}); add(list, BorderLayout.CENTER);
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && selected() != null) edit();
            }
            public void mouseEntered(MouseEvent event) { frame.setStatus("Double-click an expense to edit it."); }
            public void mouseExited(MouseEvent event) { frame.setStatus("Ready."); }
        });
        details.setEditable(false); add(details, BorderLayout.EAST);
        Panel bottom = new Panel(new FlowLayout(FlowLayout.LEFT));
        Button add = new Button("Add"); Button edit = new Button("Edit"); Button delete = new Button("Delete");
        Button undo = new Button("Undo Last Delete"); Button export = new Button("Export CSV");
        add.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { add(); }});
        edit.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { edit(); }});
        delete.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { delete(); }});
        undo.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { frame.undoLastDelete(); }});
        export.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { frame.exportFiltered(); }});
        bottom.add(add); bottom.add(edit); bottom.add(delete); bottom.add(undo); bottom.add(export); bottom.add(summary);
        add(bottom, BorderLayout.SOUTH);
        search.addTextListener(new TextListener() { public void textValueChanged(TextEvent e) { refresh(); }});
        month.addItemListener(new ItemListener() { public void itemStateChanged(ItemEvent e) { refresh(); }});
        category.addItemListener(new ItemListener() { public void itemStateChanged(ItemEvent e) { refresh(); }});
        essentialOnly.addItemListener(new ItemListener() { public void itemStateChanged(ItemEvent e) { refresh(); }});
        minimumAmount.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent event) {
                threshold.setText("Minimum: " + UiUtil.money(BigDecimal.valueOf(minimumAmount.getValue())));
                refresh();
            }
        });
        Button resetMinimum = new Button("Reset minimum");
        resetMinimum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent event) {
                minimumAmount.setValue(0);
                threshold.setText("Minimum: " + UiUtil.money(BigDecimal.ZERO));
                refresh();
            }
        });
        filters.add(resetMinimum);
        refreshFilters();
    }
    public void refreshFilters() {
        String oldMonth = month.getSelectedItem(); String oldCategory = category.getSelectedItem();
        month.removeAll(); month.add("All Months");
        ArrayList<YearMonth> months = new ArrayList<YearMonth>();
        for (Expense expense : frame.getExpenses()) if (!months.contains(expense.getMonth())) months.add(expense.getMonth());
        for (YearMonth value : months) month.add(value.toString());
        if (oldMonth != null) month.select(oldMonth);
        category.removeAll(); category.add("All Categories"); for (Category value : Category.values()) category.add(value.getDisplayName());
        if (oldCategory != null) category.select(oldCategory); refresh();
    }
    private ArrayList<Expense> filtered() {
        ArrayList<Expense> result = new ArrayList<Expense>(); String searchText = search.getText().trim().toLowerCase();
        for (Expense expense : frame.getExpenses()) {
            boolean monthOk = month.getSelectedIndex() == 0 || expense.getMonth().toString().equals(month.getSelectedItem());
            boolean categoryOk = category.getSelectedIndex() == 0 || expense.getCategory().getDisplayName().equals(category.getSelectedItem());
            boolean essentialOk = !essentialOnly.getState() || expense.isEssential();
            boolean amountOk = expense.getAmount().compareTo(BigDecimal.valueOf(minimumAmount.getValue())) >= 0;
            if (monthOk && categoryOk && essentialOk && amountOk
                    && (searchText.length() == 0 || expense.getNote().toLowerCase().contains(searchText))) result.add(expense);
        }
        return result;
    }
    public ArrayList<Expense> getFilteredExpenses() { return filtered(); }
    public void refresh() {
        displayed.clear(); displayed.addAll(filtered()); list.removeAll();
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : displayed) { total = total.add(expense.getAmount()); list.add(expense.getDate() + " | " + expense.getCategory() + " | " + UiUtil.money(expense.getAmount()) + " | " + shortNote(expense.getNote())); }
        summary.setText("Visible: " + displayed.size() + " | Total: " + UiUtil.money(total)); details.setText("");
    }
    private String shortNote(String value) { return value.length() > 24 ? value.substring(0, 24) + "..." : value; }
    private Expense selected() { int index = list.getSelectedIndex(); return index < 0 || index >= displayed.size() ? null : displayed.get(index); }
    private void add() { ExpenseDialog dialog = new ExpenseDialog(frame, null); dialog.setVisible(true); if (dialog.getResult() != null) frame.addExpense(dialog.getResult()); }
    private void edit() { Expense value = selected(); if (value == null) { Dialogs.message(frame, "Select an expense first.", "Edit"); return; } ExpenseDialog dialog = new ExpenseDialog(frame, value); dialog.setVisible(true); if (dialog.getResult() != null) frame.updateExpense(dialog.getResult()); }
    private void delete() { Expense value = selected(); if (value == null) { Dialogs.message(frame, "Select an expense first.", "Delete"); return; } if (Dialogs.confirm(frame, "Delete the selected expense?", "Confirm deletion")) frame.deleteExpense(value.getId()); }
    private void showDetails() { Expense value = selected(); if (value != null) details.setText("Date: " + value.getDate() + "\nCategory: " + value.getCategory() + "\nAmount: " + UiUtil.money(value.getAmount()) + "\nNote:\n" + value.getNote()); }
}
