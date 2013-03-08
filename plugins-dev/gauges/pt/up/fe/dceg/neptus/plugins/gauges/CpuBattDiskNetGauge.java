/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/09/15
 * $Id:: CpuBattDiskNetGauge.java 9616 2012-12-30 23:23:22Z pdias         $:
 */
package pt.up.fe.dceg.neptus.plugins.gauges;

import java.awt.Dimension;
import java.util.concurrent.LinkedBlockingDeque;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.colormap.InterpolationColorMap;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.AlarmProviderOld;
import pt.up.fe.dceg.neptus.console.plugins.ConsoleScript;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.util.ConsoleParse;

/**
 * @author zp
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "ZP", name = "MultiVariable Gauge", description = "This panel displays various variables simultaneously", icon = "pt/up/fe/dceg/neptus/plugins/gauges/gauges.png")
public class CpuBattDiskNetGauge extends SimpleSubPanel implements AlarmProviderOld, IPeriodicUpdates,
        ConfigurationListener, NeptusMessageListener {

    private final LinkedBlockingDeque<Long> beats = new LinkedBlockingDeque<Long>();
    private final GaugeDisplay battDisplay = new GaugeDisplay(), cpuDisplay = new GaugeDisplay(),
            netDisplay = new GaugeDisplay(), diskDisplay = new GaugeDisplay();

    private final ConsoleScript battScript = new ConsoleScript(), battTextScript = new ConsoleScript(),
            cpuScript = new ConsoleScript(), cpuTextScript = new ConsoleScript(), diskScript = new ConsoleScript(),
            diskTextScript = new ConsoleScript();

    @NeptusProperty(name = "Update interval", description = "Interval between updates in milliseconds")
    public long millisBetweenUpdates = 1000;
    protected long lastBeatTime = System.currentTimeMillis();

    @NeptusProperty(name = "Battery value", category = "Battery", description = "Expression for calculating battery voltage. Should be a value between 0 and 1.")
    public String battExpression = "$(InternalVoltage.11.value) * 12";

    protected String message = "As usual, everything is working just fine.";
    protected int alarmLevel = AlarmProviderOld.LEVEL_0;

    public String validateBattExpression(String script) {
        try {
            new ConsoleScript().setScript(script);
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    @NeptusProperty(name = "Battery text", category = "Battery", description = "Text to display. Can use tree variables.")
    public String battText = "$(InternalVoltage.0.value) * 0.01";

    public String validateBattText(String script) {
        try {
            new ConsoleScript().setScript(script);
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    @NeptusProperty(name = "CPU load value", category = "CPU", description = "Expression for calculating CPU load. Should be a value between 0 and 1.")
    public String cpuExpression = "$(CpuUsage.usage)/100";

    public String validateCpuExpression(String script) {
        try {
            new ConsoleScript().setScript(script);
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    @NeptusProperty(name = "CPU load text", category = "CPU", description = "Text to display in the CPU gauge. Can use tree variables.")
    public String cpuText = "\"CPU load: \"+$(CpuUsage.usage)+\"%\"";

    public String validateCpuText(String script) {
        try {
            new ConsoleScript().setScript(script);
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    @NeptusProperty(name = "Disk usage value", category = "Disk", description = "Expression for calculating disk usage. Should be a value between 0 and 1.")
    public String diskExpression = "$(StorageUsage.value) / 100.0";

    public String validateDiskExpression(String script) {
        try {
            new ConsoleScript().setScript(script);
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    @NeptusProperty(name = "Disk usage text", category = "Disk", description = "Text to display in the disk usage gauge.")
    public String diskText = "\"Free: \"+$(StorageUsage.available)+\" MB\"";

    public String validateDiskText(String script) {
        try {
            new ConsoleScript().setScript(script);
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    @NeptusProperty(category = "Network")
    public int heartBeatSecs = 5;

    @Override
    public long millisBetweenUpdates() {
        return millisBetweenUpdates;
    }

    private double evalToDouble(ConsoleScript script) {
        Object o = script.evaluate(getState());
        if (o != null && o instanceof Number)
            return ((Number) o).doubleValue();

        return Double.NaN;

    }

    private String evalToString(ConsoleScript script) {
        Object o = script.evaluate(getState());
        if (o != null)
            return o.toString();

        return "";
    }

    protected Runnable updateInterface = new Runnable() {

        @Override
        public void run() {

            boolean enabled = (System.currentTimeMillis() - lastBeatTime) < heartBeatSecs * 2000;

            // HEARTBEAT
            netDisplay.setEnabled(enabled);
            long minTime = System.currentTimeMillis() - (heartBeatSecs * 1000);
            while (beats.peek() != null && beats.peek() < minTime) {
                beats.poll();
            }
            float hbRatio = (float) beats.size() / (float) heartBeatSecs;
            if (!Float.isNaN(hbRatio)) {
                netDisplay.setValue(Math.min(1.0, hbRatio));
                netDisplay.setToolTipText("Heartbeat reception rate");
            }

            if (hbRatio == 0) {
                int before = alarmLevel;
                alarmLevel = AlarmProviderOld.LEVEL_4;
                message = "No heartbeat has been heard for more than " + heartBeatSecs + " seconds";
                if (before != alarmLevel)
                    getMainpanel().getAlarmlistener().updateAlarmsListeners(CpuBattDiskNetGauge.this);
            }
            else {
                int before = alarmLevel;
                alarmLevel = AlarmProviderOld.LEVEL_0;
                message = "As usual, everything is working just fine.";
                if (before != alarmLevel)
                    getMainpanel().getAlarmlistener().updateAlarmsListeners(CpuBattDiskNetGauge.this);
            }

            // CPU USAGE:
            cpuDisplay.setEnabled(enabled);
            double val = evalToDouble(cpuScript);
            if (!Double.isNaN(val)) {
                cpuDisplay.setValue(val);
                cpuDisplay.setToolTipText(evalToString(cpuTextScript));
            }

            // BATT:
            battDisplay.setEnabled(enabled);
            val = evalToDouble(battScript);
            if (!Double.isNaN(val)) {
                battDisplay.setValue(val);
                battDisplay.setToolTipText(evalToString(battTextScript));
            }

            // DISK:
            diskDisplay.setEnabled(enabled);
            val = evalToDouble(diskScript);
            if (!Double.isNaN(val)) {
                diskDisplay.setValue(val);
                diskDisplay.setToolTipText(evalToString(diskTextScript));
            }
            repaint();
        }
    };

    @Override
    public boolean update() {

        if (isVisible()) {
            try {
                SwingUtilities.invokeLater(updateInterface);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public void propertiesChanged() {
        try {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    try {

                        cpuScript.setScript(cpuExpression);
                        cpuTextScript.setScript(cpuText);

                        battScript.setScript(battExpression);
                        battTextScript.setScript(battText);

                        diskScript.setScript(diskExpression);
                        diskTextScript.setScript(diskText);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAlarmMessage() {
        return message;
    }

    @Override
    public int getAlarmState() {
        return alarmLevel;
    }

    @Override
    public int sourceState() {
        return alarmLevel;
    }

    public CpuBattDiskNetGauge(ConsoleLayout console) {
        super(console);
        removeAll();
        setLayout(new MigLayout("", "[right][grow]", "[]3[]"));
        // JPanel tmp = new JPanel(new BorderLayout());

        JLabel batLbl = new JLabel("Battery"), cpuLbl = new JLabel("CPU"), hddLabel = new JLabel("Storage"), hbLabel = new JLabel(
                "HeartBeat");

        batLbl.setPreferredSize(new Dimension(50, 25));
        batLbl.setHorizontalAlignment(JLabel.RIGHT);
        cpuLbl.setPreferredSize(new Dimension(50, 25));
        cpuLbl.setHorizontalAlignment(JLabel.RIGHT);
        hddLabel.setPreferredSize(new Dimension(50, 25));
        hddLabel.setHorizontalAlignment(JLabel.RIGHT);
        hbLabel.setPreferredSize(new Dimension(50, 25));
        hbLabel.setHorizontalAlignment(JLabel.RIGHT);

        add(batLbl);
        add(battDisplay, "wrap,grow");

        add(cpuLbl);
        cpuDisplay.setColormap(ColorMapFactory.createInvertedColorMap((InterpolationColorMap) ColorMapFactory
                .createRedYellowGreenColorMap()));
        add(cpuDisplay, "wrap,grow");

        add(hddLabel);
        diskDisplay.setColormap(ColorMapFactory.createInvertedColorMap((InterpolationColorMap) ColorMapFactory
                .createRedYellowGreenColorMap()));
        add(diskDisplay, "wrap,grow");

        add(hbLabel);
        add(netDisplay, "wrap,grow");

        propertiesChanged();
    }

    public static void main(String[] args) {
        ConsoleParse.testSubPanel(CpuBattDiskNetGauge.class);
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "Heartbeat" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        beats.add(System.currentTimeMillis());
        lastBeatTime = System.currentTimeMillis();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
