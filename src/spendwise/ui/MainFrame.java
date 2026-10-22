package spendwise.ui;

import spendwise.model.Budget;
import spendwise.model.Expense;
import spendwise.storage.BudgetRepository;
import spendwise.storage.ExpenseRepository;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

public final class MainFrame extends JFrame {
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final List<Expense> expenses = new ArrayList<>();
    private final List<Budget> budgets = new ArrayList<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final DashboardPanel dashboardPanel;
    private final ExpensesPanel expensesPanel;
    private final BudgetPanel budgetPanel;
    private final ReportsPanel reportsPanel;
    private final List<JButton> navigationButtons = new ArrayList<>();
    private Expense lastDeleted;
    private boolean saving;

    public MainFrame(ExpenseRepository expenseRepository, BudgetRepository budgetRepository) {
        super("SpendWise — Personal Expense Tracker");
        this.expenseRepository = expenseRepository; this.budgetRepository = budgetRepository;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600)); setSize(1100, 700); setLocationRelativeTo(null);
        dashboardPanel = new DashboardPanel(this);
        expensesPanel = new ExpensesPanel(this);
        budgetPanel = new BudgetPanel(this);
        reportsPanel = new ReportsPanel(this);
        cardPanel.add(dashboardPanel, "Dashboard"); cardPanel.add(expensesPanel, "Expenses");
        cardPanel.add(budgetPanel, "Budgets"); cardPanel.add(reportsPanel, "Reports");
        add(createNavigation(), BorderLayout.WEST); add(cardPanel, BorderLayout.CENTER);
        setJMenuBar(createMenuBar());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (saving) JOptionPane.showMessageDialog(MainFrame.this, "Please wait for saving to finish.");
                else dispose();
            }
        });
        loadData();
    }

    private JPanel createNavigation() {
        JPanel nav = new JPanel(new GridLayout(5, 1, 4, 4));
        nav.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12)); nav.setBackground(UiUtil.NAVY);
        nav.setPreferredSize(new Dimension(170, 0));
        JLabel title = new JLabel("<html><font color='white'><b>SpendWise</b><br>Expense tracker</font></html>");
        nav.add(title);
        for (String name : new String[]{"Dashboard", "Expenses", "Budgets", "Reports"}) {
            JButton button = new JButton(name); button.setFocusPainted(false);
            button.addActionListener(e -> {
                cards.show(cardPanel, name);
                for (JButton other : navigationButtons) other.setBackground(null);
                button.setBackground(UiUtil.TEAL);
            });
            navigationButtons.add(button); nav.add(button);
        }
        return nav;
    }

    private javax.swing.JMenuBar createMenuBar() {
        javax.swing.JMenuBar bar = new javax.swing.JMenuBar();
        javax.swing.JMenu file = new javax.swing.JMenu("File");
        javax.swing.JMenuItem backup = new javax.swing.JMenuItem("Backup data...");
        javax.swing.JMenuItem restore = new javax.swing.JMenuItem("Restore data...");
        backup.addActionListener(e -> chooseBackup());
        restore.addActionListener(e -> chooseRestore());
        file.add(backup); file.add(restore); bar.add(file);
        return bar;
    }

    private void loadData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                expenses.addAll(expenseRepository.load()); budgets.addAll(budgetRepository.load()); return null;
            }
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try { get(); refreshAll(); }
                catch (Exception ex) { showError("Could not load saved data. Original files were not changed.", ex); }
            }
        }.execute();
    }

    public List<Expense> getExpenses() { return new ArrayList<>(expenses); }
    public List<Budget> getBudgets() { return new ArrayList<>(budgets); }
    public Budget findBudget(YearMonth month) {
        return budgets.stream().filter(b -> b.getMonth().equals(month)).findFirst().orElse(null);
    }

    public void addExpense(Expense expense) { expenses.add(expense); saveAll("Expense added."); }
    public void updateExpense(Expense updated) {
        for (int i = 0; i < expenses.size(); i++) if (expenses.get(i).getId().equals(updated.getId())) { expenses.set(i, updated); break; }
        saveAll("Expense updated.");
    }
    public void deleteExpense(UUID id) {
        for (Expense expense : expenses) {
            if (expense.getId().equals(id)) { lastDeleted = expense; break; }
        }
        expenses.removeIf(e -> e.getId().equals(id)); saveAll("Expense deleted.");
    }
    public void undoLastDelete() {
        if (lastDeleted == null) {
            JOptionPane.showMessageDialog(this, "There is no deletion to undo.");
            return;
        }
        boolean duplicate = false;
        for (Expense expense : expenses) if (expense.getId().equals(lastDeleted.getId())) duplicate = true;
        if (!duplicate) { expenses.add(lastDeleted); saveAll("Last deletion undone."); lastDeleted = null; }
    }
    public void setBudget(Budget budget) {
        budgets.removeIf(b -> b.getMonth().equals(budget.getMonth())); budgets.add(budget); saveAll("Budget saved.");
    }
    public void removeBudget(YearMonth month) { budgets.removeIf(b -> b.getMonth().equals(month)); saveAll("Budget removed."); }

    private void saveAll(String successMessage) {
        if (saving) { showError("A save is already in progress. Please wait and retry.", null); return; }
        saving = true; setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        List<Expense> expenseSnapshot = getExpenses(); List<Budget> budgetSnapshot = new ArrayList<>(budgets);
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception { expenseRepository.save(expenseSnapshot); budgetRepository.save(budgetSnapshot); return null; }
            protected void done() {
                saving = false; setCursor(Cursor.getDefaultCursor());
                try { get(); refreshAll(); JOptionPane.showMessageDialog(MainFrame.this, successMessage); }
                catch (Exception ex) { showError("Save failed. Your in-memory changes remain; please retry.", ex); }
            }
        }.execute();
    }

    public void exportFiltered() {
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Export filtered expenses");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        if (Files.exists(path) && JOptionPane.showConfirmDialog(this, "Overwrite existing file?", "Confirm overwrite",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        List<Expense> snapshot = expensesPanel.getFilteredExpenses();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    writer.write("Date,Category,Note,Amount_INR\n");
                    for (Expense e : snapshot) {
                        writer.write(csv(e.getDate().toString()) + "," + csv(e.getCategory().getDisplayName()) + ","
                                + csv(e.getNote()) + "," + e.getAmount().toPlainString() + "\n");
                    }
                }
                return null;
            }
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try { get(); JOptionPane.showMessageDialog(MainFrame.this, "CSV export completed."); }
                catch (Exception ex) { showError("CSV export failed.", ex); }
            }
        }.execute();
    }
    private String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    private void chooseBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save SpendWise backup");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        if (Files.exists(path) && JOptionPane.showConfirmDialog(this, "Overwrite existing backup?",
                "Confirm overwrite", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            writeBackup(path, getExpenses(), getBudgets());
            JOptionPane.showMessageDialog(this, "Backup saved successfully.");
        } catch (IOException ex) {
            showError("Backup failed.", ex);
        }
    }

    private void chooseRestore() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open SpendWise backup");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        List<Expense> originalExpenses = getExpenses();
        List<Budget> originalBudgets = getBudgets();
        try {
            Properties backup = new Properties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { backup.load(reader); }
            List<Expense> restoredExpenses = readExpenses(backup);
            List<Budget> restoredBudgets = readBudgets(backup);
            if (JOptionPane.showConfirmDialog(this,
                    "Restore replaces current expenses and budgets. Continue?", "Confirm restore",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            Path safety = path.resolveSibling(path.getFileName() + ".before-restore");
            writeBackup(safety, originalExpenses, originalBudgets);
            expenseRepository.save(restoredExpenses);
            budgetRepository.save(restoredBudgets);
            expenses.clear(); expenses.addAll(restoredExpenses);
            budgets.clear(); budgets.addAll(restoredBudgets);
            refreshAll();
            JOptionPane.showMessageDialog(this, "Restore completed. Previous data was backed up to:\n" + safety);
        } catch (Exception ex) {
            try {
                expenseRepository.save(originalExpenses);
                budgetRepository.save(originalBudgets);
            } catch (IOException rollbackError) {
                ex.addSuppressed(rollbackError);
            }
            showError("Restore failed. Current data was restored from the in-memory snapshot.", ex);
        }
    }

    private void writeBackup(Path path, List<Expense> expenseData, List<Budget> budgetData) throws IOException {
        Properties backup = new Properties();
        backup.setProperty("format", "SpendWise backup 1");
        backup.setProperty("expense.count", Integer.toString(expenseData.size()));
        for (int i = 0; i < expenseData.size(); i++) {
            Expense expense = expenseData.get(i); String prefix = "expense." + i + ".";
            backup.setProperty(prefix + "id", expense.getId().toString());
            backup.setProperty(prefix + "amount", expense.getAmount().toPlainString());
            backup.setProperty(prefix + "category", expense.getCategory().getDisplayName());
            backup.setProperty(prefix + "date", expense.getDate().toString());
            backup.setProperty(prefix + "note", expense.getNote());
        }
        backup.setProperty("budget.count", Integer.toString(budgetData.size()));
        for (int i = 0; i < budgetData.size(); i++) {
            Budget budget = budgetData.get(i); String prefix = "budget." + i + ".";
            backup.setProperty(prefix + "month", budget.getMonth().toString());
            backup.setProperty(prefix + "amount", budget.getAmount().toPlainString());
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) { backup.store(writer, "SpendWise backup"); }
    }

    private List<Expense> readExpenses(Properties backup) throws IOException {
        List<Expense> result = new ArrayList<>();
        int count = parseCount(backup, "expense.count");
        for (int i = 0; i < count; i++) {
            String p = "expense." + i + ".";
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(required(backup, p + "amount"));
                spendwise.model.Category category = spendwise.model.Category.fromDisplayName(required(backup, p + "category"));
                java.time.LocalDate date = java.time.LocalDate.parse(required(backup, p + "date"));
                spendwise.service.ValidationService.validateExpenseValues(amount, category, date);
                result.add(new Expense(UUID.fromString(required(backup, p + "id")), amount, category, date,
                        backup.getProperty(p + "note", "")));
            } catch (Exception ex) { throw new IOException("Invalid backup expense " + i, ex); }
        }
        return result;
    }

    private List<Budget> readBudgets(Properties backup) throws IOException {
        List<Budget> result = new ArrayList<>();
        int count = parseCount(backup, "budget.count");
        for (int i = 0; i < count; i++) {
            String p = "budget." + i + ".";
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(required(backup, p + "amount"));
                spendwise.service.ValidationService.validateBudget(amount);
                result.add(new Budget(YearMonth.parse(required(backup, p + "month")), amount));
            } catch (Exception ex) { throw new IOException("Invalid backup budget " + i, ex); }
        }
        return result;
    }

    private int parseCount(Properties properties, String key) throws IOException {
        try { int count = Integer.parseInt(required(properties, key)); if (count < 0) throw new NumberFormatException(); return count; }
        catch (NumberFormatException ex) { throw new IOException("Invalid backup count: " + key, ex); }
    }

    private String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) throw new IOException("Missing backup property: " + key);
        return value;
    }

    private void refreshAll() {
        expensesPanel.refreshFilters();         dashboardPanel.refreshMonths(); budgetPanel.refresh(); reportsPanel.refreshMonths();
    }
    private void showError(String message, Exception ex) {
        String detail = ex == null ? "" : "\n" + (ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage());
        JOptionPane.showMessageDialog(this, message + detail, "SpendWise", JOptionPane.ERROR_MESSAGE);
    }
}
