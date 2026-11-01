package shelflife.ui;

import shelflife.model.User;
import shelflife.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Arrays;

/** Asks the user to replace a temporary or default password before continuing. */
public final class ChangePasswordDialog {

    private static final int MIN_LENGTH = 8;

    private ChangePasswordDialog() {
    }

    /**
     * Shows the dialog until the password is changed.
     *
     * @return true when the password was changed, false when the user cancelled
     */
    public static boolean requireChange(Component parent, AuthService auth, User user) {
        String message = "Welcome, " + user.getFullName() + ". Please choose a new password before you start.";
        while (true) {
            JPasswordField current = new JPasswordField();
            JPasswordField next = new JPasswordField();
            JPasswordField repeat = new JPasswordField();
            JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
            form.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            form.add(new JLabel(message));
            form.add(new JLabel("Current password"));
            form.add(current);
            form.add(new JLabel("New password (at least " + MIN_LENGTH + " characters)"));
            form.add(next);
            form.add(new JLabel("Repeat new password"));
            form.add(repeat);

            int choice = JOptionPane.showConfirmDialog(parent, form, "Change password",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return false;
            }
            char[] a = current.getPassword();
            char[] b = next.getPassword();
            char[] c = repeat.getPassword();
            try {
                if (b.length < MIN_LENGTH) {
                    message = "The new password must have at least " + MIN_LENGTH + " characters.";
                } else if (!Arrays.equals(b, c)) {
                    message = "The two new passwords do not match.";
                } else if (Arrays.equals(a, b)) {
                    message = "The new password must be different from the current one.";
                } else {
                    auth.changePassword(user.getUsername(), new String(a), new String(b));
                    return true;
                }
            } catch (Exception ex) {
                message = ex.getMessage() == null ? "Could not change the password." : ex.getMessage();
            } finally {
                Arrays.fill(a, '\0');
                Arrays.fill(b, '\0');
                Arrays.fill(c, '\0');
            }
        }
    }
}
