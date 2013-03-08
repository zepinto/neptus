/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by sergioferreira
 * 14 de Dez de 2010
 * $Id:: UavAltitudePanel.java 9863 2013-02-05 12:08:29Z sergioferreira       $:
 */
package pt.up.fe.dceg.neptus.plugins.uavs.panels;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.MultiSystemIMCMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.LEVEL;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.containers.MigLayoutContainer;
import pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter;
import pt.up.fe.dceg.neptus.plugins.uavs.painters.background.UavCoverLayerPainter;
import pt.up.fe.dceg.neptus.plugins.uavs.painters.elements.UavLabelPainter;
import pt.up.fe.dceg.neptus.plugins.uavs.painters.elements.UavMissionElementPainter;
import pt.up.fe.dceg.neptus.plugins.uavs.painters.foreground.UavRulerPainter;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * Neptus panel which allows the console operator to see the current altitudes of all active vehicles, detected by the console, and filtered by the selected type.
 * 
 * @author Sergio Ferreira
 * @version 2.0
 * @category UavPanel  
 * 
 */
@PluginDescription(name = "Uav Altitude Panel", icon = "pt/up/fe/dceg/neptus/plugins/uavs/planning.png", author = "sergioferreira",  version = "2.0", category = CATEGORY.INTERFACE)
public class UavAltitudePanel extends SimpleSubPanel implements ComponentListener {

    private static final long serialVersionUID = 1L;

    private final static int SIDE_PANEL_WIDTH = 50;
    private final static int BOTTOM_LABEL_HEIGHT = 25;
        
    @NeptusProperty(name = "Vehicles detected", category = "UAV", userLevel = LEVEL.REGULAR)
    public String vehicleType = "UAV";
    
    @NeptusProperty(name = "Minimum Altitude", category = "UAV", userLevel = LEVEL.REGULAR)
    public Integer maxAlt = 200;
    
    @NeptusProperty(name = "Minimum security vertical distance between UAVs", category = "UAV", userLevel = LEVEL.REGULAR)
    public Integer secAlt = 50;
    
    @NeptusProperty(name = "Panel measuring units", category = "UAV", userLevel = LEVEL.REGULAR)
    public String measure = "SI";
    
    @NeptusProperty(name = "Individual ruler mark width in pixels", category = "UAV", userLevel = LEVEL.REGULAR)
    public Integer markWidth = 5;
    
    //layers to be painted as background for panel's draw area
    private LinkedHashMap<String, IUavPainter> layers;
    
    //arguments passed to each layer in the painting phase, in order to provide them with necessary data to allow rendering
    private LinkedHashMap<String, Object> args;

    //table containing the rendered vehicles and their altitudes
    private LinkedHashMap<String, Integer> vehicleAltitudes;

    //current profile active in the host's MigLayoutPanel
    private String profile;

    //structure used to house the pixels per mark and marking grade for drawing purposes
    private Point pixelsPerMark_markGrade_Pair;
   
    //listener object which allows the panel to tap into the various IMC messages
    private MultiSystemIMCMessageListener listener = new MultiSystemIMCMessageListener(this.getClass().getSimpleName()
            + " [" + Integer.toHexString(hashCode()) + "]") {

        @Override
        public void messageArrived(ImcId16 id, IMCMessage msg) {

            //Check if the message is coming from a UAV. Only if it is, do something
            if (ImcSystemsHolder.lookupSystem(id).getTypeVehicle().name().equalsIgnoreCase("UAV")){
                
                //updates the vehicle's registered altitude
                vehicleAltitudes.put(ImcSystemsHolder.lookupSystem(id).getName(), msg.getInteger("height") + (-msg.getInteger("z")));
                repaint();
            }            
        }
    };

