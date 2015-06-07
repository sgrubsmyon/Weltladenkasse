package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.io.*; // for File
import java.lang.*; // Math, StringIndexOutOfBoundsException
import java.text.BreakIterator;

// GUI stuff:
import java.awt.event.ActionEvent;
import javax.swing.*; // JFrame, JPanel, JTable, JButton etc.
import javax.swing.filechooser.FileNameExtensionFilter;

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statement, ResultSet ...

// jOpenDocument stuff:
import org.jopendocument.dom.template.*; // JavaScriptFileTemplate, TemplateException
import org.jopendocument.dom.OOUtils;
import org.jdom.JDOMException;

import WeltladenDB.*;

public class PreisschilderExport extends WindowContent {
    Vector<String> names;
    Vector<String> mengen;
    Vector<String> preise;
    Vector<String> lieferanten;
    Vector<String> kg_preise;
    private FileExistsAwareFileChooser odtChooser;

    public PreisschilderExport(Connection conn, MainWindowGrundlage mw,
            Vector<String> n, Vector<String> m, Vector<String> p,
            Vector<String> l, Vector<String> k) {
        super(conn, mw);

        this.names = n;
        this.mengen = m;
        this.preise = p;
        this.lieferanten = l;
        this.kg_preise = k;

        odtChooser = new FileExistsAwareFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "ODT Text-Dokumente", "odt");
        odtChooser.setFileFilter(filter);

        createPriceTagFile();
    }


    private JavaScriptFileTemplate loadTemplate() {
        final JavaScriptFileTemplate template;
        try {
            String filename = "vorlagen"+bc.fileSep+"Preisschilder_Lebensmittel.odt";
            //String filename = "vorlagen"+bc.fileSep+"Preisschilder_Lebensmittel_Test.odt";
            File infile = new File(filename);
            if (!infile.exists()){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Vorlage "+
                        "'"+filename+"' nicht gefunden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            template = new JavaScriptFileTemplate(infile);
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        } catch (TemplateException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        } catch (JDOMException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
        return template;
    }


    String parseName(String name){
        //System.out.println("Original string: *"+name+"*");
        // chop off | ...
        String[] split_name = name.split("\\|");
        name = split_name[0];
        for (int j=1; j<split_name.length-1; j++){
            name += split_name[j];
        }
        //System.out.println("String w/o |: *"+name+"*");
        // truncate string after last word boundary preceding 40 chars
        try {
            BreakIterator bi = BreakIterator.getWordInstance();
            bi.setText(name);
            int last_before = bi.preceding(50);
            name = name.substring(0, last_before);
        } catch (IllegalArgumentException ex) {
            // pass (no need to truncate, because string ends before 40)
        }
        name = name.trim(); // remove possible unwanted whitespace at end
        // non-alphanum char at end?
        try {
            boolean bad_at_end = name.substring(name.length()-1, name.length()).matches("\\W");
            if (bad_at_end){
                // remove last character:
                name = name.substring(0, name.length()-1);
            }
        } catch (StringIndexOutOfBoundsException ex) {
            // pass, probably empty string
        }
        //System.out.println("Truncated string: *"+name+"*");
        return name;
    }


    void createPriceTagFile() {
        int fieldCounter = 0;
        int pageCounter = 0;
        while ( fieldCounter < names.size() ) {
            // Load the template.
            // Java 5 users will have to use RhinoFileTemplate instead
            JavaScriptFileTemplate template = loadTemplate();
            if (template == null) return;
            pageCounter++;

            for (int i=0; i<27; i++){ // always fill full page, 27 fields
                int index = fieldCounter;
                if ( index < names.size() ){
                    template.setField("name_"+i, parseName(names.get(index)));
                    template.setField("menge_"+i, mengen.get(index));
                    template.setField("preis_"+i, preise.get(index));
                    template.setField("lieferant_"+i, lieferanten.get(index));
                    template.setField("kg_preis_"+i, kg_preise.get(index));
                    template.setField("text_"+i, "Grundpreis\n(pro kg oder l)");
                } else {
                    template.setField("name_"+i, "");
                    template.setField("menge_"+i, "");
                    template.setField("preis_"+i, "");
                    template.setField("lieferant_"+i, "");
                    template.setField("kg_preis_"+i, "");
                    template.setField("text_"+i, "");
                }
                fieldCounter++;
            }

            try {
                // Save to file.
                odtChooser.setSelectedFile(new File( String.format("Preisschilder_%03d.odt", pageCounter) ));
                int returnVal = odtChooser.showSaveDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION){
                    File outFile = odtChooser.getSelectedFile();

                    System.out.println("Going to save price tag list.");
                    template.saveAs(outFile);
                    System.out.println("Done.");

                    String wantedPath = outFile.getAbsolutePath();
                    String actualPath = wantedPath+".fodt"; // strange bug? always saved like this
                    File existingFile = new File(actualPath);
                    existingFile.renameTo(new File(wantedPath));
                    System.out.println("Written to " + wantedPath);

                    // Open the document with OpenOffice.org !
                    OOUtils.open(outFile);
                } else {
                    System.out.println("Save command cancelled by user.");
                }
            } catch (IOException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            } catch (TemplateException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
    }
}
