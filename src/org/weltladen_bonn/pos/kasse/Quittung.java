package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.io.*;
import java.lang.Process; // for executing system commands
import java.lang.Runtime; // for executing system commands
import java.text.SimpleDateFormat; // for parsing and formatting dates
import java.text.ParseException;

// GUI stuff:
import javax.swing.*; // JFrame, JPanel, JTable, JButton etc.
import java.awt.event.ActionEvent;
import java.awt.print.*;
import javax.print.*; // PrintService
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

// MySQL Connector/J stuff:
import org.mariadb.jdbc.MariaDbPoolDataSource;

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

// jOpenDocument stuff:
import org.jopendocument.dom.spreadsheet.*; // SpreadSheet
import org.jopendocument.dom.OOUtils;
import org.jopendocument.model.OpenDocument;
import org.jopendocument.print.ODTPrinter;

// EPSON ESC/POS printing:
import com.github.anastaciocintra.output.PrinterOutputStream;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPos.CharacterCodeTable;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.escpos.PrintModeStyle;
import com.github.anastaciocintra.escpos.PrintModeStyle.FontName;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.weltladen_bonn.pos.*;
import org.weltladen_bonn.pos.kasse.WeltladenTSE;
import org.weltladen_bonn.pos.kasse.WeltladenTSE.TSETransaction;

public class Quittung extends WindowContent {
    private static final Logger logger = LogManager.getLogger(Quittung.class);

    private DateTime datetime;
    private Integer rechnungsNr;
    private Integer stornoVon;
    private Vector<KassierArtikel> kassierArtikel;
    private TreeMap< BigDecimal, Vector<BigDecimal> > mwstValues;
    private String zahlungsModus;
    private BigDecimal totalPrice;
    private BigDecimal kundeGibt;
    private BigDecimal rueckgeld;
    private TSETransaction tx;
    private LinkedHashMap<String, String> tseStatusValues;

    private int artikelIndex = 0;
    private int rowOffset = 7;
    private Vector<BigDecimal> mwstList;

    // ESC/POS font setup:
    // Use PrintModeStyle with Font_B (default of TM-U220), which is the slightly smaller small font:
    private PrintModeStyle normal = new PrintModeStyle().setFontName(FontName.Font_B);
    private PrintModeStyle bold = new PrintModeStyle().setFontName(FontName.Font_B).setBold(true);
    private PrintModeStyle larger = new PrintModeStyle().setFontName(FontName.Font_A_Default);
    private PrintModeStyle boldlarger = new PrintModeStyle().setFontName(FontName.Font_A_Default).setBold(true);
    private String indent = "         ";
    private int rowLength = 31; // 31 chars fit on one row of a receipt
    private int bezLaenge = 23; // make article names shorter for improved aesthetics (totals appear more to be in one column)
    // for header:
    private String shopName = "WELTLADEN BONN";
    private String shopAddress = String.join(" ", String.join(", ", bc.STRASSE, bc.PLZ), bc.ORT);
    private String shopPhone = "Telefon: 0228 / 69 70 52";
    private String shopURL = "www.weltladen-bonn.org";

    /**
     *    The constructor.
     *       */
    public Quittung(MariaDbPoolDataSource pool, MainWindowGrundlage mw,
            DateTime dt, Integer rechnungsNr, Integer stv,
            Vector<KassierArtikel> ka,
            TreeMap< BigDecimal, Vector<BigDecimal> > mv,
            String zm, BigDecimal tp, BigDecimal kgb, BigDecimal rg,
            TSETransaction transaction,
            LinkedHashMap<String, String> tseStatusValues) {
	    super(pool, mw);
        this.datetime = dt;
        this.rechnungsNr = rechnungsNr;
        this.stornoVon = stv;
        this.kassierArtikel = ka;
        this.mwstValues = mv;
        this.zahlungsModus = zm;
        this.totalPrice = tp;
        this.kundeGibt = kgb; this.rueckgeld = rg;
        this.tx = transaction;
        this.tseStatusValues = tseStatusValues;

        // Create a list from the set of mwst values
        this.mwstList = new Vector<BigDecimal>(this.mwstValues.size());
        for ( BigDecimal vat : this.mwstValues.keySet() ) {
            this.mwstList.add(vat);
        }

        // printQuittungWithEscPos();
        // writeQuittungToDeviceFile();
    }

    private String saveSpaceInMenge(String menge) {
        menge = menge.replaceAll(" ", "");
        return menge.replaceAll("St\\.", "St");
    }

    /*
        Unfortunately, font size is slightly too large, so we cannot fit much data on the paper with ESC/POS.
        Writing to the device file (see function below) yields smaller font, but only if one never
        uses ESC/POS while printer is on.
    */
    private void printQuittungWithEscPos() {
        // First test of ESC/POS printing
        PrintService printService = PrinterOutputStream.getPrintServiceByName(bc.printerName);
        EscPos escpos;
        try {
            escpos = new EscPos(new PrinterOutputStream(printService));
            
            // Unfortunately, printer does not react to the more fine-grained "Style" commands, but only to PrintModeStyle commands:
            // Style smallStyle = new Style()
            //     .setFontSize(Style.FontSize._1, Style.FontSize._1);
            // Style bigStyle = new Style()
            //     .setFontSize(Style.FontSize._8, Style.FontSize._8);
            
            // for German umlauts etc.:
            escpos.setCharacterCodeTable(CharacterCodeTable.CP858_Euro);
            // if it does not work, use this code to explicitly set to CP858:
            // escpos.setPrinterCharacterTable(19);
            // escpos.setCharsetName("cp858");

            printEscPosHeader(escpos);
            printEscPosItems(escpos);
            printEscPosTotals(escpos);
            printEscPosTSEValues(escpos);
            
            escpos.feed(6).cut(EscPos.CutMode.FULL);
            escpos.close();

        } catch (IOException ex) {
            logger.error("{}", ex);
        }
    }

