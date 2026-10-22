package spendwise.ui;

import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class UiUtil {
    public static final Color NAVY = new Color(34, 52, 84);
    public static final Color LIGHT = new Color(246, 248, 252);
    public static final Font NORMAL = new Font("Dialog", Font.PLAIN, 14);
    public static final Font HEADING = new Font("Dialog", Font.BOLD, 20);
    private UiUtil() {}
    public static String money(BigDecimal amount) {
        return "\u20B9" + NumberFormat.getNumberInstance(new Locale("en", "IN")).format(amount.setScale(2));
    }
    public static String plainMoney(BigDecimal amount) { return amount.setScale(2).toPlainString(); }
}
