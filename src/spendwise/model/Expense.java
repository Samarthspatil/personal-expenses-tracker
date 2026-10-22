package spendwise.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

public final class Expense {
    private final UUID id;
    private final BigDecimal amount;
    private final Category category;
    private final LocalDate date;
    private final String note;

    public Expense(UUID id, BigDecimal amount, Category category, LocalDate date, String note) {
        this.id = Objects.requireNonNull(id);
        this.amount = amount;
        this.category = Objects.requireNonNull(category);
        this.date = Objects.requireNonNull(date);
        this.note = note == null ? "" : note.trim();
    }

    public Expense(BigDecimal amount, Category category, LocalDate date, String note) {
        this(UUID.randomUUID(), amount, category, date, note);
    }

    public UUID getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public Category getCategory() { return category; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public YearMonth getMonth() { return YearMonth.from(date); }
}
