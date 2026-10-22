package spendwise.storage;

import spendwise.model.Budget;
import java.io.IOException;
import java.util.List;

public interface BudgetRepository {
    List<Budget> load() throws IOException;
    void save(List<Budget> budgets) throws IOException;
}