    private String centerString(String text, int nCharRow) {
        if (text.length() > nCharRow) {
            return text.substring(0, nCharRow);
        }
        if (text.length() == nCharRow) {
            return text;
        }
        int spaces = nCharRow - text.length();
        int spacesInFront = spaces / 2; // this should always round down, so that with odd numbers, spaces before are one less than behind
        int spacesBehind = spaces - spacesInFront;
        return " ".repeat(spacesInFront) + text + " ".repeat(spacesBehind);
    }

    private String centerString(String text) {
        return centerString(text, rowLength);
    }

    private String leftAlignedString(String text, int nCharRow) {
        if (text.length() > nCharRow) {
            return text.substring(0, nCharRow);
        }
        if (text.length() == nCharRow) {
            return text;
        }
        int spaces = nCharRow - text.length();
        return text + " ".repeat(spaces);
    }

    private String leftAlignedString(String text) {
        return leftAlignedString(text, rowLength);
    }

    private String rightAlignedString(String text, int nCharRow) {
        if (text.length() > nCharRow) {
            return text.substring(0, nCharRow);
        }
        if (text.length() == nCharRow) {
            return text;
        }
        int spaces = nCharRow - text.length();
        return " ".repeat(spaces) + text;
    }

    private String rightAlignedString(String text) {
        return rightAlignedString(text, rowLength);
    }

    private String columnStrings(int[] colWidths, String[] alignments, String[] strings, int nCharRow) {
        String result = "";
        for (int i = 0; i < colWidths.length; i++) {
            if (i > 0) result += " ";
            result += alignments[i].equals("r") ? rightAlignedString(strings[i], colWidths[i]) :
                alignments[i].equals("c") ? centerString(strings[i], colWidths[i]) :
                leftAlignedString(strings[i], colWidths[i]);
        }
        return leftAlignedString(result, nCharRow);
    }

    private String columnStrings(int[] colWidths, String[] alignments, String[] strings) {
        return columnStrings(colWidths, alignments, strings, rowLength);
    }

    private String columnStrings(int[] colWidths, String[] strings, int nCharRow) {
        String[] alignments = "r".repeat(strings.length).split("");
        return columnStrings(colWidths, alignments, strings, nCharRow);
    }

    private String columnStrings(int[] colWidths, String[] strings) {
        return columnStrings(colWidths, strings, rowLength);
    }

    private String dividerLine() {
        return "-".repeat(rowLength);
    }

    private String spaceBetweenStrings(String stringLeft, String stringRight, int nCharRow) {
        int nLeft = stringLeft.length();
        int nRight = stringRight.length();
        if (nLeft + nRight > nCharRow) {
            return stringLeft + stringRight.substring(0, nCharRow - nLeft);
        }
        if (nLeft + nRight == nCharRow) {
            return stringLeft + stringRight;
        }
        int spaces = nCharRow - nLeft - nRight;
        return stringLeft + " ".repeat(spaces) + stringRight;
    }

    private String spaceBetweenStrings(String stringLeft, String stringRight) {
        return spaceBetweenStrings(stringLeft, stringRight, rowLength);
    }

    private void printEscPosHeader(EscPos escpos) throws IOException, UnsupportedEncodingException {
        // Use maximum width printer can handle (31 chars with indent of 9 empty spaces in front to get text onto paper)
        escpos.writeLF(bold,   indent + centerString(shopName));
        escpos.writeLF(normal, indent + centerString(shopAddress));
        escpos.writeLF(normal, indent + centerString(shopPhone));
        escpos.writeLF(normal, indent + centerString(shopURL));
        escpos.writeLF(normal, indent + dividerLine());
        String datetimeStr = (datetime != null) ? datetime.format(bc.dateFormatDate4j) : "";
        escpos.writeLF(normal, indent + centerString(datetimeStr));
        String rechnungsNrStr = (rechnungsNr != null) ? rechnungsNr.toString() : "";
        escpos.writeLF(normal, indent + spaceBetweenStrings(
            "Rechnungsnummer:", rechnungsNrStr, rowLength
        ));
        escpos.writeLF(normal, indent + dividerLine());
        escpos.writeLF(normal, indent + leftAlignedString("Bezeichnung"));
        escpos.writeLF(normal, indent + columnStrings(
            new int[] {7, 6, 6, 7, 1},
            new String[] {"Menge", "Stk", "Einzel", "Ges. "+bc.currencySymbol, "M"}
        ));
        escpos.writeLF(normal, indent + dividerLine());

        logger.debug("{}", indent + centerString(shopName));
        logger.debug("{}", indent + centerString(shopAddress));
        logger.debug("{}", indent + centerString(shopPhone));
        logger.debug("{}", indent + centerString(shopURL));
        logger.debug("{}", indent + dividerLine());
        logger.debug("{}", indent + centerString(datetimeStr));
        logger.debug("{}", indent + spaceBetweenStrings(
            "Rechnungsnummer:", rechnungsNrStr, rowLength
        ));
        logger.debug("{}", indent + dividerLine());
        logger.debug("{}", indent + leftAlignedString("Bezeichnung"));
        logger.debug("{}", indent + columnStrings(
            new int[] {7, 6, 6, 7, 1},
            new String[] {"Menge", "Stk", "Einzel", "Ges. "+bc.currencySymbol, "M"}
        ));
        logger.debug("{}", indent + dividerLine());
    }

