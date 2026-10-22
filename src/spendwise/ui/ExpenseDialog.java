package spendwise.ui;

import spendwise.model.Category;
import spendwise.model.Expense;
import spendwise.service.ValidationException;
import spendwise.service.ValidationService;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.UUID;
import spendwise.model.PaymentMethod;

public final class ExpenseDialog extends Dialog {
    private final TextField amount = new TextField();
    private final Choice category = new Choice();
    private final TextField day = new TextField();
    private final TextField month = new TextField();
    private final TextField year = new TextField();
    private final TextArea note = new TextArea("", 4, 28, TextArea.SCROLLBARS_VERTICAL_ONLY);
    private final Checkbox essential = new Checkbox("Essential expense");
    private final CheckboxGroup paymentGroup = new CheckboxGroup();
    private final Checkbox cash = new Checkbox("Cash", paymentGroup, false);
    private final Checkbox upi = new Checkbox("UPI", paymentGroup, false);
    private final Checkbox card = new Checkbox("Card", paymentGroup, false);
    private final Label error = new Label(" ");
    private final UUID editingId;
    private Expense result;

    public ExpenseDialog(Frame owner, Expense existing) {
        super(owner, existing == null ? "Add Expense" : "Edit Expense", true);
        editingId = existing == null ? null : existing.getId();
        setLayout(new BorderLayout(8, 8));
        Panel fields = new Panel(new GridLayout(8, 2, 6, 6));
        fields.add(new Label("Amount in INR:")); fields.add(amount);
        fields.add(new Label("Category:"));
        for (Category value : Category.values()) category.add(value.getDisplayName());
        fields.add(category);
        fields.add(new Label("Day:")); fields.add(day);
        fields.add(new Label("Month:")); fields.add(month);
        fields.add(new Label("Year:")); fields.add(year);
        fields.add(new Label("Note:")); fields.add(note);
        fields.add(new Label("")); fields.add(essential);
        Panel payments = new Panel(new FlowLayout(FlowLayout.LEFT));
        payments.add(cash); payments.add(upi); payments.add(card);
        fields.add(new Label("Payment method:")); fields.add(payments);
        add(fields, BorderLayout.CENTER);
        error.setForeground(java.awt.Color.RED); add(error, BorderLayout.NORTH);
        Panel buttons = new Panel(new FlowLayout()); Button save = new Button("Save"); Button cancel = new Button("Cancel");
        save.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { save(); }});
        cancel.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }});
        buttons.add(save); buttons.add(cancel);
        add(buttons, BorderLayout.SOUTH);
        LocalDate date = existing == null ? LocalDate.now() : existing.getDate();
        amount.setText(existing == null ? "" : existing.getAmount().toPlainString());
        category.select(existing == null ? Category.OTHER.getDisplayName() : existing.getCategory().getDisplayName());
        if (existing == null || existing.getPaymentMethod() == PaymentMethod.UNSPECIFIED) {
            cash.setState(false); upi.setState(false); card.setState(false);
        } else if (existing.getPaymentMethod() == PaymentMethod.CASH) cash.setState(true);
        else if (existing.getPaymentMethod() == PaymentMethod.UPI) upi.setState(true);
        else card.setState(true);
        if (existing != null) essential.setState(existing.isEssential());
        day.setText(Integer.toString(date.getDayOfMonth())); month.setText(Integer.toString(date.getMonthValue()));
        year.setText(Integer.toString(date.getYear())); if (existing != null) note.setText(existing.getNote());
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { dispose(); }});
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ESCAPE) dispose();
            }
        });
        amount.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ESCAPE) dispose();
            }
        });
        day.addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent event) { if (event.getKeyCode() == KeyEvent.VK_ESCAPE) dispose(); }});
        month.addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent event) { if (event.getKeyCode() == KeyEvent.VK_ESCAPE) dispose(); }});
        year.addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent event) { if (event.getKeyCode() == KeyEvent.VK_ESCAPE) dispose(); }});
        note.addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent event) { if (event.getKeyCode() == KeyEvent.VK_ESCAPE) dispose(); }});
        setSize(430, 350); setLocationRelativeTo(owner);
    }
    private void save() {
        try {
            java.math.BigDecimal value = ValidationService.parseAmount(amount.getText(), "Amount");
            LocalDate date = LocalDate.of(Integer.parseInt(year.getText().trim()), Integer.parseInt(month.getText().trim()),
                    Integer.parseInt(day.getText().trim()));
            if (date.isAfter(LocalDate.now())) throw new ValidationException("Date cannot be in the future.");
            Category selected = Category.fromDisplayName(category.getSelectedItem());
            PaymentMethod method = PaymentMethod.UNSPECIFIED;
            if (cash.getState()) method = PaymentMethod.CASH;
            else if (upi.getState()) method = PaymentMethod.UPI;
            else if (card.getState()) method = PaymentMethod.CARD;
            result = new Expense(editingId == null ? UUID.randomUUID() : editingId, value, selected, date,
                    note.getText().trim(), essential.getState(), method);
            dispose();
        } catch (Exception exception) { error.setText(exception.getMessage() == null ? "Invalid form values." : exception.getMessage()); }
    }
    public Expense getResult() { return result; }
}
