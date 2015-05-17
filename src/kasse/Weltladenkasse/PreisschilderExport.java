package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.io.*; // for File

// GUI stuff:
import javax.swing.*; // JFrame, JPanel, JTable, JButton etc.
import java.awt.event.ActionEvent;

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

    public PreisschilderExport(Connection conn, MainWindowGrundlage mw,
            Vector<String> n, Vector<String> m, Vector<String> p,
            Vector<String> l, Vector<String> k) {
        super(conn, mw);

        this.names = n;
        this.mengen = m;
        this.preise = p;
        this.lieferanten = l;
        this.kg_preise = k;

        createPriceTagFile();
    }

    private JavaScriptFileTemplate loadTemplate() {
        final JavaScriptFileTemplate template;
        try {
            String filename = "vorlagen"+fileSep+"Preisschilder_Lebensmittel.odt";
            //String filename = "vorlagen"+fileSep+"Preisschilder_Lebensmittel_Test.odt";
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
                    template.setField("name_"+i, names.get(index));
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
                File outFile = new File( String.format("out_%03d", pageCounter) );
                System.out.println("Going to save price tag list.");
                template.saveAs(outFile);
                System.out.println("Done.");

                // Open the document with OpenOffice.org !
                OOUtils.open(outFile);
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