    private void printEscPosItems(EscPos escpos) throws IOException, UnsupportedEncodingException {
        for (KassierArtikel ka : kassierArtikel) {
            Integer mwstIndex = mwstList.indexOf(ka.getMwst()) + 1;
            if (ka.getType().equals("gutschein") || ka.getType().equals("rabattrechnung") ||
                ka.getType().equals("anzahlung") || ka.getType().equals("anzahlungsaufloesung")) {
                // display compact in one line
                escpos.writeLF(normal, indent + columnStrings(
                    new int[] {21, 7, 1},
                    new String[] {"l", "r", "r"},
                    new String[] {
                        ka.getName(),
                        bc.priceFormatter(ka.getGesPreis()),
                        mwstIndex.toString()
                    }
                ));
            } else {
                // normaler Artikel: display extended in two lines
                escpos.writeLF(normal, indent + leftAlignedString(ka.getName(), bezLaenge));
                escpos.writeLF(normal, indent + columnStrings(
                    new int[] {7, 6, 6, 7, 1},
                    new String[] {
                        saveSpaceInMenge(ka.getMenge()),
                        ka.getStueckzahl().toString() + " x",
                        bc.priceFormatter(ka.getEinzelPreis()),
                        ka.getPartOfAnzahlung() ? "" : bc.priceFormatter(ka.getGesPreis()),
                        mwstIndex.toString()
                    }
                ));
            }
        }

        for (KassierArtikel ka : kassierArtikel) {
            Integer mwstIndex = mwstList.indexOf(ka.getMwst()) + 1;
            if (ka.getType().equals("gutschein") || ka.getType().equals("rabattrechnung") ||
                ka.getType().equals("anzahlung") || ka.getType().equals("anzahlungsaufloesung")) {
                // display compact in one line
                logger.debug("{}", indent + columnStrings(
                    new int[] {21, 7, 1},
                    new String[] {"l", "r", "r"},
                    new String[] {
                        ka.getName(),
                        bc.priceFormatter(ka.getGesPreis()),
                        mwstIndex.toString()
                    }
                ));
            } else {
                // normaler Artikel
                logger.debug("{}", indent + leftAlignedString(ka.getName(), bezLaenge));
                logger.debug("{}", indent + columnStrings(
                    new int[] {7, 6, 6, 7, 1},
                    new String[] {
                        saveSpaceInMenge(ka.getMenge()),
                        ka.getStueckzahl().toString() + " x",
                        bc.priceFormatter(ka.getEinzelPreis()),
                        ka.getPartOfAnzahlung() ? "" : bc.priceFormatter(ka.getGesPreis()),
                        mwstIndex.toString()
                    }
                ));
            }
        }

        // escpos.writeLF(normal,     indent + "Pfand 0,15 Euro                ");
        // escpos.writeLF(normal,     indent + "          -1 x   0,15   -0,15 2");
        // escpos.writeLF(normal,     indent + "Örangensaft 1 l                ");
        // escpos.writeLF(normal,     indent + "     1l    1 x   2,90    2,90 2");
        // escpos.writeLF(normal,     indent + "   Pfand 0,15 Euro             ");
        // escpos.writeLF(normal,     indent + "           1 x   0,15    0,15 2");
        // escpos.writeLF(normal,     indent + "Schoko Crispies                ");
        // escpos.writeLF(normal,     indent + "   100g    1 x   2,70    2,70 1");
        // escpos.writeLF(normal,     indent + "Rooibos Good Friends, mit Zimt ");
        // escpos.writeLF(normal,     indent + "   100g    1 x   5,00    5,00 1");
        // escpos.writeLF(normal,     indent + "Mascobado Weiße Schokolade     ");
        // escpos.writeLF(normal,     indent + "   100g    1 x   2,00    2,00 1");
        // escpos.writeLF(normal,     indent + "Hom Mali Jasminreis            ");
        // escpos.writeLF(normal,     indent + "    1kg    1 x   5,50    5,50 1");
        // escpos.writeLF(normal,     indent + "Rabatt auf Rechnung            ");
        // escpos.writeLF(normal,     indent + "                        -0,29 2");
        // escpos.writeLF(normal,     indent + "Rabatt auf Rechnung            ");
        // escpos.writeLF(normal,     indent + "                        -1,52 1");
    }

