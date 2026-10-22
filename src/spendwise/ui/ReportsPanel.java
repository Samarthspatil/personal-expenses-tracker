package spendwise.ui;

import spendwise.model.Budget;
import spendwise.model.Expense;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.TextArea;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;

public final class ReportsPanel extends Panel {
    private final MainFrame frame; private final Choice month = new Choice(); private final TextArea preview = new TextArea();
    public ReportsPanel(MainFrame frame) {
        this.frame = frame; setLayout(new BorderLayout(8, 8)); Panel top = new Panel(new FlowLayout(FlowLayout.LEFT)); top.add(new java.awt.Label("Report month:")); top.add(month);
        Button text = new Button("Save Text Report"); Button csv = new Button("Export CSV"); top.add(text); top.add(csv); add(top, BorderLayout.NORTH); preview.setEditable(false); add(preview, BorderLayout.CENTER);
        month.addItemListener(new java.awt.event.ItemListener() { public void itemStateChanged(java.awt.event.ItemEvent e) { refresh(); }});
        text.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { save(new TextReportExporter(), ".txt"); }});
        csv.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { save(new CsvReportExporter(), ".csv"); }}); refreshMonths();
    }
    public void refreshMonths() { String chosen = month.getSelectedItem(); month.removeAll(); ArrayList<YearMonth> values = new ArrayList<YearMonth>(); for (Expense e : frame.getExpenses()) if (!values.contains(e.getMonth())) values.add(e.getMonth()); for (Budget b : frame.getBudgets()) if (!values.contains(b.getMonth())) values.add(b.getMonth()); if (values.isEmpty()) month.add(YearMonth.now().toString()); else for (YearMonth value : values) month.add(value.toString()); if (chosen != null) month.select(chosen); refresh(); }
    private ArrayList<Expense> expenses() { ArrayList<Expense> result = new ArrayList<Expense>(); YearMonth selected = YearMonth.parse(month.getSelectedItem()); for (Expense e : frame.getExpenses()) if (e.getMonth().equals(selected)) result.add(e); return result; }
    public void refresh() { if (month.getItemCount() > 0) preview.setText(new TextReportExporter().export(YearMonth.parse(month.getSelectedItem()), frame.findBudget(YearMonth.parse(month.getSelectedItem())), expenses())); }
    private void save(ReportExporter exporter, String extension) { FileDialog dialog = new FileDialog(frame, "Save report", FileDialog.SAVE); dialog.setFile("spendwise-report" + extension); dialog.setVisible(true); if (dialog.getFile() == null) return; Path path = Path.of(dialog.getDirectory(), dialog.getFile()); if (Files.exists(path) && !Dialogs.confirm(frame, "Overwrite existing file?", "Confirm overwrite")) return; try { YearMonth selected = YearMonth.parse(month.getSelectedItem()); Files.writeString(path, exporter.export(selected, frame.findBudget(selected), expenses()), StandardCharsets.UTF_8); Dialogs.message(frame, "Report saved.", "Success"); } catch (IOException exception) { Dialogs.error(frame, "Report save failed.", exception); } }
}
