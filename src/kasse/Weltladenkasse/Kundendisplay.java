package Weltladenkasse;

import java.util.*; // Map, SortedMap
import java.io.IOException;
import java.nio.charset.Charset;

// HID API to talk with Nixdorf BA63/USB display:
import com.codeminders.hidapi.*;

//import WeltladenDB.JNIFromJar;

/**
 * This class is for interaction with VFD customer display Wincor Nixdorf BA63
 * USB.
 */
public class Kundendisplay {

    static {
        ClassPathLibraryLoader.loadNativeHIDLibrary();

        // or:

        //try {
        //    System.loadLibrary("hidapi-jni"); // used for tests. This library in classpath only
        //} catch (UnsatisfiedLinkError e) {
        //    try {
        //        JNIFromJar.loadLibraryFromJar("/resources/natives/"+System.mapLibraryName("hidapi-jni-32"));
        //                                        // during runtime. .DLL within .JAR
        //    } catch (IOException e1) {
        //        throw new RuntimeException(e1);
        //    }
        //}
    }

    private HIDManager manager;
    private HIDDevice device = null;
    //private static final long READ_UPDATE_DELAY_MS = 50L;

    // Wincor Nixdorf International GmbH Operator Display, BA63-USB
    static final int VENDOR_ID = 2727;
    static final int PRODUCT_ID = 512;
    //private static final int BUFSIZE = 2048;
    private static final int BUFSIZE = 32;

    /**
     *    The constructor.
     *       */
    public Kundendisplay() {
        listDevices();
        openDevice();
        showWelcomeScreen();
    }

    private void listDevices() {
    /**
     * Static function to find the list of all the HID devices
     * attached to the system.
     */
        String property = System.getProperty("java.library.path");
        System.out.println(property);
        HIDManager mgr = null;
        try
        {
            mgr = HIDManager.getInstance();
            HIDDeviceInfo[] devs = mgr.listDevices();
            System.out.println("HID (Display) Devices:");
            if (devs != null){
                for(int i=0; i<devs.length; i++)
                {
                    System.out.println(""+i+":\t"+devs[i]);
                    System.out.println("---------------------------------------------\n");
                }
            } else {
                System.out.println("No devices found.");
            }
        }
        catch(IOException e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        catch(NullPointerException e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.err.println("It seems that the customer display is not connected. (No HID devices found.)");
        } finally {
            if (null != mgr){
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
            if (devs != null){
                // always use the second device, first is defunct (don't know why)
                String path = devs[1].getPath();
                //device = manager.openById(VENDOR_ID, PRODUCT_ID, null);
                //device = manager.openByPath("0004:0002:01");
                System.out.println("Trying to open device at path "+path);
                device = manager.openByPath(path);
                System.out.print("Manufacturer: " + device.getManufacturerString() + "\n");
                System.out.print("Product: " + device.getProductString() + "\n");
                System.out.print("Serial Number: " + device.getSerialNumberString() + "\n");
                setCodePage();
            }
        } catch(IOException e) {
            e.printStackTrace();
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
    protected void finalize() throws Throwable
    {
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
                for (byte b : data){
                    System.out.print(b+" ");
                }
                System.out.println();
                int n = device.write(data);
                System.out.println("Number of bytes written to BA63 device: "+n);
            } catch(IOException e) {
                e.printStackTrace();
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
         * than 20 chars.
         */
        // Print available charsets:
        //SortedMap<String, Charset> charsets = Charset.availableCharsets();
        //for ( Map.Entry<String, Charset> entry : charsets.entrySet() ){
        //    System.out.println(entry.getKey());
        //}
        byte[] textAsBytes = text.getBytes(Charset.forName("IBM00858")); // default charset of BA63/USB is Codepage 437
        int nchars = text.length() > 20 ? 20 : text.length();
        int nbytes = 3+nchars;
        byte[] data = new byte[nbytes];
        data[0] = 0x02; data[1] = 0x00; data[2] = (byte)nchars;
        for (int i=0; i<nchars; i++){
            data[3+i] = textAsBytes[i];
        }
        writeToDevice(data);
    }



    public void clearScreen() {
        byte[] data = new byte[]{0x02, 0x00, 0x07,
            0x1B, 0x5B, 0x32, 0x4A, // clear screen
            0x1B, 0x5B, 0x48 // set cursor in row 1, column 1
        };
        writeToDevice(data);
    }

    public void showWelcomeScreen() {
        clearScreen();
        printToScreen("     Wällkömménßü€  ");
        oneRowDown();
        printToScreen(" im  Weltladen Bonn ");
    }

    private String parseString(String str) {
        /**
         * Remove problem characters.
         */
        //str = str.replaceAll("€", "EUR");
        return str;
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

        String priceRow = stueck.toString()+"x"+preis;
        int nspaces = 20 - priceRow.length() - total.length();
        for (int i=0; i<nspaces; i++){ priceRow += " "; }
        priceRow += total;
        printToScreen(priceRow);
    }


    private void readDevice() {
        try {
            try {
                int n = 1;
                while (n > 0){
                    byte[] buf = new byte[BUFSIZE];
                    //device.enableBlocking();
                    n = device.readTimeout(buf, 10000);
                    //n = device.read(buf);
                    System.out.println("Number of bytes read: "+n);
                    //device.disableBlocking();
                    for(int i=0; i<n; i++)
                    {
                        int v = buf[i];
                        if (v<0) v = v+256;
                        String hs = Integer.toHexString(v);
                        if (v<16)
                            System.out.print("0");
                        System.out.print(hs + " ");
                    }
                    System.out.println("");
                }
            } finally {
                device.close();
                if (null != manager){
                    manager.release();
                }
                System.gc();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


}

