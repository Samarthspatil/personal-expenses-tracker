package spendwise.ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class Dialogs {
    private Dialogs() {}
    public static void message(Frame owner, String text, String title) {
        final Dialog dialog = new Dialog(owner, title, true);
        dialog.setLayout(new BorderLayout(12, 12)); dialog.add(new Label(text), BorderLayout.CENTER);
        Button ok = new Button("OK"); ok.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { dialog.dispose(); }});
        Panel buttons = new Panel(new FlowLayout()); buttons.add(ok); dialog.add(buttons, BorderLayout.SOUTH);
        dialog.addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { dialog.dispose(); }});
        dialog.setSize(430, 130); dialog.setLocationRelativeTo(owner); dialog.setVisible(true);
    }
    public static void error(Frame owner, String text, Exception exception) {
        message(owner, text + (exception == null ? "" : "\n" + exception.getMessage()), "SpendWise error");
    }
    public static boolean confirm(Frame owner, String text, String title) {
        final boolean[] answer = {false};
        final Dialog dialog = new Dialog(owner, title, true);
        dialog.setLayout(new BorderLayout(12, 12)); dialog.add(new Label(text), BorderLayout.CENTER);
        Panel buttons = new Panel(new FlowLayout()); Button yes = new Button("Yes"); Button no = new Button("No");
        yes.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { answer[0] = true; dialog.dispose(); }});
        no.addActionListener(new java.awt.event.ActionListener() { public void actionPerformed(java.awt.event.ActionEvent e) { dialog.dispose(); }});
        buttons.add(yes); buttons.add(no); dialog.add(buttons, BorderLayout.SOUTH);
        dialog.addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { dialog.dispose(); }});
        dialog.setSize(460, 140); dialog.setLocationRelativeTo(owner); dialog.setVisible(true); return answer[0];
    }
}
