package org.weltladen_bonn.pos;

// GUI stuff:
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
// import javax.swing.SwingUtilities;
// import java.lang.Runnable;

public class LongOperationIndicatorDialog extends JFrame {
    // private JDialog dialog;

    public LongOperationIndicatorDialog(JLabel messageLabel, JPanel buttonPanel) {
        // super(null, JOptionPane.INFORMATION_MESSAGE, JOptionPane.CANCEL_OPTION, null, new Object[]{});
        // super(null, "Bitte warten...", false); // false makes it non-modal, i.e. non-blocking so that code can continue to run in the background,
            // see https://www.oracle.com/technical-resources/articles/javase/modality.html
        setTitle("Bitte warten...");

        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(0, 30));
        progressBar.setMaximumSize(new Dimension(400, 30));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
    
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(messageLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(progressBar);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        if (buttonPanel != null) {
            buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(buttonPanel);
        }
        
        // setMessage(panel);
        // dialog = createDialog("Bitte warten...");

        JOptionPane pane = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE, JOptionPane.CANCEL_OPTION, null, new Object[]{});
        
        // dialog.pack();
        // dialog.setModal(false); // important: make this dialog non-modal, i.e. non-blocking so that code can continue to run in the background
        // dialog.setVisible(true);
        setContentPane(pane);
        pack();
        // setModal(false); // important: make this dialog non-modal, i.e. non-blocking so that code can continue to run in the background
        // this.setVisible(true);
        // JFrame frame = this;
        // SwingUtilities.invokeLater(new Runnable() {
        //     @Override
        //     public void run() {
        //         frame.setVisible(true);
        //     }
        // });
    }

    // public void dispose() {
    //     dialog.dispose();
    // }

}