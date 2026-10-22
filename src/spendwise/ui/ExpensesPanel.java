package spendwise.ui;

import spendwise.model.Category;
import spendwise.model.Expense;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.JTextField;
import javax.swing.table.TableRowSorter;

public final class ExpensesPanel extends JPanel {
    private final MainFrame frame;
    private final ExpenseTableModel tableModel = new ExpenseTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<ExpenseTableModel> sorter = new TableRowSorter<>(tableModel);
    private final JComboBox<String> monthBox = new JComboBox<>();
    private final JComboBox<String> categoryBox = new JComboBox<>();
    private final JTextField searchField = new JTextField(14);
    private final JLabel totalLabel = new JLabel();
    private final JLabel emptyLabel = new JLabel("No matching expenses. Add an expense or change the filters.");

    public ExpensesPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JPanel top = new JPanel(new BorderLayout());
        top.add(UiUtil.heading("Expenses"), BorderLayout.WEST);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filters.add(new JLabel("Search note:")); filters.add(searchField);
        filters.add(new JLabel("Month:")); filters.add(monthBox);
        filters.add(new JLabel("Category:")); filters.add(categoryBox);
        JButton clear = new JButton("Clear filters");
        clear.addActionListener(e -> { searchField.setText(""); monthBox.setSelectedIndex(0); categoryBox.setSelectedIndex(0); });
        filters.add(clear);
        top.add(filters, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel tableArea = new JPanel(new BorderLayout());
        tableArea.add(new JScrollPane(table), BorderLayout.CENTER);
        tableArea.add(emptyLabel, BorderLayout.SOUTH);
        add(tableArea, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Add"); JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete"); JButton undo = new JButton("Undo last delete");
        JButton export = new JButton("Export CSV");
        UiUtil.stylePrimary(add); UiUtil.stylePrimary(edit); UiUtil.styleDanger(delete);
        add.addActionListener(e -> addExpense()); edit.addActionListener(e -> editExpense());
        delete.addActionListener(e -> deleteExpense()); undo.addActionListener(e -> frame.undoLastDelete());
        export.addActionListener(e -> frame.exportFiltered());
        buttons.add(add); buttons.add(edit); buttons.add(delete); buttons.add(undo); buttons.add(export);
        bottom.add(buttons, BorderLayout.WEST); bottom.add(totalLabel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        monthBox.addActionListener(e -> refresh());
        categoryBox.addActionListener(e -> refresh());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });
        emptyLabel.setHorizontalAlignment(JLabel.CENTER);
        refreshFilters();
    }

    public void refreshFilters() {
        String selectedMonth = (String) monthBox.getSelectedItem();
        String selectedCategory = (String) categoryBox.getSelectedItem();
        monthBox.removeAllItems(); monthBox.addItem("All Months");
        List<YearMonth> months = new ArrayList<>();
        for (Expense expense : frame.getExpenses()) if (!months.contains(expense.getMonth())) months.add(expense.getMonth());
        months.sort(YearMonth::compareTo);
        for (YearMonth month : months) monthBox.addItem(month.toString());
        if (selectedMonth != null) monthBox.setSelectedItem(selectedMonth);
        categoryBox.removeAllItems(); categoryBox.addItem("All Categories");
        for (Category category : Category.values()) categoryBox.addItem(category.getDisplayName());
        if (selectedCategory != null) categoryBox.setSelectedItem(selectedCategory);
        refresh();
    }

    private List<Expense> filtered() {
        List<Expense> result = new ArrayList<>();
        String month = (String) monthBox.getSelectedItem();
        String category = (String) categoryBox.getSelectedItem();
        String search = searchField.getText().trim().toLowerCase();
        for (Expense expense : frame.getExpenses()) {
            boolean monthMatches = month == null || month.equals("All Months") || expense.getMonth().toString().equals(month);
            boolean categoryMatches = category == null || category.equals("All Categories")
                    || expense.getCategory().getDisplayName().equals(category);
            boolean noteMatches = search.isEmpty() || expense.getNote().toLowerCase().contains(search);
            if (monthMatches && categoryMatches && noteMatches) result.add(expense);
        }
        return result;
    }

    public List<Expense> getFilteredExpenses() { return filtered(); }

    private void refresh() {
        List<Expense> data = filtered();
        tableModel.setExpenses(data);
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : data) total = total.add(expense.getAmount());
        totalLabel.setText("Visible: " + data.size() + " | Total: " + UiUtil.money(total));
        emptyLabel.setVisible(data.isEmpty());
    }

    private Expense selectedExpense() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        return tableModel.getExpenseAt(table.convertRowIndexToModel(viewRow));
    }

    private void addExpense() {
        ExpenseDialog dialog = new ExpenseDialog(frame, null); dialog.setVisible(true);
        if (dialog.getResult() != null) frame.addExpense(dialog.getResult());
    }
    private void editExpense() {
        Expense selected = selectedExpense();
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select an expense first."); return; }
        ExpenseDialog dialog = new ExpenseDialog(frame, selected); dialog.setVisible(true);
        if (dialog.getResult() != null) frame.updateExpense(dialog.getResult());
    }
    private void deleteExpense() {
        Expense selected = selectedExpense();
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select an expense first."); return; }
        int answer = JOptionPane.showConfirmDialog(this, "Delete this expense?", "Confirm deletion",
                JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) frame.deleteExpense(selected.getId());
    }
}
