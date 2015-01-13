package Weltladenkasse;

import java.util.Vector;

// GUI stuff:
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JLabel;

// Class holding only the main function
public class Kasse {
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //JFrame.setDefaultLookAndFeelDecorated(true);

        boolean passwdIncorrect = false;
        while (true){
            JLabel label = new JLabel("");
            if (passwdIncorrect){ label = new JLabel("Falsches Passwort!"); }
            Vector<String> result = showPasswordWindow(label);
            if (result.get(0) == "CANCEL"){
                return;
            }
            if (result.get(0) == "OK"){
                String password = result.get(1);
                final MainWindow myWindow = new MainWindow(password);

                if (myWindow.connectionWorks){
                    // Check if there is an incomplete Tagesabrechnung from the start!
                    if ( myWindow.isThereIncompleteAbrechnungTag() ){
                        JOptionPane.showMessageDialog(myWindow,
                                "Hinweis: Es gibt eine offene Tagesabrechnung.\n"+
                                "Wurde vergessen, Tagesabrechnung zu machen (unter 'Abrechnungen')?",
                                "Ausstehende Abrechnung", JOptionPane.WARNING_MESSAGE);
                    }

                    //myWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    //myWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // (maybe better)
                    myWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // (first check if Tagesabrechnung has been done:)
                    myWindow.addWindowListener(new WindowAdapter(){
                        @Override
                        public void windowClosing(WindowEvent we) {
                            if ( myWindow.isThereIncompleteAbrechnungTag() ){
                                int answer = JOptionPane.showConfirmDialog(myWindow,
                                    "Es wurde heute noch keine Tagesabrechnung (unter 'Abrechnungen') gemacht.\n"+
                                    "Wirklich schließen?", "Warnung",
                                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (answer == JOptionPane.YES_OPTION){
                                    myWindow.dispose();
                                } else {
                                    // do nothing
                                }
                            } else {
                                myWindow.dispose();
                            }
                        }
                    });

                    myWindow.setTitle("Weltladenkasse");
                    // Specify where it will appear on the screen:
                    //	myWindow.setLocation(200, 100);
                    //myWindow.setSize(1024, 768);
                    //myWindow.setSize(1024, 400);
                    myWindow.setPreferredSize(new Dimension(1024, 768));
                    myWindow.pack();

                    //WelcomeScreen welcome = new WelcomeScreen();
                    //myWindow.setContentPanel(welcome);

                    // Show it!
                    myWindow.setVisible(true);
                    System.out.println("Password was correct.");
                    return;
                } else {
                    passwdIncorrect = true;
                    System.out.println("Password was incorrect.");
                }
            }
        }
    }

    private static Vector<String> showPasswordWindow(JLabel label) {
        // retrieve the password from the user, set focus to password field
        // gracefully taken from http://blogger.ziesemer.com/2007/03/java-password-dialog.html,
        // @ack credit: Mark A. Ziesemer, Anonymous, Akerbos (see comments)
        final JPasswordField pf = new JPasswordField();
        JOptionPane jop = null;
        if (label.getText().length() > 0){
            jop = new JOptionPane(new Object[]{label, pf},
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
        } else {
            jop = new JOptionPane(pf,
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
        }
        JDialog dialog = jop.createDialog("Bitte Mitarbeiter-Passwort eingeben");
        dialog.addWindowFocusListener(new WindowAdapter(){
            @Override
            public void windowGainedFocus(WindowEvent e){
                pf.requestFocusInWindow();
            }
        });
        pf.addFocusListener(new FocusListener() {
            public void focusGained( FocusEvent e ) {
                pf.selectAll();
            }
            public void focusLost( FocusEvent e ) {
                if ( pf.getPassword().length == 0 ) {
                    pf.requestFocusInWindow();
                }
            }
        });
        dialog.setVisible(true);
        int result = (Integer)jop.getValue();
        dialog.dispose();

        Vector<String> okPassword = new Vector<String>(2);
        String ok;
        if (result == JOptionPane.OK_OPTION){ ok = "OK"; }
        else { ok = "CANCEL"; }
        String password = new String(pf.getPassword());
        okPassword.add(ok);
        okPassword.add(password);
        return okPassword;
    }

    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                //UIManager.put("swing.boldMetal", Boolean.FALSE);
                /*
                // Set System Look&Feel
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                catch (UnsupportedLookAndFeelException ex) {
                    System.out.println("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
                catch (ClassNotFoundException ex) {
                    System.out.println("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
                catch (InstantiationException ex) {
                    System.out.println("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
                catch (IllegalAccessException ex) {
                    System.out.println("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
                */
                createAndShowGUI();
	//	try {
	//	    myKasse.conn.close();
	//	} catch (SQLException ex) {
	//	    System.out.println("Exception: " + ex.getMessage());
	//	    ex.printStackTrace();
	//	}
            }
        });
    }
}
