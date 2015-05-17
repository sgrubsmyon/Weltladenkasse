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

    //Map<String, String> createMap(String name, String menge, String preis,
    //        String lieferant, String kg_preis) {
    //    HashMap<String, String> map = new HashMap<String, String>();
    //    map.put("name", name);
    //    map.put("menge", menge);
    //    map.put("preis", preis);
    //    map.put("lieferant", lieferant);
    //    map.put("kg_preis", kg_preis);
    //    return map;
    //}

    void createPriceTagFile() {
        // Load the template.
        // Java 5 users will have to use RhinoFileTemplate instead
        JavaScriptFileTemplate template = loadTemplate();
        if (template == null) return;

        // Fill with sample values.
        template.setField("name_0", "Äthiopienhonig");
        template.setField("menge_0", "500");
        template.setField("preis_0", "5,00 €");
        template.setField("lieferant_0", "EP");
        template.setField("kg_preis_0", "10,00 €");

        template.setField("name_1", "Blütenhonig");
        template.setField("menge_1", "500");
        template.setField("preis_1", "4,00 €");
        template.setField("lieferant_1", "EP");
        template.setField("kg_preis_1", "8,00 €");

        template.setField("name_2", "Kaffee Hausmarke");
        template.setField("menge_2", "250");
        template.setField("preis_2", "3,50 €");
        template.setField("lieferant_2", "EP");
        template.setField("kg_preis_2", "14,00 €");

        //List<Map<String, String>> articles = new ArrayList<Map<String, String>>();
        //articles.add(createMap("Äthiopienhonig", "500", "5,00 €", "EP", "10,00 €"));
        //articles.add(createMap("Blütenhonig", "500", "4,00 €", "EP", "8,00 €"));
        //articles.add(createMap("Kaffee Hausmarke", "250", "3,50 €", "EP", "14,00 €"));
        //template.setField("column1", articles);
        //template.setField("column2", articles);
        //template.setField("column3", articles);

        try {
            // Save to file.
            File outFile = new File("out.odt");
            System.out.println("Going to save.");
            template.saveAs(outFile);
            System.out.println("Have saved.");

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

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
    }
}
