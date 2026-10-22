import spendwise.model.Category;
import spendwise.model.Expense;
import spendwise.service.ExpenseService;
import spendwise.service.ValidationException;
import spendwise.service.ValidationService;
import spendwise.storage.FileExpenseRepository;
import spendwise.storage.JdbcExpenseRepository;
import spendwise.ui.CsvReportExporter;
import spendwise.ui.TextReportExporter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

public final class LogicCheck {
    public static void main(String[] args) throws Exception {
        check(new BigDecimal("0.10").add(new BigDecimal("0.20")).compareTo(new BigDecimal("0.30")) == 0,
                "BigDecimal decimal calculation");
        check(ValidationService.parseAmount("12.30", "Amount").scale() == 2, "amount validation");
        expectInvalid("12.345");
        expectInvalid("0");
        List<Expense> expenses = Arrays.asList(
                new Expense(new BigDecimal("0.10"), Category.FOOD, LocalDate.of(2025, 12, 31), "Tea, \"special\" \u2615"),
                new Expense(new BigDecimal("0.20"), Category.FOOD, LocalDate.of(2026, 1, 1), "January"));
        ExpenseService service = new ExpenseService();
        check(service.calculateTotal(expenses, YearMonth.of(2025, 12)).compareTo(new BigDecimal("0.10")) == 0,
                "December filtering");
        check(service.calculateTotal(expenses, YearMonth.of(2026, 1), Category.FOOD).compareTo(new BigDecimal("0.20")) == 0,
                "January/category filtering");
        Path temp = Files.createTempDirectory("spendwise-check");
        FileExpenseRepository repository = new FileExpenseRepository(temp);
        repository.save(expenses);
        List<Expense> loaded = repository.load();
        check(loaded.size() == 2 && loaded.get(0).getNote().contains("\u2615"), "persistence Unicode round-trip");
        JdbcExpenseRepository jdbc = new JdbcExpenseRepository(temp.resolve("jdbc"));
        jdbc.save(expenses);
        check(jdbc.load().size() == 2, "H2 JDBC round-trip");
        String csv = new CsvReportExporter().export(YearMonth.of(2025, 12), null, Arrays.asList(expenses.get(0)));
        check(csv.contains("\"Tea, \"\"special\"\" \u2615\""), "CSV escaping");
        check(new TextReportExporter().export(YearMonth.of(2025, 12), null,
                Arrays.asList(expenses.get(0))).contains("Total spending: 0.10"), "text report");
        Files.writeString(temp.resolve("expenses.properties"), "count=1\nexpense.0.amount=bad\n");
        try {
            repository.load();
            throw new AssertionError("malformed file was accepted");
        } catch (java.io.IOException expected) {
            System.out.println("Malformed file rejected: PASS");
        }
        System.out.println("All lightweight checks passed.");
    }

    private static void expectInvalid(String amount) throws Exception {
        try { ValidationService.parseAmount(amount, "Amount"); throw new AssertionError("accepted " + amount); }
        catch (ValidationException expected) { }
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        System.out.println(name + ": PASS");
    }
}
