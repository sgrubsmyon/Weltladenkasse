package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// GUI stuff:
import java.awt.event.ActionEvent;

public class Quittung {
    int lineLength = 31;
    int maxNameLength = 27;

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
            }
            String position = positions.get(i) != null ?
                String.format("% 3d", positions.get(i)) : "   ";
            printStr += String.format("%s %-"+maxNameLength+"s % 4dX %6.2f = %7.2f%n",
                    position, artikelName, stueckzahlen.get(i),
                     einzelpreise.get(i), preise.get(i));
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
