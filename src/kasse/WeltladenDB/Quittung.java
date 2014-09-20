package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// GUI stuff:
import java.awt.event.ActionEvent;

public class Quittung extends WindowContent {
    int maxNameLength = 20;

    /**
     *    The constructor.
     *       */
    public Quittung() {
    }

    public void printReceipt(Vector<Integer> positions, Vector<String> articleNames,
            Vector<Integer> stueckzahlen, Vector<BigDecimal> einzelpreise,
            Vector<BigDecimal> preise) {
        String printStr = new String();
        for (int i=0; i<positions.size(); i++){
            String artikelName = articleNames.get(i);
            if (artikelName.length() > maxNameLength){
                artikelName = artikelName.substring(0, maxNameLength);
            } else {
                for (int j=0; j<maxNameLength-artikelName.length(); j++){
                    artikelName += " ";
                }
            }
            printStr += positions.get(i)+" "+artikelName+" "+stueckzahlen.get(i)+"X "+
                einzelpreise.get(i)+" = "+preise.get(i) + lineSep;
        }
        System.out.println(printStr);
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
