package org.weltladen_bonn.pos.kasse;

import java.util.*; // Map, SortedMap
import java.io.IOException;
import java.nio.charset.Charset;

// HID API to talk with Nixdorf BA63/USB display:
import com.codeminders.hidapi.*;

//import org.weltladen_bonn.pos.JNIFromJar;
import org.weltladen_bonn.pos.BaseClass;

/**
 * This class is for interaction with VFD customer display Wincor Nixdorf BA63
 * USB.
 */
public class Kundendisplay {
    private BaseClass bc;

    static {
        // it seems that this method works if the hidapi jar file is included in
        // the overall jar file!
        ClassPathLibraryLoader.loadNativeHIDLibrary();

        // or:

        // Optionally copy the files contained in the subfolders of `native` in
        // the hidapi jar file to `resources/natives` if JNIFromJar.loadLibraryFromJar()
        // is used.
    }

    private HIDManager manager;
    private HIDDevice device = null;

    /**
     *    The constructor.
     *       */
    public Kundendisplay(BaseClass bc_) {
        bc = bc_;
        listDevices();
        openDevice();
        showWelcomeScreen();
    }

    public boolean deviceWorks() {
        return (device != null);
    }

    private void listDevices() {
    /**
     * Static function to find the list of all the HID devices
     * attached to the system.
     */
        String property = System.getProperty("java.library.path");
        System.out.println(property);
        HIDManager mgr = null;
        try {
            mgr = HIDManager.getInstance();
            HIDDeviceInfo[] devs = mgr.listDevices();
            System.out.println("HID (Display) Devices:");
            if (devs != null) {
                for (int i=0; i<devs.length; i++) {
                    System.out.println(i+":\t"+devs[i]);
                    System.out.print("Manufacturer: " + devs[i].getManufacturer_string() + "\n");
                    System.out.print("Product: " + devs[i].getProduct_string() + "\n");
                    System.out.print("Serial Number: " + devs[i].getSerial_number() + "\n");
                    System.out.println("---------------------------------------------\n");
                }
            } else {
                System.out.println("No devices found.");
            }
        } catch(IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch(NullPointerException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.err.println("It seems that the customer display is not connected. (No HID devices found.)");
        } finally {
            if (null != mgr) {
                mgr.release();
            }
            System.gc();
        }
    }

    private void openDevice() {
        /**
         * Function to open connection to HID device VFD display Nixdorf
         * BA63 USB. Must be called before starting to work with device.
         */
        try {
            manager = HIDManager.getInstance();
            HIDDeviceInfo[] devs = manager.listDevices();
            if (devs != null) {
                Vector<String> paths = new Vector<String>();
                for (HIDDeviceInfo dev : devs){
                    String manufacturer = dev.getManufacturer_string();
                    String model = dev.getProduct_string();
                    if (manufacturer != null && model != null){
                        if ( manufacturer.equals(bc.displayManufacturer) && model.equals(bc.displayModel) ){
                            paths.add(dev.getPath());
                        }
                    }
                }
                if (paths.size() > 0){
                    // always use the second device, first is defunct (don't know why)
                    String path = paths.lastElement();
                    //device = manager.openByPath("0004:0002:01");
                    System.out.println("Trying to open device at path "+path);
                    device = manager.openByPath(path);
                    System.out.print("Manufacturer: " + device.getManufacturerString() + "\n");
                    System.out.print("Product: " + device.getProductString() + "\n");
                    System.out.print("Serial Number: " + device.getSerialNumberString() + "\n");
                    setCodePage();
                }
            } else {
              System.out.println("No devices found.");
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.err.println("There seems to be an error with the HID device. Consider unplugging and replugging.");
            device = null;
        }
    }

    public void closeDevice() {
    /**
     * Function to close connection to HID device and free resources.
     * Should be called when working with device is finished.
     */
        try {
            if (device != null){
                clearScreen();
                device.close();
            }
        } catch(IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (manager != null){
                manager.release();
            }
            System.gc();
        }
    }

    /**
     * Release resources. Will call closeDevice().
     *
     * @throws Throwable
     */
    protected void finalize() throws Throwable {
        // It is important to call closeDevice() if user forgot to do so.
        try {
           closeDevice();
        } finally {
           super.finalize();
        }
    }


    private void writeToDevice(byte[] data) {
        if (device != null){
            try {
                //for (byte b : data){
                //    System.out.print(b+" ");
                //}
                //System.out.println();
                int n = device.write(data);
                //System.out.println("Number of bytes written to BA63 device: "+n);
            } catch(IOException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                System.err.println("There seems to be an error with the HID device. Consider unplugging and replugging.");
                device = null;
            }
        }
    }

    private void setCodePage() {
        /**
         * Set Code Page to one with € symbol (858), which is county code 0x34,
         * according to user's manual of BA63/USB.
         */
        byte[] data = new byte[]{0x02, 0x00, 0x03,
            0x1B, 0x52, 0x34
        };
        writeToDevice(data);
    }

    private void oneRowDown() {
        /**
         * Move the display's cursor one row down on the screen.
         */
        byte[] data = new byte[]{0x02, 0x00, 0x02,
            0x0A, 0x0D // Line feed and carriage return
        };
        writeToDevice(data);

        cursorToSecondRowFront();
    }

    private void cursorToSecondRowFront() {
        /**
         * Move the display's cursor to front of second row.
         */
        byte[] data = new byte[]{0x02, 0x00, 0x06,
            0x1B, 0x5B, 0x32, 0x3B, 0x31, 0x48 // set cursor in row 2, column 1
        };
        writeToDevice(data);
    }

    private void printToScreen(String text) {
        /**
         * Print text on the current row of the screen. Text cannot be longer
         * than `displayWidth` chars (set in config.properties).
         */
        // Print available charsets:
        //SortedMap<String, Charset> charsets = Charset.availableCharsets();
        //for ( Map.Entry<String, Charset> entry : charsets.entrySet() ){
        //    System.out.println(entry.getKey());
        //}
        byte[] textAsBytes = text.getBytes(Charset.forName("IBM00858")); // default charset of BA63/USB is Codepage 437
        int nchars = text.length() > bc.displayWidth ? bc.displayWidth : text.length();
        int nbytes = 3+nchars;
        byte[] data = new byte[nbytes];
        data[0] = 0x02; data[1] = 0x00; data[2] = (byte)nchars;
        for (int i=0; i<nchars; i++){
            data[3+i] = textAsBytes[i];
        }
        writeToDevice(data);
    }

    private void printFrontAndRear(String front, String rear) {
        /**
         * Print `front` to beginning and `rear` to end of the row.
         */
        String row = front;
        int nspaces = bc.displayWidth - row.length() - rear.length();
        for (int i=0; i<nspaces; i++){ row += " "; }
        row += rear;
        printToScreen(row);
    }

    private String parseString(String str) {
        /**
         * Remove problem characters.
         */
        //str = str.replaceAll("€", "EUR");
        return str;
    }



    /* Public print methods: */

    public void clearScreen() {
        byte[] data = new byte[]{0x02, 0x00, 0x07,
            0x1B, 0x5B, 0x32, 0x4A, // clear screen
            0x1B, 0x5B, 0x48 // set cursor in row 1, column 1
        };
        writeToDevice(data);
    }

    public void showWelcomeScreen() {
        clearScreen();
        printToScreen("     Willkommen     ");
        oneRowDown();
        printToScreen("  im Weltladen Bonn ");
    }

    public void printArticle(String name, Integer stueck, String preis, String total) {
        /**
         * Convenience method for printing details on bought article. Interface
         * method exposed to the public.
         */
        name = parseString(name);
        preis = parseString(preis);
        total = parseString(total);

        clearScreen();
        printToScreen(name);
        oneRowDown();
        printFrontAndRear(stueck.toString()+"x"+preis, total);
    }

    public void printZWS(String zws) {
        /**
         * Convenience method for printing subtotal (ZWS). Interface
         * method exposed to the public.
         */
        zws = parseString(zws);

        clearScreen();
        printToScreen("ZU ZAHLEN");
        oneRowDown();
        printFrontAndRear("", zws);
    }

    public void printReturnMoney(String kundeGibt, String rueckgeld) {
        /**
         * Convenience method for printing amount given by customer (kundeGibt)
         * and return money (rueckgeld). Interface method exposed to the public.
         */
        kundeGibt = parseString(kundeGibt);
        rueckgeld = parseString(rueckgeld);

        clearScreen();
        printFrontAndRear("KUNDE GIBT", kundeGibt);
        oneRowDown();
        printFrontAndRear("RÜCKGELD", rueckgeld);
    }
}
