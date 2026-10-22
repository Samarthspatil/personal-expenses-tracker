package spendwise.storage;

import spendwise.model.Category;
import spendwise.model.Expense;
import spendwise.model.PaymentMethod;
import spendwise.service.ValidationService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class JdbcExpenseRepository implements ExpenseRepository {
    private final JdbcDatabase database;
    public JdbcExpenseRepository(Path dataDirectory) throws IOException { database = new JdbcDatabase(dataDirectory); migrateIfNeeded(dataDirectory); }
    public List<Expense> load() throws IOException {
        List<Expense> result = new ArrayList<Expense>();
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement("SELECT id, amount, category, expense_date, note, essential, payment_method FROM expenses ORDER BY expense_date")) {
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                Expense expense = new Expense(UUID.fromString(rows.getString(1)), rows.getBigDecimal(2),
                        Category.fromDisplayName(rows.getString(3)), rows.getDate(4).toLocalDate(),
                        rows.getString(5), rows.getBoolean(6), PaymentMethod.fromDisplayName(rows.getString(7)));
                ValidationService.validateExpenseValues(expense.getAmount(), expense.getCategory(), expense.getDate());
                result.add(expense);
            }
            return result;
        } catch (Exception exception) { throw new IOException("Could not load expenses from H2.", exception); }
    }
    public void save(List<Expense> expenses) throws IOException {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement clear = connection.prepareStatement("DELETE FROM expenses");
                 PreparedStatement insert = connection.prepareStatement("INSERT INTO expenses VALUES (?,?,?,?,?,?,?)")) {
                clear.executeUpdate();
                for (Expense expense : expenses) {
                    insert.setString(1, expense.getId().toString()); insert.setBigDecimal(2, expense.getAmount());
                    insert.setString(3, expense.getCategory().getDisplayName()); insert.setObject(4, expense.getDate());
                    insert.setString(5, expense.getNote()); insert.setBoolean(6, expense.isEssential());
                    insert.setString(7, expense.getPaymentMethod().getDisplayName()); insert.addBatch();
                }
                insert.executeBatch(); connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
        } catch (SQLException exception) { throw new IOException("Could not save expenses to H2.", exception); }
    }
    private void migrateIfNeeded(Path directory) throws IOException {
        try (Connection connection = database.open();
             PreparedStatement check = connection.prepareStatement("SELECT metadata_value FROM app_metadata WHERE name='properties-migration'")) {
            ResultSet rows = check.executeQuery();
            if (rows.next()) return;
            Path file = directory.resolve("expenses.properties");
            if (java.nio.file.Files.exists(file)) {
                Path backup = directory.resolve("expenses.properties.pre-jdbc-backup");
                if (!java.nio.file.Files.exists(backup)) java.nio.file.Files.copy(file, backup);
                List<Expense> imported = new FileExpenseRepository(directory).load();
                save(imported);
            }
            try (PreparedStatement mark = connection.prepareStatement("MERGE INTO app_metadata KEY(name) VALUES('properties-migration','expenses')")) { mark.executeUpdate(); }
        } catch (SQLException exception) { throw new IOException("Could not migrate expenses to H2.", exception); }
    }
}
