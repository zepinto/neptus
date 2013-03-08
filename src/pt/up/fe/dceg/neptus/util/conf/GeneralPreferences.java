/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2005/10/12 refactored in 2012/11/10
 * $Id:: GeneralPreferences.java 9926 2013-02-14 14:20:01Z pdias          $:
 */
package pt.up.fe.dceg.neptus.util.conf;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.LEVEL;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.PropertiesLoader;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author Paulo Dias
 * 
 */
public class GeneralPreferences implements PropertiesProvider {
    
    private static PropertiesLoader properties = null;

    public static final String GENERAL_PROPERTIES_FILE = "conf/general-properties.xml";

    public static Vector<PreferencesListener> pListeners = new Vector<PreferencesListener>();

    @NeptusProperty(name = "Language", category = "Interface", userLevel = LEVEL.REGULAR,
            description = "Select the language to use for the interface (needs restart). (Format [a-z]{2}_[A-Z]{2}")
    public static String language = "en";

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Speech On", category = "Interface", userLevel = LEVEL.REGULAR, 
            description = "Select this if you want the speech on or off.")
    public static boolean speechOn = true;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Comms Local Port UDP", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static int commsLocalPortUDP = 6001;

    @NeptusProperty(name = "Comms Local Port TCP", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static int commsLocalPortTCP = 6001;

    @NeptusProperty(name = "IMC transports to use", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "Comma separated transports list. Valid values are (UDP, TCP). (The order implies preference of use.)")
    public static String imcTransportsToUse = "UDP, TCP";

    @NeptusProperty(name = "IMC CCU ID", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static ImcId16 imcCcuId = new ImcId16("40:00");

    @NeptusProperty(name = "IMC CCU Name", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "The CCU Name to be presented to other peers")
    public static String imcCcuName = "CCU " + System.getProperty("user.name");

    @NeptusProperty(name = "IMC Multicast Enable", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "Multicast enable or disable")
    public static boolean imcMulticastEnable = true;

    @NeptusProperty(name = "IMC Multicast Address", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static String imcMulticastAddress = "224.0.75.69";

    @NeptusProperty(name = "IMC Multicast/Broadcast Port Range", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "Multicast port range to use for the announce channel.\n The form is, e.g. '6969','6967-6970'")
    public static String imcMulticastBroadcastPortRange = "30100-30104";

    @NeptusProperty(name = "IMC Broadcast Enable", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static boolean imcBroadcastEnable = true;

    @NeptusProperty(name = "IMC Change by Source IP Request", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "If enable allows the announce msg request to use the sender IP to be use in future comms. to the sender system.")
    public static boolean imcChangeBySourceIpRequest = true;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Logs Downloader - Enable Parcial Download", category = "IMC Logs Downloader", userLevel = LEVEL.ADVANCED, 
            description = "Enable the partial logs downloads (resume partial downloads). NOTE: THE DOWNLOAD BOXES ONLY READ THIS OPTION UPON CREATION.")
    public static boolean logsDownloaderEnablePartialDownload = true;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Heartbeat Time Period (ms)", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static int heartbeatTimePeriodMillis = 1000;

    @NeptusProperty(name = "Heartbeat Timeout (ms)", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static int heartbeatTimeoutMillis = 2000;


    @NeptusProperty(name = "Number Of Shown Trails Points", category = "Map", userLevel = LEVEL.REGULAR)
    public static int numberOfShownPoints = 500;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Auto Snapshot Period (s)", category = "Interface", userLevel = LEVEL.REGULAR)
    public static int autoSnapshotPeriodSeconds = 60;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Renderer Plan Color", category = "Map", userLevel = LEVEL.ADVANCED)
    public static Color rendererPlanColor = new Color(255, 255, 255);

    @NeptusProperty(name = "Renderer 3D Priority", category = "Map", userLevel = LEVEL.ADVANCED,
            description = Thread.MIN_PRIORITY + "- Minimum Priority <br>"
                    + Thread.MAX_PRIORITY + "- Maximum Priority<br>" + Thread.NORM_PRIORITY + "- Normal Priority")
    public static int renderer3DPriority = Thread.MIN_PRIORITY;

    @NeptusProperty(name = "Renderer Update Periode For Vehicle State (ms)", category = "Map", userLevel = LEVEL.ADVANCED,
            description = "This is the update periode to "
                    + "update the vehicle state in the renders (in miliseconds). Use '-1' to disable it. One good "
                    + "good value is 100ms (10Hz) or 50ms (20hz).")
    public static int rendererUpdatePeriodeForVehicleStateMillis = 50;

    @NeptusProperty(name = "Console Edit Border Color", category = "Console", userLevel = LEVEL.ADVANCED)
    public static Color consoleEditBorderColor = new Color(150, 0, 0);

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "SSH Connection Timeout (ms)", category = "SSH", userLevel = LEVEL.ADVANCED)
    public static int sshConnectionTimeoutMillis = 3000;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Comms. Queue Size", category = "Communications", userLevel = LEVEL.ADVANCED, 
            description = "Select the comms. queues size.")
    public static int commsQueueSize = 1024;

    @NeptusProperty(name = "Comms. Messsage Separation Time (ms)", category = "Communications", userLevel = LEVEL.ADVANCED, 
            description = "Select the comms. separation time in miliseconds that a message (by type) should be warn. Use \"-1\" for always warn.")
    public static int commsMsgSeparationMillis = -1;

    @NeptusProperty(name = "Filter UDP Redirect Also By Port", hidden = true, category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static boolean filterUdpAlsoByPort = false;

    @NeptusProperty(name = "Redirect Unknown Comms. To First Vehicle In Comm. List", hidden = true, category = "IMC Communications", userLevel = LEVEL.ADVANCED,
            description = "Any messages comming from unknown vehicle will be redirect to the first on comm. list.")
    public static boolean redirectUnknownIdsToFirstCommVehicle = false;
    

    @NeptusProperty(name = "Use New System Activity Counter", hidden = true, category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static boolean commsUseNewSystemActivityCounter = true;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Enable Log Sent Messages", category = "Message Logging", userLevel = LEVEL.REGULAR)
    public static boolean messageLogSentMessages = true;

    @NeptusProperty(name = "Enable Log Received Messages", category = "Message Logging", userLevel = LEVEL.REGULAR)
    public static boolean messageLogReceivedMessages = true;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Extended Program Output Log", category = "Neptus Program Logging", userLevel = LEVEL.ADVANCED,
            description = "If true, the program output will be augmented with more info (mainly for problem solving).")
    public static boolean programLogExtendedLog = false;

    
    // -------------------------------------------------------------------------
    // Constructor and initialize

    public GeneralPreferences() {
    }

    public static void initialize() {
        String generalPropertiesFile = ConfigFetch.resolvePathBasedOnConfigFile(GENERAL_PROPERTIES_FILE);
        PropertiesLoader generalProperties = new PropertiesLoader(generalPropertiesFile, PropertiesLoader.XML_PROPERTIES);
        setPropertiesLoader(generalProperties);
    }
    

    // -------------------------------------------------------------------------
    // Validators

    public static String validateLanguage(String value) {
        return new StringPatternValidator("[a-z]{2}_[A-Z]{2}").validate(value);
    }

    public static String validateCommsLocalPortUDP(int value) {
        return new IntegerMinMaxValidator(1, 65535).validate(value);
    }

    public static String validateCommsLocalPortTCP(int value) {
        return new IntegerMinMaxValidator(1, 65535).validate(value);
    }

    public static String validateImcTransportsToUse(String value) {
        return new StringCommaSeparatedListValidator("UDP", "TCP").validate(value);
    }

    public static String validateImcCcuId(ImcId16 value) {
        if (ImcId16.ANNOUNCE.equals(value) || ImcId16.BROADCAST_ID.equals(value) || ImcId16.NULL_ID.equals(value))
            return "This is a reserved ID, choose another other than " + ImcId16.ANNOUNCE.toPrettyString() + ", "
                    + ImcId16.BROADCAST_ID.toPrettyString() + ", and " + ImcId16.NULL_ID.toPrettyString() + ".";

        return null;
    }

    public static String validateImcCcuName(String value) {
        return new StringNonEmptyValidator().validate(value);
    }

    public static String validateImcMulticastAddress(String value) {
        return new StringPatternValidator("\\d{2,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}").validate(value);
    }

    public static String validateImcMulticastBroadcastPortRange(String value) {
        return new PortRangeValidator().validate(value);
    }

    public static String validateHeartbeatTimePeriodMillis(int value) {
        return new IntegerMinMaxValidator(100, 65535).validate(value);
    }

    public static String validateHeartbeatTimeoutMillis(int value) {
        return new IntegerMinMaxValidator(1000, 65535).validate(value);
    }

    public static String validateNumberOfShownPoints(int value) {
        return new IntegerMinMaxValidator(-1, 1000).validate(value);
    }

    public static String validateAutoSnapshotPeriodSeconds(int value) {
        return new IntegerMinMaxValidator(20, 30 * 60).validate(value);
    }

    public static String validateRenderer3DPriority(int value) {
        return new IntegerMinMaxValidator(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY).validate(value);
    }

    public static String validateRendererUpdatePeriodeForVehicleStateMillis(int value) {
        return new IntegerMinMaxValidator(-1, 1000).validate(value);
    }
    
    public static String validateSshConnectionTimeoutMillis(int value) {
        return new IntegerMinMaxValidator(0, false).validate(value);
    }

    public static String validateCommsQueueSize(int value) {
        return new IntegerMinMaxValidator(1, Integer.MAX_VALUE).validate(value);
    }
    
    public static String validateCommsMsgSeparationMillis(int value) {
        return new IntegerMinMaxValidator(-1, 1000).validate(value);
    }

    
    // -------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#getProperties()
     */
    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#setProperties(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
        
        Thread t = new Thread("Properties Change Warner") {
            @Override
            public void run() {
                warnPreferencesListeneres();
            }
        };
        t.setDaemon(true);
        t.start();
        
        saveProperties();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#getPropertiesDialogTitle()
     */
    @Override
    public String getPropertiesDialogTitle() {
        return "Neptus General Preferences";
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#getPropertiesErrors(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }

    /**
     * @param listener
     */
    public static void addPreferencesListener(PreferencesListener listener) {
        if (!pListeners.contains(listener))
            pListeners.add(listener);
    }

    /**
     * @param listener
     */
    public static void removePreferencesListener(PreferencesListener listener) {
        pListeners.remove(listener);
    }

    /**
     * @param propertyChanged
     */
    public static void warnPreferencesListeneres() {
        for (PreferencesListener pl : pListeners) {
            try {
                pl.preferencesUpdated();
            }
            catch (Exception e) {
                NeptusLog.pub().error(
                        "Exception warning '" + pl.getClass().getSimpleName() + "["
                                + Integer.toHexString(pl.hashCode()) + "]" + "' for "
                                + GeneralPreferences.class.getSimpleName() + " propertieschanges", e);
            }
            catch (Error e) {
                NeptusLog.pub().error(
                        "Error warning '" + pl.getClass().getSimpleName() + "[" + Integer.toHexString(pl.hashCode())
                                + "]" + "' for " + GeneralPreferences.class.getSimpleName() + " propertieschanges", e);
            }
        }
    }

    /**
     * @param c
     * @return
     */
    public static String colorToString(Color c) {
        if (c == null)
            return "0,0,0";

        return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    /**
     * @param s
     * @return
     */
    public static Color stringToColor(String s) {
        StringTokenizer st = new StringTokenizer(s, ", ");
        if (st.countTokens() != 3) {
            return Color.black;
        }
        try {
            int red = Integer.parseInt(st.nextToken());
            int green = Integer.parseInt(st.nextToken());
            int blue = Integer.parseInt(st.nextToken());
            return new Color(red, green, blue);
        }
        catch (Exception e) {
            e.printStackTrace();
            return Color.black;
        }
    }

    public static void setPropertiesLoader(PropertiesLoader properties) {
        GeneralPreferences.properties = properties;
        PluginUtils.loadProperties(GeneralPreferences.properties, GeneralPreferences.class);
    }

    public static void saveProperties() {
        try {
            PluginUtils.savePropertiesToXML(properties.getWorkingFile(), true, GeneralPreferences.class);
        }
        catch (IOException e) {
            NeptusLog.pub().error("saveProperties", e);
        }
    }

    public static void dumpGeneralPreferences() {
        try {
            PluginUtils.savePropertiesToXML(properties.getWorkingFile(), false, GeneralPreferences.class);
        }
        catch (IOException e) {
            NeptusLog.pub().error("saveProperties", e);
        }
    }

    /**
     * Generate IMC ID
     */
    public static void generateImcId() {
        // IMC Local ID
        try {
            String hostadr;
            try {
                InetAddress addr = InetAddress.getLocalHost();
                hostadr = addr.getHostAddress();
            }
            catch (Exception e1) { // UnknownHostException
                e1.printStackTrace();
                hostadr = "127.0.0.1";
            }
            String osName = System.getProperty("os.name");
            if (osName.toLowerCase().indexOf("linux") != -1) {
                try {
                    Enumeration<NetworkInterface> netInt = NetworkInterface.getNetworkInterfaces();
                    while (netInt.hasMoreElements()) {
                        NetworkInterface ni = netInt.nextElement();
                        Enumeration<InetAddress> iAddress = ni.getInetAddresses();
                        while (iAddress.hasMoreElements()) {
                            InetAddress ia = iAddress.nextElement();
                            if (!ia.isLoopbackAddress()) {
                                if (ia instanceof Inet4Address) {
                                    hostadr = ia.getHostAddress();
                                    break;
                                }
                            }
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String[] sl2 = hostadr.split("\\.");

            // IMC
            long idd = (Integer.parseInt(sl2[2]) << 8) + (Integer.parseInt(sl2[3]));
            ImcId16 newCcuId = new ImcId16((idd & 0x1FFF) | 0x4000);

//            GeneralPreferences.setProperty(GeneralPreferences.IMC_CCU_ID, newCcuId.toPrettyString());
//
//            GeneralPreferences.setProperty(GeneralPreferences.IMC_CCU_NAME, "CCU " + System.getProperty("user.name")
//                    + " " + sl2[2] + "_" + sl2[3]);
//            GeneralPreferences.saveProperties();

            GeneralPreferences.imcCcuId = newCcuId;
            GeneralPreferences.imcCcuName = "CCU " + System.getProperty("user.name") + " " + sl2[2] + "_" + sl2[3];
            GeneralPreferences.saveProperties();

        }
        catch (NumberFormatException e) {
//            GeneralPreferences.setProperty(GeneralPreferences.IMC_CCU_ID,
//                    GeneralPreferences.getPropertyDefaultValue(GeneralPreferences.IMC_CCU_ID));
        }
    }

    public static void main(String[] args) {
        final GeneralPreferences gp = new GeneralPreferences();
        
        GeneralPreferences.addPreferencesListener(new PreferencesListener() {
            @Override
            public void preferencesUpdated() {
                System.out.println("preferencesUpdated");
            }
        });

        final String filenameProps = "" + GeneralPreferences.class.getSimpleName().toLowerCase() + ".properties";
        final String filenameXML = "" + GeneralPreferences.class.getSimpleName().toLowerCase() + ".xml";

        PropertiesLoader pl = new PropertiesLoader(filenameXML, PropertiesLoader.XML_PROPERTIES);
        GeneralPreferences.setPropertiesLoader(pl);

        @SuppressWarnings("serial")
        final JButton button = new JButton(new AbstractAction(I18n.text("General Preferences")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                try {
                    PluginUtils.loadProperties(filenameXML, GeneralPreferences.class);
                }
                catch (IOException e2) {
                    e2.printStackTrace();
                }

                PropertiesEditor.editProperties(gp, true);
                
                try {
                    PluginUtils.saveProperties(filenameProps, true, GeneralPreferences.class);
                    PluginUtils.savePropertiesToXML(filenameXML, true, GeneralPreferences.class);
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        JFrame frame = GuiUtils.testFrame(button, I18n.text("General Preferences"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
