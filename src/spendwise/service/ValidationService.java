package spendwise.service;

import spendwise.model.Category;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class ValidationService {
    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private ValidationService() {}

    public static BigDecimal parseAmount(String text, String fieldName) throws ValidationException {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
        String value = text.trim();
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.signum() <= 0) {
                throw new ValidationException(fieldName + " must be positive.");
            }
            if (amount.scale() > 2) {
                throw new ValidationException(fieldName + " must have at most two decimal places.");
            }
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException ex) {
            throw new ValidationException(fieldName + " must be a valid number.");
        }
    }

    public static LocalDate parseExpenseDate(String text) throws ValidationException {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("Date is required. Use dd/MM/yyyy.");
        }
        try {
            LocalDate date = LocalDate.parse(text.trim(), DATE_FORMAT);
            if (date.isAfter(LocalDate.now())) {
                throw new ValidationException("Date cannot be in the future.");
            }
            return date;
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Date must be a real date in dd/MM/yyyy format.");
        }
    }

    public static void validateExpenseValues(BigDecimal amount, Category category, LocalDate date)
            throws ValidationException {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new ValidationException("Expense amount must be positive with at most two decimals.");
        }
        if (category == null) {
            throw new ValidationException("Expense category is required.");
        }
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new ValidationException("Expense date is invalid or in the future.");
        }
    }

    public static void validateBudget(BigDecimal amount) throws ValidationException {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new ValidationException("Budget must be positive with at most two decimals.");
        }
    }
}
