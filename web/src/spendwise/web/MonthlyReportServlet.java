package spendwise.web;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;

public final class MonthlyReportServlet extends HttpServlet {
    private String databaseUrl;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        databaseUrl = "jdbc:h2:" + Path.of(System.getProperty("user.home"), "SpendWiseData", "spendwise");
        getServletContext().log("SpendWise monthly report servlet started.");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String monthText = request.getParameter("month");
        String yearText = request.getParameter("year");
        YearMonth month;
        try {
            int year = Integer.parseInt(yearText == null ? Integer.toString(java.time.Year.now().getValue()) : yearText);
            int value = Integer.parseInt(monthText == null ? Integer.toString(java.time.LocalDate.now().getMonthValue()) : monthText);
            month = YearMonth.of(year, value);
        } catch (RuntimeException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Month and year must be valid numbers.");
            return;
        }
        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*), COALESCE(SUM(amount), 0) FROM expenses WHERE expense_date >= ? AND expense_date < ?")) {
            statement.setObject(1, month.atDay(1));
            statement.setObject(2, month.plusMonths(1).atDay(1));
            ResultSet result = statement.executeQuery();
            BigDecimal total = BigDecimal.ZERO; int count = 0;
            if (result.next()) { count = result.getInt(1); total = result.getBigDecimal(2); }
            request.setAttribute("month", month.toString());
            request.setAttribute("count", count);
            request.setAttribute("total", total);
            request.getRequestDispatcher("/WEB-INF/monthly-report.jsp").forward(request, response);
        } catch (SQLException exception) {
            getServletContext().log("SpendWise report query failed.", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load report data.");
        }
    }

    public void destroy() {
        getServletContext().log("SpendWise monthly report servlet stopped.");
        databaseUrl = null;
    }
}
