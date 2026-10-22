package spendwise.storage;

import spendwise.model.Category;
import spendwise.model.Expense;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class FileExpenseRepository implements ExpenseRepository {
    private final Path file;

    public FileExpenseRepository(Path dataDirectory) {
        this.file = dataDirectory.resolve("expenses.properties");
    }

    @Override
    public List<Expense> load() throws IOException {
        if (!Files.exists(file)) return new ArrayList<>();
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        int count = parseCount(properties);
        List<Expense> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String prefix = "expense." + i + ".";
            try {
                UUID id = UUID.fromString(required(properties, prefix + "id"));
                BigDecimal amount = new BigDecimal(required(properties, prefix + "amount"));
                Category category = Category.fromDisplayName(required(properties, prefix + "category"));
                LocalDate date = LocalDate.parse(required(properties, prefix + "date"));
                ValidationService.validateExpenseValues(amount, category, date);
                result.add(new Expense(id, amount, category, date, properties.getProperty(prefix + "note", "")));
            } catch (IllegalArgumentException | ValidationException ex) {
                throw new IOException("Invalid expense record at index " + i + " in " + file, ex);
            }
        }
        return result;
    }

    @Override
    public void save(List<Expense> expenses) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("count", Integer.toString(expenses.size()));
        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);
            String prefix = "expense." + i + ".";
            properties.setProperty(prefix + "id", expense.getId().toString());
            properties.setProperty(prefix + "amount", expense.getAmount().toPlainString());
            properties.setProperty(prefix + "category", expense.getCategory().getDisplayName());
            properties.setProperty(prefix + "date", expense.getDate().toString());
            properties.setProperty(prefix + "note", expense.getNote());
        }
        writeAtomically(properties);
    }

    private int parseCount(Properties properties) throws IOException {
        try {
            int count = Integer.parseInt(required(properties, "count"));
            if (count < 0) throw new NumberFormatException();
            return count;
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid expense count in " + file, ex);
        }
    }

    private String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) throw new IOException("Missing property '" + key + "' in " + file);
        return value;
    }

    private void writeAtomically(Properties properties) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "expenses-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                properties.store(writer, "SpendWise expenses");
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
}
