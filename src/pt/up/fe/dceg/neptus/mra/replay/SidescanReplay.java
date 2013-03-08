/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * May 18, 2012
 * $Id:: SidescanReplay.java 9952 2013-02-19 18:24:10Z jqcorreia                $:
 */
package pt.up.fe.dceg.neptus.mra.replay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.SonarData;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.coord.MapTileUtil;

/**
 * @author jqcorreia
 * 
 */
@LayerPriority(priority = 1)
public class SidescanReplay implements LogReplayLayer {

    List<SidescanData> dataSet = new ArrayList<SidescanData>();
    float range = 0;
    
    BufferedImage image;
    LocationType imageLoc;
    double imageScaleX;
    
    boolean generate = true;
    int lod;    
    double top=0,bot=0,left=0,right=0;
    
    LocationType topleftLT;
    LocationType botrightLT;
    
    LocationType lastCenter = new LocationType();
    
    int imageLod = 20;
    
    
    @Override
    public void cleanup() {
        image = null;
        dataSet.clear();
    }
    
    protected void generateImage(StateRenderer2D renderer)
    {
        final StateRenderer2D rend = renderer;
        
        final double groundResolution = MapTileUtil.groundResolution(dataSet.get(0).loc.getLatitudeAsDoubleValue(), renderer.getLevelOfDetail());
        final double invGR = 1/groundResolution;
        lod = renderer.getLevelOfDetail();
        imageLod = lod;
        imageScaleX = range*2 * (invGR) / 2000;
        
       
        Point2D p1 = renderer.getScreenPosition(topleftLT);
        Point2D p2 = renderer.getScreenPosition(botrightLT);
        
        top = p1.getY();
        left = p1.getX();
        right = p2.getX();
        bot = p2.getY();
        
        image = ImageUtils.createCompatibleImage((int)(right - left), (int)(bot - top), Transparency.BITMASK);
        lastCenter = renderer.getCenter();
        
        Thread t = new Thread() {
            public void run() {
                Graphics2D g = ((Graphics2D)image.getGraphics());

                g.setColor(Color.green);
                g.drawRect(0, 0, image.getWidth() - 1, image.getHeight() - 1);
                g.setColor(null);
                g.setComposite(new SideScanComposite());

                double lod = rend.getLevelOfDetail();
                
                for(SidescanData ssd : dataSet) {
                    if (lod != rend.getLevelOfDetail())
                        return;
                    
                    Point2D p = rend.getScreenPosition(ssd.loc);
                    Graphics2D g2 = (Graphics2D)g.create();
                    
                    g2.translate(-left, -top);
                    g2.translate(p.getX() - 1000 * imageScaleX, p.getY());
                    g2.rotate(ssd.heading,1000*imageScaleX,0);
                    g2.scale(imageScaleX, 1);
                    //g2.drawImage(ssd.img, null, 0, 0);
                    //int ysize =  (int)((ssd.alongTrackLength*invGR) > 1 ? (ssd.alongTrackLength*invGR) : 1);
//                    System.out.println(ysize + " " + groundResolution + " " + ssd.alongTrackLength);
                    g2.drawImage(ssd.img, 0, 0, null);
                    g2.dispose();
                    rend.repaint();
                }
            };          
        };
        t.start();
        
        
    }
            
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        LocationType center  = renderer.getCenter().getNewAbsoluteLatLonDepth();
        
        if(renderer.getLevelOfDetail()!= lod) 
            generate = true;
        if(generate) {
            generateImage(renderer);
            generate = false;
        }
        double[] offset = center.getDistanceInPixelTo(lastCenter, renderer.getLevelOfDetail());

