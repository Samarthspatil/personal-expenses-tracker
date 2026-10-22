package spendwise.storage;

import spendwise.model.Budget;
import spendwise.service.ValidationService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class JdbcBudgetRepository implements BudgetRepository {
    private final JdbcDatabase database;
    public JdbcBudgetRepository(Path dataDirectory) throws IOException { database = new JdbcDatabase(dataDirectory); migrateIfNeeded(dataDirectory); }
    public List<Budget> load() throws IOException {
        List<Budget> result = new ArrayList<Budget>();
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement("SELECT month_value, amount FROM budgets ORDER BY month_value")) {
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                BigDecimal amount = rows.getBigDecimal(2); ValidationService.validateBudget(amount);
                result.add(new Budget(YearMonth.parse(rows.getString(1)), amount));
            }
            return result;
        } catch (Exception exception) { throw new IOException("Could not load budgets from H2.", exception); }
    }
    public void save(List<Budget> budgets) throws IOException {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement clear = connection.prepareStatement("DELETE FROM budgets");
                 PreparedStatement insert = connection.prepareStatement("INSERT INTO budgets VALUES (?,?)")) {
                clear.executeUpdate();
                for (Budget budget : budgets) { insert.setString(1, budget.getMonth().toString()); insert.setBigDecimal(2, budget.getAmount()); insert.addBatch(); }
                insert.executeBatch(); connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
        } catch (SQLException exception) { throw new IOException("Could not save budgets to H2.", exception); }
    }
    private void migrateIfNeeded(Path directory) throws IOException {
        Path file = directory.resolve("budgets.properties");
        if (!java.nio.file.Files.exists(file)) return;
        Path backup = directory.resolve("budgets.properties.pre-jdbc-backup");
        if (!java.nio.file.Files.exists(backup)) java.nio.file.Files.copy(file, backup);
        try (Connection connection = database.open();
             PreparedStatement check = connection.prepareStatement("SELECT metadata_value FROM app_metadata WHERE name='budget-migration'")) {
            ResultSet rows = check.executeQuery(); if (rows.next()) return;
            save(new FileBudgetRepository(directory).load());
            try (PreparedStatement mark = connection.prepareStatement("MERGE INTO app_metadata KEY(name) VALUES('budget-migration','complete')")) { mark.executeUpdate(); }
        } catch (SQLException exception) { throw new IOException("Could not migrate budgets to H2.", exception); }
    }
}
