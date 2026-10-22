package spendwise.storage;

import spendwise.model.Expense;
import java.io.IOException;
import java.util.List;

public interface ExpenseRepository {
    List<Expense> load() throws IOException;
    void save(List<Expense> expenses) throws IOException;
}