    private void printEscPosTotals(EscPos escpos) throws IOException, UnsupportedEncodingException {
        escpos.writeLF(normal, indent + dividerLine());
        escpos.writeLF(normal, indent + columnStrings(
            new int[] {5, 7, 7, 7, 1},
            new String[] {"l", "r", "r", "r", "r"},
            new String[] {"MwSt.", "Netto", "Steuer", "Umsatz", "M"}
        ));
        Integer mwstIndex = 1;
        for (Map.Entry<BigDecimal, Vector<BigDecimal>> entry : mwstValues.entrySet()) {
            BigDecimal steuersatz = entry.getKey();
            Vector<BigDecimal> values = entry.getValue();
            escpos.writeLF(normal, indent + columnStrings(
                new int[] {5, 7, 7, 7, 1},
                new String[] {"l", "r", "r", "r", "r"},
                new String[] {
                    bc.vatFormatter(steuersatz),
                    bc.priceFormatter(values.get(0)), // Netto (see calculateMwStValuesInRechnung() in RechnungsGrundlage.java)
                    bc.priceFormatter(values.get(1)), // Steuer
                    bc.priceFormatter(values.get(2)), // Brutto/Umsatz
                    mwstIndex.toString()
                }
            ));
            mwstIndex++;
        }
        escpos.feed(1);
        if ( zahlungsModus.equals("bar") ) {
            escpos.writeLF(boldlarger, indent + " " + columnStrings(
                new int[] {10, 12},
                new String[] {"l", "r"},
                new String[] {"BAR", bc.priceFormatter(totalPrice)+" "+bc.currencySymbol}
            ));
            if (kundeGibt != null && rueckgeld != null) {
                escpos.writeLF(boldlarger, indent + " " + columnStrings(
                    new int[] {10, 12},
                    new String[] {"l", "r"},
                    new String[] {"Kunde gibt", bc.priceFormatter(kundeGibt)+" "+bc.currencySymbol}
                ));
                escpos.writeLF(boldlarger, indent + " " + columnStrings(
                    new int[] {10, 12},
                    new String[] {"l", "r"},
                    new String[] {"Rückgeld", bc.priceFormatter(rueckgeld)+" "+bc.currencySymbol}
                ));
            }
        } else if ( zahlungsModus.equals("ec") ){
            escpos.writeLF(boldlarger, indent + " " + columnStrings(
                new int[] {10, 12},
                new String[] {"l", "r"},
                new String[] {"EC", bc.priceFormatter(totalPrice)+" "+bc.currencySymbol}
            ));
        }
        escpos.feed(1);
        if (stornoVon != null) {
            escpos.writeLF(normal, indent + leftAlignedString("!!! STORNO VON !!!"));
            escpos.writeLF(normal, indent + leftAlignedString("!!! RECHN.-NR. "+stornoVon+" !!!"));
            escpos.feed(1);
        }

        logger.debug("{}", indent + dividerLine());
        logger.debug("{}", indent + columnStrings(
            new int[] {5, 7, 7, 7, 1},
            new String[] {"l", "r", "r", "r", "r"},
            new String[] {"MwSt.", "Netto", "Steuer", "Umsatz", "M"}
        ));
        mwstIndex = 1;
        for (Map.Entry<BigDecimal, Vector<BigDecimal>> entry : mwstValues.entrySet()) {
            BigDecimal steuersatz = entry.getKey();
            Vector<BigDecimal> values = entry.getValue();
            logger.debug("{}", indent + columnStrings(
                new int[] {5, 7, 7, 7, 1},
                new String[] {"l", "r", "r", "r", "r"},
                new String[] {
                    bc.vatFormatter(steuersatz),
                    bc.priceFormatter(values.get(0)), // Netto (see calculateMwStValuesInRechnung() in RechnungsGrundlage.java)
                    bc.priceFormatter(values.get(1)), // Steuer
                    bc.priceFormatter(values.get(2)), // Brutto/Umsatz
                    mwstIndex.toString()
                }
            ));
            mwstIndex++;
        }
        logger.debug("");
        if ( zahlungsModus.equals("bar") ) {
            logger.debug("{}", indent + " " + columnStrings(
                new int[] {10, 12},
                new String[] {"l", "r"},
                new String[] {"BAR", bc.priceFormatter(totalPrice)+" "+bc.currencySymbol}
            ));
            if (kundeGibt != null && rueckgeld != null) {
                logger.debug("{}", indent + " " + columnStrings(
                    new int[] {10, 12},
                    new String[] {"l", "r"},
                    new String[] {"Kunde gibt", bc.priceFormatter(kundeGibt)+" "+bc.currencySymbol}
                ));
                logger.debug("{}", indent + " " + columnStrings(
                    new int[] {10, 12},
                    new String[] {"l", "r"},
                    new String[] {"Rückgeld", bc.priceFormatter(rueckgeld)+" "+bc.currencySymbol}
                ));
            }
        } else if ( zahlungsModus.equals("ec") ){
            logger.debug("{}", indent + " " + columnStrings(
                new int[] {10, 12},
                new String[] {"l", "r"},
                new String[] {"EC", bc.priceFormatter(totalPrice)+" "+bc.currencySymbol}
            ));
        }
        logger.debug("");
        if (stornoVon != null) {
            logger.debug("{}", indent + leftAlignedString("!!! STORNO VON !!!"));
            logger.debug("{}", indent + leftAlignedString("!!! RECHN.-NR. "+stornoVon+" !!!"));
            logger.debug("");
        }
    }

