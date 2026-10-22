package spendwise.ui;

import spendwise.model.Budget;
import spendwise.model.Expense;
import spendwise.storage.BudgetRepository;
import spendwise.storage.ExpenseRepository;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class MainFrame extends Frame {
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final List<Expense> expenses = new ArrayList<Expense>();
    private final List<Budget> budgets = new ArrayList<Budget>();
    private final CardLayout cards = new CardLayout();
    private final Panel content = new Panel(cards);
    private final Label status = new Label("Loading saved data...");
    private final DashboardPanel dashboard;
    private final ExpensesPanel expensesPanel;
    private final BudgetPanel budgetPanel;
    private final ReportsPanel reportsPanel;
    private Expense lastDeleted;
    private boolean busy;
    private boolean loadFailed;

    public MainFrame() {
        this(defaultExpenseRepository(), defaultBudgetRepository());
    }

    public MainFrame(ExpenseRepository expenseRepository, BudgetRepository budgetRepository) {
        super("SpendWise - Personal Expense Tracker");
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        setLayout(new BorderLayout(8, 8));
        setBackground(UiUtil.LIGHT);
        setSize(1000, 700);
        setMinimumSize(new java.awt.Dimension(850, 600));
        dashboard = new DashboardPanel(this);
        expensesPanel = new ExpensesPanel(this);
        budgetPanel = new BudgetPanel(this);
        reportsPanel = new ReportsPanel(this);
        ScrollPane dashboardScroll = new ScrollPane();
        dashboardScroll.add(dashboard);
        content.add(dashboardScroll, "Dashboard");
        content.add(expensesPanel, "Expenses");
        content.add(budgetPanel, "Budgets");
        content.add(reportsPanel, "Reports");
        add(createHeader(), BorderLayout.NORTH);
        add(createNavigation(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        setMenuBar(createMenuBar());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                if (busy) Dialogs.message(MainFrame.this, "Please wait until the current file operation finishes.", "Save pending");
                else dispose();
            }
        });
        loadData();
    }

    private static ExpenseRepository defaultExpenseRepository() {
        Path directory = Path.of(System.getProperty("user.home"), "SpendWiseData");
        try { return new spendwise.storage.JdbcExpenseRepository(directory); }
        catch (IOException exception) { throw new IllegalStateException("H2 database is unavailable.", exception); }
    }

    private static BudgetRepository defaultBudgetRepository() {
        Path directory = Path.of(System.getProperty("user.home"), "SpendWiseData");
        try { return new spendwise.storage.JdbcBudgetRepository(directory); }
        catch (IOException exception) { throw new IllegalStateException("H2 database is unavailable.", exception); }
    }

    private Panel createHeader() {
        Panel header = new Panel(new FlowLayout(FlowLayout.LEFT, 18, 10));
        header.setBackground(UiUtil.NAVY);
        Label title = new Label("SpendWise - Personal Expense Tracker");
        title.setForeground(Color.WHITE);
        title.setFont(UiUtil.HEADING);
        header.add(title);
        return header;
    }

    private Panel createNavigation() {
        Panel navigation = new Panel(new GridLayout(5, 1, 5, 5));
        navigation.setBackground(UiUtil.NAVY);
        navigation.setPreferredSize(new java.awt.Dimension(150, 0));
        Label logo = new Label("Navigation", Label.CENTER);
        logo.setForeground(Color.WHITE);
        navigation.add(logo);
        String[] names = {"Dashboard", "Expenses", "Budgets", "Reports"};
        for (final String name : names) {
            Button button = new Button(name);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    cards.show(content, name);
                    status.setText("Viewing " + name);
                }
            });
            navigation.add(button);
        }
        return navigation;
    }

    private MenuBar createMenuBar() {
        MenuBar bar = new MenuBar();
        Menu file = new Menu("File");
        MenuItem backup = new MenuItem("Backup data...");
        MenuItem restore = new MenuItem("Restore data...");
        MenuItem retryLoad = new MenuItem("Retry load");
        backup.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { chooseBackup(); }});
        restore.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { chooseRestore(); }});
        retryLoad.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { loadData(); }});
        file.add(backup); file.add(restore); file.add(retryLoad); bar.add(file);
        return bar;
    }

    private void loadData() {
        busy = true;
        new Thread(new Runnable() { public void run() {
            try {
                final List<Expense> loadedExpenses = expenseRepository.load();
                final List<Budget> loadedBudgets = budgetRepository.load();
                EventQueue.invokeLater(new Runnable() { public void run() {
                    expenses.addAll(loadedExpenses);
                    budgets.addAll(loadedBudgets);
                    loadFailed = false; busy = false; refreshAll(); status.setText("Saved data loaded.");
                }});
            } catch (final Exception exception) {
                EventQueue.invokeLater(new Runnable() { public void run() {
                    busy = false;
                    loadFailed = true;
                    Dialogs.error(MainFrame.this, "Could not load saved data. Existing files were not changed.", exception);
                }});
            }
        }}, "SpendWise-load").start();
    }

    public List<Expense> getExpenses() { return new ArrayList<Expense>(expenses); }
    public List<Budget> getBudgets() { return new ArrayList<Budget>(budgets); }
    public Budget findBudget(YearMonth month) {
        for (Budget budget : budgets) if (budget.getMonth().equals(month)) return budget;
        return null;
    }
    public void addExpense(final Expense expense) {
        List<Expense> proposed = getExpenses(); proposed.add(expense);
        saveAll("Expense added.", proposed, getBudgets(), new Runnable() {
            public void run() { expenses.add(expense); }
        });
    }
    public void updateExpense(Expense updated) {
        List<Expense> proposed = getExpenses();
        for (int i = 0; i < proposed.size(); i++) if (proposed.get(i).getId().equals(updated.getId())) proposed.set(i, updated);
        saveAll("Expense updated.", proposed, getBudgets(), new Runnable() {
            public void run() {
                for (int i = 0; i < expenses.size(); i++) if (expenses.get(i).getId().equals(updated.getId())) expenses.set(i, updated);
            }
        });
    }
    public void deleteExpense(UUID id) {
        final Expense deleted = findExpense(id);
        if (deleted == null) return;
        List<Expense> proposed = getExpenses();
        for (int i = proposed.size() - 1; i >= 0; i--) if (proposed.get(i).getId().equals(id)) proposed.remove(i);
        saveAll("Expense deleted.", proposed, getBudgets(), new Runnable() {
            public void run() { expenses.remove(deleted); lastDeleted = deleted; }
        });
    }
    public void undoLastDelete() {
        if (lastDeleted == null) { Dialogs.message(this, "There is no deletion to undo.", "Undo"); return; }
        for (Expense expense : expenses) if (expense.getId().equals(lastDeleted.getId())) return;
        final Expense restored = lastDeleted;
        List<Expense> proposed = getExpenses(); proposed.add(restored);
        saveAll("Last deletion undone.", proposed, getBudgets(), new Runnable() {
            public void run() { expenses.add(restored); lastDeleted = null; }
        });
    }
    public void setBudget(Budget budget) {
        List<Budget> proposed = getBudgets();
        for (int i = proposed.size() - 1; i >= 0; i--) if (proposed.get(i).getMonth().equals(budget.getMonth())) proposed.remove(i);
        proposed.add(budget); saveAll("Budget saved.", getExpenses(), proposed, new Runnable() {
            public void run() { replaceBudget(budget); }
        });
    }
    public void removeBudget(YearMonth month) {
        List<Budget> proposed = getBudgets();
        for (int i = proposed.size() - 1; i >= 0; i--) if (proposed.get(i).getMonth().equals(month)) proposed.remove(i);
        saveAll("Budget removed.", getExpenses(), proposed, new Runnable() {
            public void run() {
                for (int i = budgets.size() - 1; i >= 0; i--) if (budgets.get(i).getMonth().equals(month)) budgets.remove(i);
            }
        });
    }

    private Expense findExpense(UUID id) {
        for (Expense expense : expenses) if (expense.getId().equals(id)) return expense;
        return null;
    }

    private void replaceBudget(Budget budget) {
        for (int i = budgets.size() - 1; i >= 0; i--) if (budgets.get(i).getMonth().equals(budget.getMonth())) budgets.remove(i);
        budgets.add(budget);
    }

    private void saveAll(final String message) {
        saveAll(message, getExpenses(), getBudgets(), new Runnable() { public void run() {} });
    }

    private void saveAll(final String message, final List<Expense> expenseSnapshot,
                         final List<Budget> budgetSnapshot, final Runnable commit) {
        if (loadFailed) { Dialogs.message(this, "Saved data is unavailable. Use File > Retry load before editing.", "Load failed"); return; }
        if (busy) { Dialogs.message(this, "A file operation is already in progress. Please retry.", "Save pending"); return; }
        busy = true;
        new Thread(new Runnable() { public void run() {
            try {
                expenseRepository.save(expenseSnapshot);
                budgetRepository.save(budgetSnapshot);
                EventQueue.invokeLater(new Runnable() { public void run() { commit.run(); busy = false; refreshAll(); status.setText(message + " Saved."); }});
            } catch (final Exception exception) {
                EventQueue.invokeLater(new Runnable() { public void run() {
                    busy = false;
                    Dialogs.error(MainFrame.this, message + " failed. Changes remain in memory; retry is available.", exception);
                }});
            }
        }}, "SpendWise-save").start();
    }

    public void exportFiltered() {
        FileDialog dialog = new FileDialog(this, "Export filtered expenses", FileDialog.SAVE);
        dialog.setFile("expenses.csv"); dialog.setVisible(true);
        if (dialog.getFile() == null) return;
        Path path = Path.of(dialog.getDirectory(), dialog.getFile());
        if (Files.exists(path) && !Dialogs.confirm(this, "Overwrite the existing file?", "Confirm overwrite")) return;
        final List<Expense> snapshot = expensesPanel.getFilteredExpenses();
        new Thread(new Runnable() { public void run() {
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write("Date,Category,Note,Amount_INR\n");
                for (Expense expense : snapshot) writer.write(CsvReportExporter.line(expense));
                EventQueue.invokeLater(new Runnable() { public void run() { status.setText("CSV export completed."); }});
            } catch (final IOException exception) {
                EventQueue.invokeLater(new Runnable() { public void run() { Dialogs.error(MainFrame.this, "CSV export failed.", exception); }});
            }
        }}, "SpendWise-export").start();
    }

    private void chooseBackup() {
        FileDialog dialog = new FileDialog(this, "Save SpendWise backup", FileDialog.SAVE);
        dialog.setFile("spendwise-backup.properties"); dialog.setVisible(true);
        if (dialog.getFile() == null) return;
        Path path = Path.of(dialog.getDirectory(), dialog.getFile());
        if (Files.exists(path) && !Dialogs.confirm(this, "Overwrite this backup?", "Confirm overwrite")) return;
        try { writeBackup(path, getExpenses(), getBudgets()); status.setText("Backup completed."); }
        catch (IOException exception) { Dialogs.error(this, "Backup failed.", exception); }
    }

    private void chooseRestore() {
        FileDialog dialog = new FileDialog(this, "Restore SpendWise backup", FileDialog.LOAD);
        dialog.setVisible(true); if (dialog.getFile() == null) return;
        Path path = Path.of(dialog.getDirectory(), dialog.getFile());
        try {
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { properties.load(reader); }
            List<Expense> restoredExpenses = readExpenses(properties);
            List<Budget> restoredBudgets = readBudgets(properties);
            if (!Dialogs.confirm(this, "Restore replaces all current records. Continue?", "Confirm restore")) return;
            Path safety = path.resolveSibling(path.getFileName() + ".before-restore");
            writeBackup(safety, getExpenses(), getBudgets());
            expenseRepository.save(restoredExpenses); budgetRepository.save(restoredBudgets);
            expenses.clear(); expenses.addAll(restoredExpenses); budgets.clear(); budgets.addAll(restoredBudgets);
            refreshAll(); status.setText("Restore completed. Safety backup: " + safety);
        } catch (Exception exception) { Dialogs.error(this, "Restore failed. Current records were not replaced.", exception); }
    }

    private void writeBackup(Path path, List<Expense> expenseData, List<Budget> budgetData) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("format", "SpendWise backup 1");
        properties.setProperty("expense.count", Integer.toString(expenseData.size()));
        for (int i = 0; i < expenseData.size(); i++) {
            Expense e = expenseData.get(i); String p = "expense." + i + ".";
            properties.setProperty(p + "id", e.getId().toString()); properties.setProperty(p + "amount", e.getAmount().toPlainString());
            properties.setProperty(p + "category", e.getCategory().getDisplayName()); properties.setProperty(p + "date", e.getDate().toString());
            properties.setProperty(p + "note", e.getNote());
            properties.setProperty(p + "essential", Boolean.toString(e.isEssential()));
            properties.setProperty(p + "paymentMethod", e.getPaymentMethod().getDisplayName());
        }
        properties.setProperty("budget.count", Integer.toString(budgetData.size()));
        for (int i = 0; i < budgetData.size(); i++) {
            Budget b = budgetData.get(i); String p = "budget." + i + ".";
            properties.setProperty(p + "month", b.getMonth().toString()); properties.setProperty(p + "amount", b.getAmount().toPlainString());
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            properties.store(writer, "SpendWise backup");
        }
    }

    private List<Expense> readExpenses(Properties p) throws IOException {
        List<Expense> result = new ArrayList<Expense>(); int count = count(p, "expense.count");
        if (!"SpendWise backup 1".equals(p.getProperty("format"))) throw new IOException("Unsupported backup format.");
        Set<UUID> ids = new HashSet<UUID>();
        for (int i = 0; i < count; i++) {
            String x = "expense." + i + ".";
            try {
                BigDecimal amount = new BigDecimal(required(p, x + "amount"));
                spendwise.model.Category category = spendwise.model.Category.fromDisplayName(required(p, x + "category"));
                java.time.LocalDate date = java.time.LocalDate.parse(required(p, x + "date"));
                spendwise.service.ValidationService.validateExpenseValues(amount, category, date);
                UUID id = UUID.fromString(required(p, x + "id"));
                if (!ids.add(id)) throw new IOException("Duplicate expense ID in backup.");
                result.add(new Expense(id, amount, category, date, p.getProperty(x + "note", ""),
                        Boolean.parseBoolean(p.getProperty(x + "essential", "false")),
                        p.getProperty(x + "paymentMethod") == null ? spendwise.model.PaymentMethod.UNSPECIFIED
                                : spendwise.model.PaymentMethod.fromDisplayName(p.getProperty(x + "paymentMethod"))));
            } catch (Exception exception) { throw new IOException("Invalid backup expense " + i, exception); }
        }
        return result;
    }
    private List<Budget> readBudgets(Properties p) throws IOException {
        List<Budget> result = new ArrayList<Budget>(); int count = count(p, "budget.count");
        Set<YearMonth> months = new HashSet<YearMonth>();
        for (int i = 0; i < count; i++) {
            String x = "budget." + i + ".";
            try {
                BigDecimal amount = new BigDecimal(required(p, x + "amount"));
                spendwise.service.ValidationService.validateBudget(amount);
                YearMonth month = YearMonth.parse(required(p, x + "month"));
                if (!months.add(month)) throw new IOException("Duplicate budget month in backup.");
                result.add(new Budget(month, amount));
            } catch (Exception exception) { throw new IOException("Invalid backup budget " + i, exception); }
        }
        return result;
    }
    private int count(Properties p, String key) throws IOException {
        try { int value = Integer.parseInt(required(p, key)); if (value < 0) throw new NumberFormatException(); return value; }
        catch (NumberFormatException exception) { throw new IOException("Invalid count " + key, exception); }
    }
    private String required(Properties p, String key) throws IOException {
        String value = p.getProperty(key); if (value == null) throw new IOException("Missing property " + key); return value;
    }
    public void refreshAll() {
        dashboard.refreshMonths(); expensesPanel.refreshFilters(); budgetPanel.refresh(); reportsPanel.refreshMonths();
    }
    public void setStatus(String message) { status.setText(message); }
}