        left += offset[0];
        top += offset[1];
        Graphics2D g2 = (Graphics2D) g.create();
        g2.rotate(-renderer.getRotation(), renderer.getWidth()/2, renderer.getHeight()/2);
        g2.drawImage(image, null, (int)(left), (int)(top));
        g2.dispose();
        lastCenter = center;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("EstimatedState") != null && source.getLog("SonarData") != null;
    }
    
    @Override
    public String getName() {
        return I18n.text("Sidescan Replay");
    }

    @Override
    public void parse(IMraLogGroup source) {
        //System.out.println("parsing sidescan for replay");
        IMraLog ssParse = source.getLog("SonarData");
        IMraLog esParse = source.getLog("EstimatedState");

        IMCMessage msgSS = ssParse.firstLogEntry();
        IMCMessage msgES = esParse.getEntryAtOrAfter(msgSS.getTimestampMillis());
        IMCMessage prevMsgSS = null;
        
        LocationType loc = new LocationType();
        LocationType tempLoc;
      
        double minLat = 180;
        double maxLat = -180;
        double minLon = 360;
        double maxLon = -360;
        
        range = (float) msgSS.getFloat("max_range");
        System.out.println("Range = " + range);
        while (msgSS != null) {
            if(msgSS.getInteger("type") == SonarData.TYPE.SIDESCAN.value()) {
                msgES = esParse.getEntryAtOrAfter(msgSS.getTimestampMillis());
                if(msgES == null)
                {
                    msgSS = ssParse.nextLogEntry();
                    continue;
                }
                loc.setLatitude(Math.toDegrees(msgES.getDouble("lat")));
                loc.setLongitude(Math.toDegrees(msgES.getDouble("lon")));
                loc.setOffsetNorth(msgES.getDouble("x"));
                loc.setOffsetEast(msgES.getDouble("y"));
                tempLoc = loc.getNewAbsoluteLatLonDepth();

                if (tempLoc.getLatitudeAsDoubleValue() < minLat)
                    minLat = tempLoc.getLatitudeAsDoubleValue();
                if (tempLoc.getLatitudeAsDoubleValue() > maxLat)
                    maxLat = tempLoc.getLatitudeAsDoubleValue();
                if (tempLoc.getLongitudeAsDoubleValue() < minLon)
                    minLon = tempLoc.getLongitudeAsDoubleValue();
                if (tempLoc.getLongitudeAsDoubleValue() > maxLon)
                    maxLon = tempLoc.getLongitudeAsDoubleValue();

                if (prevMsgSS != null) {
                    byte[] currentRaw = msgSS.getRawData("data");
                    byte[] prevRaw = prevMsgSS.getRawData("data");
                    for (int i = 0; i < currentRaw.length; i++)
                        currentRaw[i] = (byte) ((prevRaw[i] + currentRaw[i]) / 2);
                    msgSS.setValue("data", currentRaw);
                    double len = msgES.getDouble("u") * 0.063;
                    dataSet.add(new SidescanData(currentRaw, loc.getNewAbsoluteLatLonDepth(), msgES.getDouble("psi"),
                            msgES.getDouble("alt"),len));
                }
                else {
                    double len = msgES.getDouble("u") * 0.2;
                    dataSet.add(new SidescanData(msgSS.getRawData("data"), loc.getNewAbsoluteLatLonDepth(), msgES
                            .getDouble("psi"), msgES.getDouble("alt"),len));
                }
            }
            msgSS = ssParse.nextLogEntry();
        }
        
        topleftLT = new LocationType(maxLat, minLon);
        botrightLT = new LocationType(minLat, maxLon);
        
        topleftLT.setOffsetNorth(range);
        topleftLT.setOffsetWest(range);
        botrightLT.setOffsetSouth(range);
        botrightLT.setOffsetEast(range);
        
        topleftLT = topleftLT.getNewAbsoluteLatLonDepth();
        botrightLT = botrightLT.getNewAbsoluteLatLonDepth();
    }

    @Override
    public String[] getObservedMessages() {
        // return new String[] { "EstimatedState", "SidescanPing" };
        return null;
    }

    @Override
    public void onMessage(IMCMessage message) {

    }

    class SidescanData {
        public double heading;
        public double alongTrackLength;
        public BufferedImage img;
        public LocationType loc;

        public SidescanData(byte[] raw, LocationType loc, double heading, double bottDistance, double alongTdist) {
            img = ImageUtils.createCompatibleImage(raw.length, 1, Transparency.BITMASK);
            double startAngle = 185.0;
            double angleStep = 170.0 / 2000.0;
            double angle;
            ColorMap cp = ColorMapFactory.createBronzeColormap();
            for (int i = 0; i < raw.length; i++) {
                angle = startAngle + (angleStep * i);
                double srange = (bottDistance * (2000f/100f)) / Math.cos(Math.toRadians(angle)); 
                double d = Math.sqrt(Math.pow(srange, 2) - Math.pow(bottDistance,2));
                int pos = (int)d *(i < 1000? -1 : 1);
                if(pos <= -1000 || pos >= 1000) {
                    continue;
                }
                img.setRGB(i,0,cp.getColor((raw[i] & 0xFF)/255.0).getRGB());
            }
            this.loc = loc;
            this.heading = heading;
            this.alongTrackLength = alongTdist;
        }
    }
    @Override
    public boolean getVisibleByDefault() {
        return false;
    }
}
