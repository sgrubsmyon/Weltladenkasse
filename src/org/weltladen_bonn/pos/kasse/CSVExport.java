package org.weltladen_bonn.pos.kasse;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.LinkedHashMap;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.weltladen_bonn.pos.BaseClass;

public class CSVExport {
  private static final Logger logger = LogManager.getLogger(CSVExport.class);

  public static void writeToCSV(String csvFilename, HashMap<String, String> fields,
      LinkedHashMap<String, CSVColumn> colDefs, BaseClass bc,
      String colDel, String rowDel, Character decSep, Character grSep, String textEnc) {

    String csvStr = "";
    if (fields.size() > 0) {
      for (String colName : colDefs.keySet()) {
        String col = fields.get(colName);
        if (col != null) {
          // if necessary, format the column string according to index.xml specification
          CSVColumn colSpec = colDefs.get(colName);
          if (colSpec != null && colSpec.type == CSVColumnType.ALPHANUMERIC) {
            // truncate string if too long
            if (colSpec.maxLength != null && col.length() > colSpec.maxLength) {
              col = col.substring(0, colSpec.maxLength);
            }
            // escape any occurrences of the text encapsulator with double occurrence of the
            // text encapsulator
            // (this is meant for " as encapsulator, which is currently the only
            // encapsulator used)
            col = col.replaceAll(textEnc, textEnc + textEnc);
            // now encapsulate the text with the text encapsulator
            col = textEnc + col + textEnc;
          } else if (colSpec != null && colSpec.type == CSVColumnType.NUMERIC) {
            // if you also want grouping separators in integer numbers:
            // DecimalFormatSymbols mySymbols = new DecimalFormatSymbols(bc.myLocale);
            // mySymbols.setDecimalSeparator(decSep);
            // mySymbols.setGroupingSeparator(grSep);
            // DecimalFormat myFormatter = new DecimalFormat("###,###.###", mySymbols);
            // myFormatter.setRoundingMode(RoundingMode.HALF_UP);
            // if (colSpec.accuracy != null && colSpec.accuracy > 0) {
            // myFormatter.setMinimumFractionDigits(colSpec.accuracy);
            // myFormatter.setMaximumFractionDigits(colSpec.accuracy);
            // Float colFloat = Float.parseFloat(col);
            // col = myFormatter.format(colFloat);
            // } else {
            // myFormatter.setMinimumFractionDigits(0);
            // myFormatter.setMaximumFractionDigits(0);
            // Integer colInt = Integer.parseInt(col);
            // col = myFormatter.format(colInt);
            // }
            // if not:
            if (colSpec.accuracy != null && colSpec.accuracy > 0) {
              DecimalFormatSymbols mySymbols = new DecimalFormatSymbols(bc.myLocale);
              mySymbols.setDecimalSeparator(decSep);
              mySymbols.setGroupingSeparator(grSep);
              DecimalFormat myFormatter = new DecimalFormat("###,###.###", mySymbols);
              myFormatter.setRoundingMode(RoundingMode.HALF_UP);
              myFormatter.setMinimumFractionDigits(colSpec.accuracy);
              myFormatter.setMaximumFractionDigits(colSpec.accuracy);
              Float colFloat = Float.parseFloat(col);
              col = myFormatter.format(colFloat);
            }
          }
        } else {
          col = "";
        }
        csvStr += col + colDel;
      }
      // remove the very last column separator:
      csvStr = csvStr.substring(0, csvStr.length() - colDel.length());
      csvStr += rowDel;
    }

    try {
      if (!Files.exists(Path.of(csvFilename))) {
        // create the file with a header row (data start at second row, as specified in
        // index.xml: "<From>2</From>")
        String headerStr = "";
        for (String colName : colDefs.keySet()) {
          headerStr += colName + colDel;
        }
        // remove the very last column separator:
        headerStr = headerStr.substring(0, headerStr.length() - colDel.length());
        headerStr += rowDel;
        Files.writeString(Path.of(csvFilename), headerStr,
            StandardOpenOption.CREATE, // create file if not exists
            StandardOpenOption.APPEND); // append to file if exists
      }
      if (csvStr.length() > 0) {
        Files.writeString(Path.of(csvFilename), csvStr,
            StandardOpenOption.CREATE, // create file if not exists
            StandardOpenOption.APPEND); // append to file if exists
      }
    } catch (IOException ex) {
      logger.error("Error writing to file {}", csvFilename);
      logger.error("Exception:", ex);
    }
  }
}
