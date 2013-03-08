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
 * Oct 16, 2012
 * $Id:: TransponderEstimation.java 9615 2012-12-30 23:08:28Z pdias             $:
 */
package pt.up.fe.dceg.neptus.plugins.noptilus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.LblBeacon;
import pt.up.fe.dceg.neptus.imc.LblConfig;
import pt.up.fe.dceg.neptus.imc.LblRange;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author noptilus
 */
@PluginDescription(name = "Transponder Location Estimation", author="Noptilus")
public class TransponderEstimation extends SimpleSubPanel implements Renderer2DPainter {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Logs folder", hidden = true)
    public String logsFolder = ".";

    @NeptusProperty(name = "Number of iterations")
    public int numIterations = 10000;
    
    @NeptusProperty(name = "Distance treshold")
    public double dropDistance = 100;
    
    
    public TransponderEstimation(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }
    
    protected LinkedHashMap<String, LocationType> estimations = new LinkedHashMap<String, LocationType>();
    protected JMenuItem calcMenu = null, settingsMenu = null;
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        
        for (String name : estimations.keySet()) {
            LocationType loc = estimations.get(name);            
            Point2D pt = renderer.getScreenPosition(loc);
            g.setColor(Color.green.brighter());
            g.drawString(name, (int)pt.getX()+10, (int)pt.getY());
            g.draw(new Line2D.Double(pt.getX()-3, pt.getY()-3, pt.getX()+3, pt.getY()+3));
            g.draw(new Line2D.Double(pt.getX()+3, pt.getY()-3, pt.getX()-3, pt.getY()+3));
        }
    }    
    
    @Override
    public void initSubPanel() {
        calcMenu = addMenuItem("Noptilus"+">"+"Transponder Estimation"+">"+"Calculate", null, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(logsFolder);
                chooser.setDialogTitle("Open surface log file");
                int option = chooser.showOpenDialog(getConsole());

                if (option == JFileChooser.APPROVE_OPTION) {
                    File selection = chooser.getSelectedFile();
                    logsFolder = selection.getParent();

                    try {
                        getRanges(selection);
                    }
                    catch (Exception ex) {
                        GuiUtils.errorMessage(getConsole(), ex);
                    }
                }
            }
        });
        
        settingsMenu = addMenuItem("Noptilus"+">"+"Transponder Estimation"+">"+"Settings", null, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
               PropertiesEditor.editProperties(TransponderEstimation.this, true);               
            }
        });
        
    }

    public static final int TIME = 0, X = 1, Y = 2, Z = 3, RANGE = 4;

    public void getRanges(File lsfFile) throws Exception {
        LsfIndex index = new LsfIndex(lsfFile, new IMCDefinition(new FileInputStream(new File(lsfFile.getParent(),
                "IMC.xml"))));

        LinkedHashMap<Short, Vector<double[]>> ranges = new LinkedHashMap<Short, Vector<double[]>>();
        LinkedHashMap<Short, double[]> beaconLocations = new LinkedHashMap<Short, double[]>();
        LinkedHashMap<Short, String> beaconNames = new LinkedHashMap<Short, String>();

        int curPos = index.getFirstMessageOfType(LblRange.ID_STATIC);

        while (curPos != -1) {
            LblRange range = new LblRange(index.getMessage(curPos));
            if (!ranges.containsKey(range.getId()))
                ranges.put((short)range.getId(), new Vector<double[]>());
            int estate = index.getNextMessageOfType(EstimatedState.ID_STATIC, curPos);

            if (estate != -1) {
                EstimatedState state = new EstimatedState(index.getMessage(estate));
                double[] values = new double[] { range.getTimestamp(), state.getX(), state.getY(), state.getZ(),
                        range.getRange() };
                ranges.get(range.getId()).add(values);
            }
            curPos = index.getNextMessageOfType(LblRange.ID_STATIC, curPos);
        }

        int lblIndex = index.getFirstMessageOfType(LblConfig.ID_STATIC);
        if (lblIndex == -1)
            throw new Exception("No LBL configuration found in the log");

        int hrefIndex = index.getFirstMessageOfType(EstimatedState.ID_STATIC);
        if (hrefIndex == -1)
            throw new Exception("No HomeRef found in the log");
        
        EstimatedState state = new EstimatedState(index.getMessage(hrefIndex));
        LocationType homeLoc = new LocationType(Math.toDegrees(state.getLat()), Math.toDegrees(state.getLon()));
        LblConfig lblConfig = new LblConfig(index.getMessage(lblIndex));
        Vector<LblBeacon> beacons = lblConfig.getBeacons();
        
        for (short i = 0; i < beacons.size(); i++) {
            if (beacons.get(i) == null)
                continue;
            
            LblBeacon beacon = beacons.get(i);
            
            if (beacon != null) {
                LocationType loc = new LocationType(Math.toDegrees(beacon.getLat()), Math.toDegrees(beacon.getLon()));
                loc.setAbsoluteDepth(beacon.getDepth());
                beaconLocations.put( i, loc.getOffsetFrom(homeLoc));
                beaconNames.put( i, beacon.getBeacon());
            }
        }

        JTabbedPane tabs = new JTabbedPane();
        for (Short key : ranges.keySet()) {
            Vector<double[]> vec = ranges.get(key);

            Object[][] values = new Object[vec.size()][5];
            for (int i = 0; i < vec.size(); i++)
                for (int j = 0; j < 5; j++)
                    values[i][j] = vec.get(i)[j];

            DefaultTableModel model = new DefaultTableModel(values, new String[] { "time", "x", "y", "z", "range" });
            JTable table = new JTable(model);
            JScrollPane scroll = new JScrollPane(table);

            tabs.addTab("Beacon " + key, scroll);
        }

        Random rand = new Random(System.currentTimeMillis());

        for (long beacon : beaconNames.keySet()) {
            double[] estimate = Arrays.copyOf(beaconLocations.get(beacon), beaconLocations.get(beacon).length);
            double[] bestEstimate = Arrays.copyOf(estimate, estimate.length);
            
            Vector<double[]> measurements = ranges.get(beacon);

            if (measurements == null)
                continue;
            
            int N = measurements.size();
            double bestR = Double.MAX_VALUE;

            for (int k = 0; k < numIterations; k++) {

                double r = 0;

                for (int i = 0; i < measurements.size(); i++) {

                    double Px = measurements.get(i)[X];
                    double Py = measurements.get(i)[Y];
                    double Pz = measurements.get(i)[Z];

                    double val1 = Math.sqrt(
                            (Px - estimate[0]) * (Px - estimate[0]) + 
                            (Py - estimate[1]) * (Py - estimate[1]) + 
                            (Pz - estimate[2]) * (Pz - estimate[2])
                        );

                    double val2 = measurements.get(i)[RANGE];

                    r += Math.abs(val1 - val2);
                }
                r = r / N;

                if (r < bestR) {
                    bestEstimate = Arrays.copyOf(estimate, estimate.length);
                    bestR = r;
                }

                for (int i = 0; i < 3; i++)
                    estimate[i] = bestEstimate[i] + rand.nextGaussian() * Math.sqrt(bestR);
            }
            
            LocationType loc = new LocationType(homeLoc);
            loc.translatePosition(bestEstimate);
            
            estimations.put(beaconNames.get(beacon), loc);
        }
        
        JMenu menu = getConsole().getOrCreateJMenu(new String[] {"Noptilus", "Transponder Estimation"});
        menu.removeAll();
        menu.add(calcMenu);
        menu.add(settingsMenu);
            
        for (String beaconName : estimations.keySet()) {
            final LocationType loc = estimations.get(beaconName);
            loc.setId(beaconName);
            JMenuItem item = new JMenuItem("Copy "+beaconName+" location");
            item.setToolTipText(loc.getLatitudeAsPrettyString()+" / "+loc.getLongitudeAsPrettyString()+" / "+loc.getAllZ());
            item.addActionListener(new ActionListener() {                
                @Override
                public void actionPerformed(ActionEvent e) {
                    ClipboardOwner owner = new ClipboardOwner() {
                        public void lostOwnership(java.awt.datatransfer.Clipboard clipboard, java.awt.datatransfer.Transferable contents) {};                       
                    };
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(loc.getClipboardText()), owner);
                }
            });
            item.setActionCommand("copy "+beaconName);
            menu.add(item);
        }
        
        JMenuItem clear = new JMenuItem("Clear");
        clear.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                JMenu menu = getConsole().getOrCreateJMenu(new String[] {"Noptilus", "Transponder Estimation"});
                estimations.clear();
                menu.removeAll();
                menu.add(calcMenu);
                menu.add(settingsMenu);
            }
        });
        menu.add(clear);
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        TransponderEstimation est = new TransponderEstimation(null);
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Open surface log file");
        int option = chooser.showOpenDialog(null);

        if (option == JFileChooser.APPROVE_OPTION) {
            File selection = chooser.getSelectedFile();

            try {
                est.getRanges(selection);
            }
            catch (Exception ex) {
                GuiUtils.errorMessage(null, ex);
                ex.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
