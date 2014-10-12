package WeltladenDB;

import javax.swing.JFileChooser;
import java.io.File;
import javax.swing.JOptionPane;

public class FileExistsAwareFileChooser extends JFileChooser {
    // override approveSelection to get a confirmation dialog if file exists
    @Override
    public void approveSelection(){
        File f = getSelectedFile();
        if (f.exists() && getDialogType() == SAVE_DIALOG){
            int result = JOptionPane.showConfirmDialog(this,
                    "Datei existiert bereits. Überschreiben?",
                    "Datei existiert",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            switch (result){
                case JOptionPane.YES_OPTION:
                    super.approveSelection();
                    return;
                case JOptionPane.NO_OPTION:
                    return;
                case JOptionPane.CLOSED_OPTION:
                    return;
                case JOptionPane.CANCEL_OPTION:
                    cancelSelection();
                    return;
            }
        }
        super.approveSelection();
    }
}