    public UavAltitudePanel(ConsoleLayout console) {
        super(console);
        
        // clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }

    // ------Setters and Getters------//

    // PixelsPerMark_markGrade_Pair
    private void setPixelsPerMark_markGrade_Pair(Point point) {
        this.pixelsPerMark_markGrade_Pair = point;
    }

    // Layers
    private void setLayers(LinkedHashMap<String, IUavPainter> backgroundLayers) {
        this.layers = backgroundLayers;
    }

    // Args
    private void setArgs(LinkedHashMap<String, Object> args) {
        this.args = args;
    }

    // VehicleAltitudes
    private void setVehicleAltitudes(LinkedHashMap<String, Integer> layerArgs) {
        this.vehicleAltitudes = layerArgs;
    }

    // ------Specific Methods------//

    /**
     * Method which makes the necessary preparations to the data that is to be sent to the UavPainters in <b>layers</b>. These preparations depend on the active <b>profile</b>
     * 
     * @return void
     */
    private void prepareArgs() {
        
        //updates the UavRulerPainter's maximum altitude
        determinePixelsPerMark(pixelsPerMark_markGrade_Pair,this.getHeight(),((Number)(Math.floor((this.getHeight())/100)*100)).intValue(),markWidth); 
        
        if (profile.equals("TACO") || profile.equals("1vX")) {

            // vehicles to draw
            args.put("vehicles", vehicleAltitudes);
        }
        else {

            // single vehicle to draw
            LinkedHashMap<String, Integer> singleUav = new LinkedHashMap<String, Integer>();
            if (vehicleAltitudes.get(this.getMainVehicleId()) != null) {
                singleUav.put(this.getMainVehicleId(), vehicleAltitudes.get(this.getMainVehicleId()));
            }
            args.put("vehicles", singleUav);
        }

        args.put("markInfo", pixelsPerMark_markGrade_Pair);
    }

    /**
     * 
     */
    private void updatePainterSizes() {
        
        //updates each of the panels draw points
        args.put("Skybox.DrawPoint", new int[] {0, 0});
        args.put("SidePainter.DrawPoint", new int[] {this.getWidth() - SIDE_PANEL_WIDTH, 0});  
        args.put("AltitudeLabel.DrawPoint", new int[] {0, this.getHeight() - BOTTOM_LABEL_HEIGHT});  
        
        //updates each of the panels sizes
        args.put("Skybox.Size", new int[] {this.getWidth() - SIDE_PANEL_WIDTH, this.getHeight() - BOTTOM_LABEL_HEIGHT});
        args.put("SidePainter.Size", new int[] {SIDE_PANEL_WIDTH, this.getHeight()});
        args.put("AltitudeLabel.Size", new int[] {this.getWidth() - SIDE_PANEL_WIDTH, BOTTOM_LABEL_HEIGHT});  
    }
    
    /**
     * 
     */
    private void updateLabelText() {

        //updates label's text
        if(measure.equals("SI"))
            args.put("AltitudeLabel.Text", "Alt. "+"[m]");
        if(measure.equals("Imperial"))
            args.put("AltitudeLabel.Text", "Alt. "+"[f]"); 
    }
    
    /**
     * @param height
     * @param rulerMax
     * @return
     */
    private void determinePixelsPerMark(Point ret, int height, int rulerMax, int minMarkHeight) {

        rulerMax = updateRulerMax(rulerMax);

        ret.x = 0;
        ret.y = 1;
        int i = 0;

        while ((ret.x = height / (rulerMax / ret.y)) < 2 * minMarkHeight) {
            switch (i % 2) {
                case 0:
                    ret.y *= 5;
                    break;
                default:
                    ret.y *= 2;
                    break;
            }
            i++;
        }
    }

    /**
     * @param object
     * @return
     */
    private int updateRulerMax(int ret) {

        // determines the highest altitude value
        for (Integer alt : vehicleAltitudes.values()) {
            if (ret < alt) {
                ret = alt;
            }
        }

        if (ret % 100 != 0) {
            ret = ((Number) (Math.floor(ret / 100) * 100 + 100)).intValue();
        }

        return ret;
    }

    @Override
    public void initSubPanel() {
        setLayers(new LinkedHashMap<String, IUavPainter>());
        setVehicleAltitudes(new LinkedHashMap<String, Integer>());
        setArgs(new LinkedHashMap<String, Object>());
        setPixelsPerMark_markGrade_Pair(new Point());

        // sets up the listener to listen to all vehicles
        listener.setSystemToListen();

        // which messages are listened to
        listener.setMessagesToListen("EstimatedState");

        // sets up all the layers used by the panel
        layers.put("Skybox", new UavCoverLayerPainter("Skybox"));
        layers.put("SidePainter", new UavCoverLayerPainter("SidePainter"));
        layers.put("AltitudeLabel", new UavLabelPainter("AltitudeLabel"));
        layers.put("UavRulerPainter1",  new UavRulerPainter());
        layers.put("UavMissionElement1", new UavMissionElementPainter());     
                        
        // sets up initial colors for the cover panels
        args.put("Skybox.Color", new Color[] {Color.blue,Color.gray.brighter()});
        args.put("SidePainter.Color",  new Color[] {Color.gray,Color.gray});
        args.put("AltitudeLabel.Color",  new Color[] {Color.gray.brighter(),Color.gray.brighter()});
       
        this.addComponentListener(this);
        
        updateLabelText();
        updatePainterSizes();  
    }

    @Override
    public void cleanSubPanel() {
        listener.clean();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if(this.getConsole().getMainPanel().getComponent(0).getClass().getSimpleName().equals("MigLayoutContainer"))
            profile = ((MigLayoutContainer) this.getConsole().getMainPanel().getComponent(0)).currentProfile;
        else
            profile = "None";       
          
        //arranges the arguments for the different operation profiles
        prepareArgs();

        synchronized (layers) {
            for (IUavPainter layer : layers.values()) {
                Graphics2D gNew = (Graphics2D)g.create();
                layer.paint(gNew, this.getWidth(), this.getHeight(), args);
                gNew.dispose();
            }
            args.clear();
        }        
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentResized(ComponentEvent e) {
        updatePainterSizes();    
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentMoved(ComponentEvent e) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentShown(ComponentEvent e) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentHidden(ComponentEvent e) {
        // TODO Auto-generated method stub
        
    }
}
