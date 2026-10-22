package spendwise.ui;

import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

public final class UiUtil {
    public static final Color NAVY = new Color(34, 52, 84);
    public static final Color TEAL = new Color(0, 137, 123);
    public static final Color DANGER = new Color(190, 55, 55);
    public static final Color LIGHT = new Color(246, 248, 252);
    public static final Font NORMAL = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font HEADING = new Font("Segoe UI", Font.BOLD, 22);

    private UiUtil() {}

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(HEADING);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        return label;
    }

    public static void stylePrimary(javax.swing.JButton button) {
        button.setBackground(TEAL);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
    }

    public static void styleDanger(javax.swing.JButton button) {
        button.setBackground(DANGER);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
    }

    public static String money(BigDecimal amount) {
        return "\u20B9" + NumberFormat.getNumberInstance(new Locale("en", "IN"))
                .format(amount.setScale(2));
    }

    public static String plainMoney(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }
}