    private void printEscPosTSEValues(EscPos escpos) throws IOException, UnsupportedEncodingException {
        escpos.writeLF(normal, indent + "--- TSE ---                    ");
        escpos.writeLF(normal, indent + "Transaktionsnr:              88");
        escpos.writeLF(normal, indent + "Start:  2021-01-20T18:43:00.000");
        escpos.writeLF(normal, indent + "Ende:   2021-01-20T18:44:09.000");
        escpos.writeLF(normal, indent + "Kassen-Seriennr (clientID):    ");
        escpos.writeLF(normal, indent + "                877666797878-01");
        escpos.writeLF(normal, indent + "TSE-Seriennr:  4a3f03a2dec81878");
        escpos.writeLF(normal, indent + "       b432548668f603d14f7b7f90");
        escpos.writeLF(normal, indent + "       d230e30c87c1a705dce1c890");
    }

    public void printReceipt() {
        // printQuittungWithSofficeTemplate();
        printQuittungWithEscPos();
    }

    // private void writeQuittungToDeviceFile() {
    //     File file = new File(bc.printerDeviceFile);
    //     if (file.exists()) {
    //         logger.debug("Trying to write receipt data to printer device file {}...", bc.printerDeviceFile);

    //         String quittungStr = "";
    //         // Old format (one-line):
    //         // quittungStr += "                  WELTLADEN BONN        " + bc.lineSep;
    //         // quittungStr += "            Maxstrasse 36, 53111 Bonn   " + bc.lineSep;
    //         // quittungStr += "             Telefon: 0228 / 69 70 52   " + bc.lineSep;
    //         // quittungStr += "              www.weltladen-bonn.org    " + bc.lineSep;
    //         // quittungStr += "         -------------------------------" + bc.lineSep;
    //         // quittungStr += "              20.01.2021, 18:44 Uhr     " + bc.lineSep;
    //         // quittungStr += "         Rechnungsnummer:          34149" + bc.lineSep;
    //         // quittungStr += "         -------------------------------" + bc.lineSep;
    //         // quittungStr += "         Bezeichnung       Betrag Euro M" + bc.lineSep;
    //         // quittungStr += "         -------------------------------" + bc.lineSep;
    //         // quittungStr += "         Pfand 0,15 Euro         -0,15 2" + bc.lineSep;
    //         // quittungStr += "         Orangensaft 1 l          2,90 2" + bc.lineSep;
    //         // quittungStr += "            Pfand 0,15 Euro       0,15 2" + bc.lineSep;
    //         // quittungStr += "         Schoko Crispies          2,70 1" + bc.lineSep;
    //         // quittungStr += "         Rooibos Good Friends     5,00 1" + bc.lineSep;
    //         // quittungStr += "         Mascobado Weisse Schokol 2,00 1" + bc.lineSep;
    //         // quittungStr += "         Hom Mali Jasminreis      5,50 1" + bc.lineSep;
    //         // quittungStr += "         Rabatt auf Rechnung     -0,29 2" + bc.lineSep;
    //         // quittungStr += "         Rabatt auf Rechnung     -1,52 1" + bc.lineSep;
    //         // quittungStr += "         -------------------------------" + bc.lineSep;
    //         // quittungStr += "         MwSt.   Netto  Steuer  Umsatz  " + bc.lineSep;
    //         // quittungStr += "         7%      12,79    0,89   13,68 1" + bc.lineSep;
    //         // quittungStr += "         19%      2,19    0,42    2,61 2" + bc.lineSep;
    //         // quittungStr += "                                        " + bc.lineSep;
    //         // quittungStr += "         BAR                16,29 Euro  " + bc.lineSep;
    //         // quittungStr += "         Kunde gibt         17,09 Euro  " + bc.lineSep;
    //         // quittungStr += "         Rueckgeld           0,80 Euro  " + bc.lineSep;
    //         // quittungStr += "                                        " + bc.lineSep;
    //         // quittungStr += "         --- TSE ---				    " + bc.lineSep;
    //         // quittungStr += "         Transaktionsnr:              88" + bc.lineSep;
    //         // quittungStr += "         Start:  2021-01-20T18:43:00.000" + bc.lineSep;
    //         // quittungStr += "         Ende:   2021-01-20T18:44:09.000" + bc.lineSep;
    //         // quittungStr += "         Kassen-Seriennr (clientID):    " + bc.lineSep;
    //         // quittungStr += "                         877666797878-01" + bc.lineSep;
    //         // quittungStr += "         TSE-Seriennr:  4a3f03a2dec81878" + bc.lineSep;
    //         // quittungStr += "                b432548668f603d14f7b7f90" + bc.lineSep;
    //         // quittungStr += "                d230e30c87c1a705dce1c890" + bc.lineSep;

