package spendwise.ui;

import spendwise.model.Category;
import spendwise.model.Expense;
import spendwise.service.ValidationException;
import spendwise.service.ValidationService;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class ExpenseDialog extends JDialog {
    private final JTextField amountField = new JTextField(18);
    private final JComboBox<Category> categoryBox = new JComboBox<>(Category.values());
    private final JTextField dateField = new JTextField(18);
    private final JTextField noteField = new JTextField(18);
    private Expense result;
    private final UUID editingId;

    public ExpenseDialog(java.awt.Frame owner, Expense existing) {
        super(owner, existing == null ? "Add Expense" : "Edit Expense", true);
        editingId = existing == null ? null : existing.getId();
        build(existing);
    }

    private void build(Expense existing) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        addRow(form, 0, "Amount (INR):", amountField);
        addRow(form, 1, "Category:", categoryBox);
        addRow(form, 2, "Date:", dateField);
        addRow(form, 3, "Note (optional):", noteField);
        JLabel hint = new JLabel("Expected date format: dd/MM/yyyy");
        GridBagConstraints hintC = constraints(4);
        hintC.gridwidth = 2;
        form.add(hint, hintC);
        if (existing == null) {
            dateField.setText(ValidationService.DATE_FORMAT.format(LocalDate.now()));
        } else {
            amountField.setText(existing.getAmount().toPlainString());
            categoryBox.setSelectedItem(existing.getCategory());
            dateField.setText(ValidationService.DATE_FORMAT.format(existing.getDate()));
            noteField.setText(existing.getNote());
        }
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        save.addActionListener(e -> save());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel();
        buttons.add(save);
        buttons.add(cancel);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private GridBagConstraints constraints(int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 5, 10);
        return c;
    }

    private void addRow(JPanel panel, int row, String label, java.awt.Component field) {
        GridBagConstraints labelC = constraints(row);
        panel.add(new JLabel(label), labelC);
        GridBagConstraints fieldC = constraints(row);
        fieldC.gridx = 1; fieldC.fill = GridBagConstraints.HORIZONTAL; fieldC.weightx = 1;
        panel.add(field, fieldC);
    }

    private void save() {
        try {
            java.math.BigDecimal amount = ValidationService.parseAmount(amountField.getText(), "Amount");
            LocalDate date = ValidationService.parseExpenseDate(dateField.getText());
            Category category = (Category) categoryBox.getSelectedItem();
            result = new Expense(editingId == null ? UUID.randomUUID() : editingId,
                    amount, category, date, noteField.getText().trim());
            dispose();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Please correct the form",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public Expense getResult() {
        return result;
    }
}
