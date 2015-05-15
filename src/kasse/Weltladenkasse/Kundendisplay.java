package Weltladenkasse;

import java.io.IOException;

// HID API to talk with Nixdorf BA63/USB display:
import com.codeminders.hidapi.*;

//import WeltladenDB.JNIFromJar;

/**
 * This class demonstrates enumeration, reading and getting
 * notifications when a HID device is connected/disconnected.
 */
public class Kundendisplay {

    private static final long READ_UPDATE_DELAY_MS = 50L;

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
        readDevice();
    }

    /**
     * Static function to read an input report to a HID device.
     */
    private static void readDevice() {
        HIDDevice dev;
        try {
            HIDManager manager = HIDManager.getInstance();
            //dev = manager.openById(VENDOR_ID, PRODUCT_ID, null);
            dev = manager.openByPath("0004:0003:00");
            //dev = manager.openByPath("0004:0003:01");
            System.out.print("Manufacturer: " + dev.getManufacturerString() + "\n");
            System.out.print("Product: " + dev.getProductString() + "\n");
            System.out.print("Serial Number: " + dev.getSerialNumberString() + "\n");
            //try {
            //    byte[] buf = new byte[BUFSIZE];
            //    dev.enableBlocking();
            //    while(true)
            //    {
            //        int n = dev.read(buf);
            //        System.out.println(n);
            //        for(int i=0; i<n; i++)
            //        {
            //            int v = buf[i];
            //            if (v<0) v = v+256;
            //            String hs = Integer.toHexString(v);
            //            if (v<16)
            //                System.out.print("0");
            //            System.out.print(hs + " ");
            //        }
            //        System.out.println("");

            //        try
            //        {
            //            Thread.sleep(READ_UPDATE_DELAY_MS);
            //        } catch(InterruptedException e)
            //        {
            //            //Ignore
            //            e.printStackTrace();
            //        }
            //    }
            try {
                byte[] data = new byte[]{0x01, 0x1b, 0x5b, 0x30, 0x63};
                //byte[] data = new byte[]{0x00, 0x00, 0x10, 0x00, 0x00,
                //    0x00, 0x00, 0x00, 0x00,
                //    0x00, 0x00, 0x00, 0x00,
                //    0x00, 0x00, 0x00, 0x00,
                //    0x00, 0x00, 0x00, 0x00,
                //    0x00, 0x00, 0x00, 0x00,
                //    0x00, 0x00, 0x00, 0x00,
                //    0x00, 0x00, 0x00, 0x00};
                int n = dev.write(data);
                System.out.println("Number of bytes written to BA63 device: "+n);

                byte[] buf = new byte[BUFSIZE];
                //dev.enableBlocking();
                n = dev.readTimeout(buf, 10000);
                System.out.println("Number of bytes read: "+n);
                //dev.disableBlocking();
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
            } finally {
                dev.close();
                if (null != manager){
                    manager.release();
                }
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
        System.out.println(property);
        HIDManager manager = null;
        try
        {
            manager = HIDManager.getInstance();
            HIDDeviceInfo[] devs = manager.listDevices();
            System.out.println("Devices:");
            for(int i=0; i<devs.length; i++)
            {
                System.out.println(""+i+":\t"+devs[i]);
                System.out.println("---------------------------------------------\n");
            }
            System.gc();
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
            if (null != manager){
                manager.release();
            }
            System.gc();
        }
    }

}