    //         // New format (two-line):
    //         quittungStr += "                  WELTLADEN BONN        " + bc.lineSep;
    //         quittungStr += "            Maxstrasse 36, 53111 Bonn   " + bc.lineSep;
    //         quittungStr += "             Telefon: 0228 / 69 70 52   " + bc.lineSep;
    //         quittungStr += "              www.weltladen-bonn.org    " + bc.lineSep;
    //         quittungStr += "         -------------------------------" + bc.lineSep;
    //         quittungStr += "              20.01.2021, 18:44 Uhr     " + bc.lineSep;
    //         quittungStr += "         Rechnungsnummer:          34149" + bc.lineSep;
    //         quittungStr += "         -------------------------------" + bc.lineSep;
    //         quittungStr += "         Bezeichnung       Betrag Euro M" + bc.lineSep;
    //         quittungStr += "         -------------------------------" + bc.lineSep;
    //         quittungStr += "         Pfand 0,15 Euro                " + bc.lineSep;
    //         quittungStr += "                   -1 x   0,15   -0,15 2" + bc.lineSep;
    //         quittungStr += "         Orangensaft 1 l                " + bc.lineSep;
    //         quittungStr += "              1l    1 x   2,90    2,90 2" + bc.lineSep;
    //         quittungStr += "            Pfand 0,15 Euro             " + bc.lineSep;
    //         quittungStr += "                    1 x   0,15    0,15 2" + bc.lineSep;
    //         quittungStr += "         Schoko Crispies                " + bc.lineSep;
    //         quittungStr += "            100g    1 x   2,70    2,70 1" + bc.lineSep;
    //         quittungStr += "         Rooibos Good Friends, mit Zimt " + bc.lineSep;
    //         quittungStr += "            100g    1 x   5,00    5,00 1" + bc.lineSep;
    //         quittungStr += "         Mascobado Weisse Schokolade    " + bc.lineSep;
    //         quittungStr += "            100g    1 x   2,00    2,00 1" + bc.lineSep;
    //         quittungStr += "         Hom Mali Jasminreis            " + bc.lineSep;
    //         quittungStr += "             1kg    1 x   5,50    5,50 1" + bc.lineSep;
    //         quittungStr += "         Rabatt auf Rechnung            " + bc.lineSep;
    //         quittungStr += "                                 -0,29 2" + bc.lineSep;
    //         quittungStr += "         Rabatt auf Rechnung            " + bc.lineSep;
    //         quittungStr += "                                 -1,52 1" + bc.lineSep;
    //         quittungStr += "         -------------------------------" + bc.lineSep;
    //         quittungStr += "         MwSt.   Netto  Steuer  Umsatz  " + bc.lineSep;
    //         quittungStr += "         7%      12,79    0,89   13,68 1" + bc.lineSep;
    //         quittungStr += "         19%      2,19    0,42    2,61 2" + bc.lineSep;
    //         quittungStr += "                                        " + bc.lineSep;
    //         quittungStr += "         BAR                16,29 Euro  " + bc.lineSep;
    //         quittungStr += "         Kunde gibt         17,09 Euro  " + bc.lineSep;
    //         quittungStr += "         Rueckgeld           0,80 Euro  " + bc.lineSep;
    //         quittungStr += "                                        " + bc.lineSep;
    //         quittungStr += "         --- TSE ---                    " + bc.lineSep;
    //         quittungStr += "         Transaktionsnr:              88" + bc.lineSep;
    //         quittungStr += "         Start:  2021-01-20T18:43:00.000" + bc.lineSep;
    //         quittungStr += "         Ende:   2021-01-20T18:44:09.000" + bc.lineSep;
    //         quittungStr += "         Kassen-Seriennr (clientID):    " + bc.lineSep;
    //         quittungStr += "                         877666797878-01" + bc.lineSep;
    //         quittungStr += "         TSE-Seriennr:  4a3f03a2dec81878" + bc.lineSep;
    //         quittungStr += "                b432548668f603d14f7b7f90" + bc.lineSep;
    //         quittungStr += "                d230e30c87c1a705dce1c890" + bc.lineSep;
    //         quittungStr += bc.lineSep;
    //         quittungStr += bc.lineSep;
    //         quittungStr += bc.lineSep;
    //         quittungStr += bc.lineSep;
    //         quittungStr += bc.lineSep;
    //         quittungStr += bc.lineSep;
    //         quittungStr += bc.lineSep;

