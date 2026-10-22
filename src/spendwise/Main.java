package spendwise;

import spendwise.storage.FileBudgetRepository;
import spendwise.storage.FileExpenseRepository;
import spendwise.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            Path dataDirectory = Paths.get(System.getProperty("user.home"), "SpendWiseData");
            new MainFrame(new FileExpenseRepository(dataDirectory), new FileBudgetRepository(dataDirectory)).setVisible(true);
        });
    }
}
