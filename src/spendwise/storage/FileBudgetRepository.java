package spendwise.storage;

import spendwise.model.Budget;
import spendwise.service.ValidationException;
import spendwise.service.ValidationService;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class FileBudgetRepository implements BudgetRepository {
    private final Path file;

    public FileBudgetRepository(Path dataDirectory) {
        this.file = dataDirectory.resolve("budgets.properties");
    }

    @Override
    public List<Budget> load() throws IOException {
        if (!Files.exists(file)) return new ArrayList<>();
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        int count;
        try {
            count = Integer.parseInt(required(properties, "count"));
            if (count < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid budget count in " + file, ex);
        }
        List<Budget> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String prefix = "budget." + i + ".";
            try {
                YearMonth month = YearMonth.parse(required(properties, prefix + "month"));
                BigDecimal amount = new BigDecimal(required(properties, prefix + "amount"));
                ValidationService.validateBudget(amount);
                result.add(new Budget(month, amount));
            } catch (IllegalArgumentException | ValidationException ex) {
                throw new IOException("Invalid budget record at index " + i + " in " + file, ex);
            }
        }
        return result;
    }

    @Override
    public void save(List<Budget> budgets) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("count", Integer.toString(budgets.size()));
        for (int i = 0; i < budgets.size(); i++) {
            Budget budget = budgets.get(i);
            String prefix = "budget." + i + ".";
            properties.setProperty(prefix + "month", budget.getMonth().toString());
            properties.setProperty(prefix + "amount", budget.getAmount().toPlainString());
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "budgets-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                properties.store(writer, "SpendWise budgets");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) throw new IOException("Missing property '" + key + "' in " + file);
        return value;
    }
}
