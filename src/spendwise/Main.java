package spendwise;

import spendwise.ui.MainFrame;
import java.awt.EventQueue;

public final class Main {
    private Main() {}
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainFrame frame = new MainFrame();
                    frame.setVisible(true);
                } catch (RuntimeException exception) {
                    System.err.println("SpendWise could not start: " + exception.getMessage());
                }
            }
        });
    }
}
