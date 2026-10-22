package spendwise.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

public final class Budget {
    private final YearMonth month;
    private final BigDecimal amount;

    public Budget(YearMonth month, BigDecimal amount) {
        this.month = Objects.requireNonNull(month);
        this.amount = amount;
    }

    public YearMonth getMonth() { return month; }
    public BigDecimal getAmount() { return amount; }
}
