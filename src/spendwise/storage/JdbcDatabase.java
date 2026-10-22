package spendwise.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class JdbcDatabase {
    private final String url;

    public JdbcDatabase(Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        this.url = "jdbc:h2:" + dataDirectory.resolve("spendwise").toString();
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS expenses ("
                    + "id VARCHAR(36) PRIMARY KEY, amount DECIMAL(19,2) NOT NULL, category VARCHAR(40) NOT NULL,"
                    + "expense_date DATE NOT NULL, note VARCHAR(2000), essential BOOLEAN NOT NULL,"
                    + "payment_method VARCHAR(30) NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS budgets ("
                    + "month_value VARCHAR(7) PRIMARY KEY, amount DECIMAL(19,2) NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS app_metadata (name VARCHAR(80) PRIMARY KEY, metadata_value VARCHAR(200))");
        } catch (SQLException exception) {
            throw new IOException("Could not initialize H2 database.", exception);
        }
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }
}