    //         BufferedWriter writer = null;
    //         try {
    //             // Use this for German umlauts:
    //             // file.write("#$@°\\è^ùàòèì\n".getBytes("Cp858"));
    //             writer = new BufferedWriter(new FileWriter(bc.printerDeviceFile));
    //             writer.write(quittungStr);
    //         } catch (Exception ex) {
    //             logger.error("Error writing to file {}", file.getName());
    //             logger.error("Exception:", ex);
    //         } finally {
    //             try {
    //                 // Close the writer regardless of what happens...
    //                 writer.close();
    //             } catch (Exception ex) {
    //                 logger.error("Error closing file {}", file.getName());
    //                 logger.error("Exception:", ex);
    //             }
    //         }
    //     } else {
    //         logger.warn("Printer device file {} does not exist, cannot print receipt!!! " +
    //             "Printer disconnected?", bc.printerDeviceFile);
    //     }
    // }

private Sheet createSheetFromTemplate() {
        final Sheet sheet;
        try {
            String filename = "vorlagen"+bc.fileSep+"Quittung.ods";
            File infile = new File(filename);
            if (!infile.exists()){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Quittungsvorlage "+
                        "'"+filename+"' nicht gefunden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            sheet = SpreadSheet.createFromFile(infile).getSheet(0);
        } catch (IOException ex) {
            logger.error("Exception:", ex);
            return null;
        }
        return sheet;
    }

    private void editHeader(Sheet sheet) {
        // if this is not the first page:
        rowOffset = 7;
        if (artikelIndex > 0){
            // Delete header
            sheet.removeRows(0, rowOffset); // last row is exclusive
            rowOffset = 0;
        } else {
            // Fill header
            if (datetime != null) {
                sheet.getCellAt("A5").setValue(datetime.format(bc.dateFormatDate4j));
            }
            if (rechnungsNr != null) {
                sheet.getCellAt("D6").setValue(rechnungsNr);
            } else {
                sheet.getCellAt("A6").setValue("");
            }
            rowOffset = 7;
        }
    }

    private int insertItems(Sheet sheet) {
        int row = rowOffset; // start here in ods document
        for (int i=artikelIndex; i<kassierArtikel.size(); i++){
            sheet.setValueAt(kassierArtikel.get(i).getName(), 0, row); // name on full row
            row++; // price infos on next row:
            sheet.setValueAt(saveSpaceInMenge(kassierArtikel.get(i).getMenge()), 0, row);
            sheet.setValueAt(kassierArtikel.get(i).getStueckzahl().toString() + " x", 1, row);
            sheet.setValueAt(kassierArtikel.get(i).getEinzelPreis(), 2, row);
            sheet.setValueAt(kassierArtikel.get(i).getGesPreis(), 3, row);
            Integer mwstIndex = mwstList.indexOf(kassierArtikel.get(i).getMwst()) + 1;
            sheet.setValueAt(mwstIndex, 4, row);
            row++;
            artikelIndex = i+1; // index of next item
            // if list becomes too long: print this sheet and create new sheet
            if (row >= 200+rowOffset){
                break;
            }
        }
        return row; // first empty row
    }

    private Integer editFooter(Sheet sheet) {
        Integer row = 0;
        // if this is not the last page:
        if (artikelIndex < kassierArtikel.size()){
            // Delete footer
            sheet.removeRow(201+rowOffset);
            row = null;
        } else {
            // Fill footer
            if ( zahlungsModus.equals("bar") ){
                sheet.setValueAt("Bar", 0, 201+rowOffset);
                if (kundeGibt != null && rueckgeld != null){
                    sheet.setValueAt("Kunde gibt", 0, 202+rowOffset);
                    sheet.setValueAt("Rückgeld", 0, 203+rowOffset);
                    sheet.setValueAt(kundeGibt, 3, 202+rowOffset);
                    sheet.setValueAt(rueckgeld, 3, 203+rowOffset);
                }
            } else if ( zahlungsModus.equals("ec") ){
                sheet.setValueAt("EC", 0, 201+rowOffset);
            }
            if (stornoVon != null) {
                sheet.setValueAt("!!! STORNO VON !!!", 0, 202+rowOffset);
                sheet.setValueAt("!!! RECHN.-NR. "+stornoVon+" !!!", 0, 203+rowOffset);
            }
            sheet.setValueAt(totalPrice, 2, 201+rowOffset);
            // fill mwst values
            row = 205+rowOffset; // now at header of mwst values
            sheet.setValueAt("Enthaltene MwSt.:", 0, row);
            row++;
            sheet.setValueAt("Satz", 0, row);
            sheet.setValueAt("Netto", 1, row);
            sheet.setValueAt("Steuer", 2, row);
            sheet.setValueAt("Brutto", 3, row);
            row++;
            Integer mwstIndex = 1;
            for (Map.Entry<BigDecimal, Vector<BigDecimal>> entry : mwstValues.entrySet()) {
                BigDecimal steuersatz = entry.getKey();
                Vector<BigDecimal> values = entry.getValue();
                sheet.setValueAt(bc.vatFormatter(steuersatz), 0, row);
                int col = 1;
                for (int i = 0; i < values.size(); i++) {
                    BigDecimal val = values.get(i);
                    sheet.setValueAt(val, col, row);
                    col++;
                }
                sheet.setValueAt(mwstIndex, col, row);
                row++;
                mwstIndex++;
            }
            if ( zahlungsModus.equals("ec") && stornoVon == null ){
                // Delete rows holding "Kunde gibt" and "Rückgeld" in case of Bar (in case of EC empty)
                sheet.removeRows(202+rowOffset, 204+rowOffset); // last row is exclusive
                row -= 2;
            }
        }
        return row; // first empty row
    }

    private int spreadTextOverSeveralRows(Sheet sheet, String text, int col, int startRow, int firstBreakAfterChar, int charsPerRow) {
        int row = startRow;
        int pos = 0;
        int textLength = text.length();
        String putText = text.substring(pos, firstBreakAfterChar > textLength ? textLength : firstBreakAfterChar);
        sheet.setValueAt(putText, col, row);
        pos = firstBreakAfterChar;
        row++;
        while (textLength > pos) {
            putText = text.substring(pos, pos + charsPerRow > textLength ? textLength : pos + charsPerRow);
            sheet.setValueAt(putText, col, row);
            pos = pos + charsPerRow;
            row++;
        }
        return row;
    }

    private void insertTSEValues(Sheet sheet, int continueAtRow) {
        int row = continueAtRow + 1; // leave one row empty for spacing
        if (tx != null && tx.tseError != null) { // means TSE has failed
            sheet.setValueAt("--- TSE ausgefallen!!! ---", 0, row);
        } else {
            sheet.setValueAt("--- TSE ---", 0, row);

        }
        row++;
        if (tx == null) {
            sheet.setValueAt("TSE-Daten nicht verfügbar", 0, row);
        } else {
            sheet.setValueAt("Start:", 0, row);
            String dateString = "???";
            try {
                Date date = new SimpleDateFormat(WeltladenTSE.dateFormatDSFinVK).parse(tx.startTimeString);
                dateString = new SimpleDateFormat(WeltladenTSE.dateFormatQuittung).format(date);
            } catch (ParseException ex) {
                logger.error("{}", ex);
            }
            sheet.setValueAt(dateString, 4, row);
            row++;
            sheet.setValueAt("Ende:", 0, row);
            dateString = "???";
            try {
                Date date = new SimpleDateFormat(WeltladenTSE.dateFormatDSFinVK).parse(tx.endTimeString);
                dateString = new SimpleDateFormat(WeltladenTSE.dateFormatQuittung).format(date);
            } catch (ParseException ex) {
                logger.error("{}", ex);
            }
            sheet.setValueAt(dateString, 4, row);
            row++;
            if (tx.tseError == null) { // if TSE has not failed
                sheet.setValueAt("Transaktionsnr.:", 0, row);
                sheet.setValueAt(tx.txNumber, 4, row);
                row++;
                sheet.setValueAt("Kassen-Seriennr. (clientID):", 0, row);
                row++;
                sheet.setValueAt(bc.Z_KASSE_ID, 4, row);
                if (tseStatusValues != null) {
                    row++;
                    sheet.setValueAt("TSE-Seriennr.:", 0, row);
                    row = spreadTextOverSeveralRows(
                        sheet, tseStatusValues.get("Seriennummer der TSE (Hex)"),
                        4, row, 10, 30
                    );
                }
            }
        }
    }

    void printQuittungFromJava(File tmpFile) {
        /** Complicated method: printing from Java (doesn't work) */
        final OpenDocument doc = new OpenDocument();
        doc.loadFrom(tmpFile);
        //doc.loadFrom(new File("/tmp/Untitled.ods"));

        // Print.
        ODTPrinter printer = new ODTPrinter(doc);
        PrinterJob job = PrinterJob.getPrinterJob();

        // Get a handle on the printer, by requiring a printer with the given name
        PrintServiceAttributeSet attrSet = new HashPrintServiceAttributeSet();
        attrSet.add(new PrinterName(bc.printerName, null));
        PrintService[] pservices = PrintServiceLookup.lookupPrintServices(null, attrSet);
        PrintService ps;
        try {
            ps = pservices[0];
            job.setPrintService(ps);   // Try setting the printer you want
        } catch (ArrayIndexOutOfBoundsException e){
            System.err.println("Error: No printer named '"+bc.printerName+"', using default printer.");
        } catch (PrinterException exception) {
            System.err.println("Printing error: " + exception);
        }
        PageFormat pageFormat = job.defaultPage();
        Paper paper = pageFormat.getPaper();
        paper.setSize(76. / 25.4 * 72., 279.4 / 25.4 * 72.);
        paper.setImageableArea(8. / 25.4 * 72., 3. / 25.4 * 72., 68. / 25.4 * 72., 259.4 / 25.4 * 72.);
        pageFormat.setPaper(paper);

        // Book with setPageable instead of setPrintable
        Book book = new Book();//java.awt.print.Book
        book.append(printer, pageFormat);
        job.setPageable(book);

        try {
            job.print();   // Actual print command
        } catch (PrinterException exception) {
            System.err.println("Printing error: " + exception);
        }
    }

    private void printQuittungWithSoffice(String filename) {
        /** Simple method: printing with openoffice command line tool 'soffice' */
        String program = constructProgramPath(bc.sofficePath, "soffice");
        String[] executeCmd = new String[] {program, "--headless",
            "--pt", bc.printerName, filename};
        String log = "Print command: ";
        for (String s : executeCmd){
            log += s+" ";
        }
        logger.info(log);
        try {
            Runtime shell = Runtime.getRuntime();
            Process proc = shell.exec(executeCmd);
            int processComplete = proc.waitFor();
            if (processComplete == 0) {
                logger.info("Printing of file '"+filename+"' with soffice was successful");
            } else {
                logger.info("Could not print file '"+filename+"' with soffice");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void printQuittungWithSofficeTemplate() {
        artikelIndex = 0;
        while (artikelIndex < kassierArtikel.size()) {
            // Create a new sheet from the template file
            final Sheet sheet = createSheetFromTemplate();
            if (sheet == null) return;

            editHeader(sheet);

            // Insert items
            int startRemRow = insertItems(sheet);
            int endRemRow = 200+rowOffset; // delete normally up to row 207
            
            Integer continueAtRow = editFooter(sheet);

            if (continueAtRow != null) { // if this is the last file written in case of multiple files
                if (tx != null && tx.training) { // means transaction was done in training mode
                    continueAtRow++; // leave one row empty for spacing
                    sheet.setValueAt("!!! TRAININGSMODUS !!!", 0, continueAtRow);
                    continueAtRow++;
                }
                insertTSEValues(sheet, continueAtRow);
            }

            // Remove all empty rows between item list and footer
            sheet.removeRows(startRemRow, endRemRow); // last row is exclusive

            try {
                // Save to temp file.
                File tmpFile = File.createTempFile("Quittung", ".ods");
                //OOUtils.open(sheet.getSpreadSheet().saveAs(tmpFile)); // for testing
                sheet.getSpreadSheet().saveAs(tmpFile);

                // Complicated method: printing from Java (doesn't work)
                //printQuittungFromJava(tmpFile);

                // Simple method: printing with openoffice command line tool 'soffice'
                printQuittungWithSoffice(tmpFile.getAbsolutePath());

                tmpFile.deleteOnExit();
                //tmpFile.delete();
            } catch (FileNotFoundException ex) {
                logger.error("Exception:", ex);
            } catch (IOException ex) {
                logger.error("Exception:", ex);
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
