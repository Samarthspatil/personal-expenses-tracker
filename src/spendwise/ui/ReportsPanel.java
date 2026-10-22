package spendwise.ui;

import spendwise.model.Budget;
import spendwise.model.Expense;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public final class ReportsPanel extends JPanel {
    private final MainFrame frame;
    private final JComboBox<YearMonth> monthBox = new JComboBox<>();
    private final JTextArea preview = new JTextArea();

    public ReportsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JPanel top = new JPanel(new BorderLayout());
        top.add(UiUtil.heading("Reports"), BorderLayout.WEST);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(new JLabel("Month:")); controls.add(monthBox);
        JButton text = new JButton("Save as text"); JButton csv = new JButton("Export CSV");
        controls.add(text); controls.add(csv); top.add(controls, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        preview.setEditable(false); preview.setFont(UiUtil.NORMAL);
        add(new JScrollPane(preview), BorderLayout.CENTER);
        monthBox.addActionListener(e -> refresh());
        text.addActionListener(e -> save(new TextReportExporter()));
        csv.addActionListener(e -> save(new CsvReportExporter()));
        refreshMonths();
    }

    public void refreshMonths() {
        YearMonth chosen = (YearMonth) monthBox.getSelectedItem();
        monthBox.removeAllItems();
        for (Expense expense : frame.getExpenses()) addIfMissing(expense.getMonth());
        for (Budget budget : frame.getBudgets()) addIfMissing(budget.getMonth());
        if (monthBox.getItemCount() == 0) monthBox.addItem(YearMonth.now());
        if (chosen != null) monthBox.setSelectedItem(chosen);
        refresh();
    }

    private void addIfMissing(YearMonth month) {
        for (int i = 0; i < monthBox.getItemCount(); i++) if (month.equals(monthBox.getItemAt(i))) return;
        monthBox.addItem(month);
    }

    private List<Expense> monthExpenses() {
        List<Expense> result = new ArrayList<>();
        YearMonth month = (YearMonth) monthBox.getSelectedItem();
        for (Expense expense : frame.getExpenses()) if (expense.getMonth().equals(month)) result.add(expense);
        return result;
    }

    public void refresh() {
        YearMonth month = (YearMonth) monthBox.getSelectedItem();
        if (month != null) preview.setText(new TextReportExporter().export(month, frame.findBudget(month), monthExpenses()));
    }

    private void save(ReportExporter exporter) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(exporter.getFileDescription());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        if (Files.exists(path) && JOptionPane.showConfirmDialog(this, "Overwrite existing file?",
                "Confirm overwrite", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            YearMonth month = (YearMonth) monthBox.getSelectedItem();
            Files.writeString(path, exporter.export(month, frame.findBudget(month), monthExpenses()),
                    StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Report saved successfully.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Report could not be saved: " + ex.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
