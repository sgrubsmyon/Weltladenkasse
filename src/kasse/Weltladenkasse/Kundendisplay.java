package Weltladenkasse;

import java.io.IOException;

// HID API to talk with Nixdorf BA63/USB display:
import com.codeminders.hidapi.*;

import WeltladenDB.JNIFromJar;

/**
 * This class demonstrates enumeration, reading and getting
 * notifications when a HID device is connected/disconnected.
 */
public class Kundendisplay {

    private static final long READ_UPDATE_DELAY_MS = 50L;

    static {
        try {
            System.loadLibrary("hidapi-jni"); // used for tests. This library in classpath only
        } catch (UnsatisfiedLinkError e) {
            try {
                JNIFromJar.loadLibraryFromJar("/resources/natives/"+System.mapLibraryName("hidapi-jni-32"));
                                                // during runtime. .DLL within .JAR
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    // Wincor Nixdorf International GmbH Operator Display, BA63-USB
    static final int VENDOR_ID = 2727;
    static final int PRODUCT_ID = 512;
    private static final int BUFSIZE = 2048;

    /**
     *    The constructor.
     *       */
    public Kundendisplay() {
        listDevices();
        readDevice();
    }

    /**
     * Static function to read an input report to a HID device.
     */
    private static void readDevice() {
        HIDDevice dev;
        try {
            HIDManager hid_mgr = HIDManager.getInstance();
            dev = hid_mgr.openById(VENDOR_ID, PRODUCT_ID, null);
            System.err.print("Manufacturer: " + dev.getManufacturerString() + "\n");
            System.err.print("Product: " + dev.getProductString() + "\n");
            System.err.print("Serial Number: " + dev.getSerialNumberString() + "\n");
            try {
                byte[] buf = new byte[BUFSIZE];
                dev.enableBlocking();
                while(true)
                {
                    int n = dev.read(buf);
                    for(int i=0; i<n; i++)
                    {
                        int v = buf[i];
                        if (v<0) v = v+256;
                        String hs = Integer.toHexString(v);
                        if (v<16)
                            System.err.print("0");
                        System.err.print(hs + " ");
                    }
                    System.err.println("");

                    try
                    {
                        Thread.sleep(READ_UPDATE_DELAY_MS);
                    } catch(InterruptedException e)
                    {
                        //Ignore
                        e.printStackTrace();
                    }
                }
            } finally
            {
                dev.close();
                hid_mgr.release();
                System.gc();
            }

        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Static function to find the list of all the HID devices
     * attached to the system.
     */
    private static void listDevices()
    {
        String property = System.getProperty("java.library.path");
        System.err.println(property);
        try
        {

            HIDManager manager = HIDManager.getInstance();
            HIDDeviceInfo[] devs = manager.listDevices();
            System.err.println("Devices:\n\n");
            for(int i=0;i<devs.length;i++)
            {
                System.err.println(""+i+".\t"+devs[i]);
                System.err.println("---------------------------------------------\n");
            }
            System.gc();
        }
        catch(IOException e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

}

