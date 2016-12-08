package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.io.*; // for File
import java.text.BreakIterator;

// GUI stuff:
import java.awt.event.ActionEvent;
import javax.swing.*; // JFrame, JPanel, JTable, JButton etc.
import javax.swing.filechooser.FileNameExtensionFilter;

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statement, ResultSet ...

// jOpenDocument stuff:
import org.jopendocument.dom.template.*; // JavaScriptTemplate, TemplateException
import org.jopendocument.dom.OOUtils;
import org.jdom.JDOMException;

import org.weltladen_bonn.pos.*;

public class PreisschilderExport extends WindowContent {
    /**
     * Class for generating OpenDocument Text document with price tags
     */
    private static final long serialVersionUID = 1L;
    String typ;
    Vector<String> namen;
    Vector<String> nummern;
    Vector<String> mengen;
    Vector<String> preise;
    Vector<String> lieferanten;
    Vector<String> kgPreise;
    Vector<String> herkuenfte;
    private FileExistsAwareFileChooser odtChooser;

    public PreisschilderExport(Connection conn, MainWindowGrundlage mw, String typ, Vector<String> lieferanten,
            Vector<String> namen, Vector<String> nummern, Vector<String> mengen, Vector<String> preise,
            Vector<String> kgPreise, Vector<String> herkuenfte) {
        /**
         * typ is either "lm" for Lebensmittel or "khw" for "Kunsthandwerk"
         */
        super(conn, mw);

        this.typ = typ;
        this.namen = namen;
        this.nummern = nummern;
        this.mengen = mengen;
        this.preise = preise;
        this.lieferanten = lieferanten;
        this.kgPreise = kgPreise;
        this.herkuenfte = herkuenfte;

        odtChooser = new FileExistsAwareFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("ODT Text-Dokumente", "odt");
        odtChooser.setFileFilter(filter);

        createPriceTagFile();
    }

    private JavaScriptTemplate loadTemplate() {
        final JavaScriptTemplate template;
        try {
            String filename = "vorlagen" + bc.fileSep;
            if (typ.equals("lm")) {
                filename += "Preisschilder_Lebensmittel.odt";
            } else if (typ.equals("khw")) {
                filename += "Preisschilder_Kunsthandwerk.odt";
            } else {
                filename += "Preisschilder.odt";
            }
            // String filename =
            // "vorlagen"+bc.fileSep+"Preisschilder_Lebensmittel_Test.odt";
            File infile = new File(filename);
            if (!infile.exists()) {
                JOptionPane.showMessageDialog(this, "Fehler: Vorlage " + "'" + filename + "' nicht gefunden.", "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
            template = new JavaScriptTemplate(infile);
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

    String parseName(String name) {
        int cut = 50;
        // System.out.println("Original string: *"+name+"*");
        // chop off | ...
        String[] split_name = name.split("\\|");
        name = split_name[0];
        for (int j = 1; j < split_name.length - 1; j++) {
            name += split_name[j];
        }
        // System.out.println("String w/o |: *"+name+"*");
        // truncate string after last word boundary preceding 'cut' chars
        try {
            BreakIterator bi = BreakIterator.getWordInstance();
            bi.setText(name);
            int last_before = bi.preceding(cut);
            name = name.substring(0, last_before);
        } catch (IllegalArgumentException ex) {
            // pass (no need to truncate, because string ends before 'cut')
        }
        name = name.trim(); // remove possible unwanted whitespace at end
        // non-alphanum char at end?
        try {
            boolean bad_at_end = name.substring(name.length() - 1, name.length()).matches("\\W");
            // make exceptions:
            if (name.substring(name.length() - 1, name.length()).matches("\'|\"|\\)")) {
                bad_at_end = false;
            }
            if (bad_at_end) {
                // remove last character:
                name = name.substring(0, name.length() - 1);
            }
        } catch (StringIndexOutOfBoundsException ex) {
            // pass, probably empty string
        }
        // System.out.println("Truncated string: *"+name+"*");
        return name;
    }

    String parseHerkunft(String herkunft) {
        int cut = 19;
        if (herkunft.length() > cut) {
            // truncate string after 'cut' chars and add "..."
            herkunft = herkunft.substring(0, cut) + "...";
        }
        return herkunft;
    }

    void createPriceTagFile() {
        int fieldCounter = 0;
        int pageCounter = 0;
        while (fieldCounter < namen.size()) {
            // Load the template.
            // Java 5 users will have to use RhinoFileTemplate instead
            JavaScriptTemplate template = loadTemplate();
            if (template == null)
                return;
            pageCounter++;

            for (int i = 0; i < 27; i++) { // always fill full page, 27 fields
                int index = fieldCounter;
                if (index < namen.size()) {
                    template.setField("name_" + i, parseName(namen.get(index)));
                    template.setField("preis_" + i, preise.get(index));
                    template.setField("lieferant_" + i, lieferanten.get(index));
                    if (typ.equals("lm")) {
                        template.setField("menge_" + i, mengen.get(index));
                        template.setField("kg_preis_" + i, kgPreise.get(index));
                        template.setField("text_" + i, "Grundpreis\n(pro kg oder l)");
                    } else if (typ.equals("khw")) {
                        template.setField("herkunft_" + i, parseHerkunft(herkuenfte.get(index)));
                        template.setField("nummer_" + i, nummern.get(index));
                        template.setField("text_" + i, "");
                    }
                } else {
                    template.setField("name_" + i, "");
                    template.setField("preis_" + i, "");
                    template.setField("lieferant_" + i, "");
                    template.setField("text_" + i, "");
                    if (typ.equals("lm")) {
                        template.setField("menge_" + i, "");
                        template.setField("kg_preis_" + i, "");
                    } else if (typ.equals("khw")) {
                        template.setField("herkunft_" + i, "");
                        template.setField("nummer_" + i, "");
                    }
                }
                fieldCounter++;
            }

            try {
                // Save to file.
                odtChooser.setSelectedFile(new File(String.format("Preisschilder_%03d.odt", pageCounter)));
                int returnVal = odtChooser.showSaveDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File outFile = odtChooser.getSelectedFile();

                    System.out.println("Going to save price tag list.");
                    template.saveAs(outFile);
                    System.out.println("Done.");

                    String wantedPath = outFile.getAbsolutePath();
                    String actualPath = wantedPath + ".fodt"; // strange bug?
                                                              // always saved
                                                              // like this
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
     * * Each non abstract class that implements the ActionListener must have
     * this method.
     *
     * @param e
     *            the action event.
     **/
    public void actionPerformed(ActionEvent e) {
    }
}
