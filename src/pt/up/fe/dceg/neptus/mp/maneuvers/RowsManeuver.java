/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp, pdias
 * 11/03/2011
 * $Id:: RowsManeuver.java 9913 2013-02-11 19:11:17Z pdias                      $:
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.ToolbarSwitch;
import pt.up.fe.dceg.neptus.gui.editor.ComboEditor;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.Rows;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.renderer2d.InteractionAdapter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.PlanElement;
import pt.up.fe.dceg.neptus.util.ConsoleParse;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author zp
 * @author pdias
 */
public class RowsManeuver extends Maneuver implements LocatedManeuver, StateRendererInteraction,
IMCSerialization, StatisticsProvider, PathProvider {

    static boolean unblockNewRows = false;

    static {
        if (IMCDefinition.getInstance().create("Rows") != null)
            unblockNewRows = true;
    }

    protected double latRad = 0, lonRad = 0, z = 2, speed = 1000, bearingRad = 0, width = 100,
            length = 200, hstep = 27, ssRangeShadow = 30;
    protected double crossAngleRadians = 0;
    protected double curvOff = 15;
    protected float alternationPercentage = 1.0f;
    protected boolean squareCurve = true, firstCurveRight = true;
    protected boolean paintSSRangeShadow = true;
    protected String speed_units = "RPM";
    protected ManeuverLocation.Z_UNITS zunits = ManeuverLocation.Z_UNITS.NONE;

    protected InteractionAdapter adapter = new InteractionAdapter(null);
    protected Point2D lastDragPoint = null;

    protected boolean editing = false;

    protected Vector<double[]> points = new Vector<double[]>();

    protected static final int X = 0, Y = 1, Z = 2, T = 3;

    /**
     * 
     */
    public RowsManeuver() {
        super();
        recalcPoints();
    }

    protected ManeuverLocation calculatePosition() {
        ManeuverLocation loc = new ManeuverLocation();
        loc.setLatitude(Math.toDegrees(latRad));
        loc.setLongitude(Math.toDegrees(lonRad));
        loc.setZ(z);
        loc.setZUnits(zunits);
        return loc;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.Maneuver#loadFromXML(java.lang.String)
     */
    @Override
    public void loadFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            //System.out.println(doc.asXML());
            // basePoint
            Node node = doc.selectSingleNode("//basePoint/point");
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);            
            latRad = getManeuverLocation().getLatitudeAsDoubleValueRads();
            lonRad = getManeuverLocation().getLongitudeAsDoubleValueRads();
            z = getManeuverLocation().getZ();
            zunits = getManeuverLocation().getZUnits();

            // Velocity
            Node speedNode = doc.selectSingleNode("//speed");
            speed = Double.parseDouble(speedNode.getText());
            speed_units = speedNode.valueOf("@unit");

            bearingRad = Math.toRadians(Double.parseDouble(doc.selectSingleNode("//bearing").getText()));

            // area
            width = Double.parseDouble(doc.selectSingleNode("//width").getText());
            node = doc.selectSingleNode("//length");
            if (node != null)
                length = Double.parseDouble(node.getText());
            else
                length = width;

            //steps
            hstep = Double.parseDouble(doc.selectSingleNode("//hstep").getText());

            node = doc.selectSingleNode("//crossAngle");
            if (node != null)
                crossAngleRadians = Math.toRadians(Double.parseDouble(node.getText()));
            else
                crossAngleRadians = 0;

            node = doc.selectSingleNode("//alternationPercentage");
            if (node != null)
                alternationPercentage = Short.parseShort(node.getText())/100f;
            else
                alternationPercentage = 1;

            node = doc.selectSingleNode("//curveOffset");
            if (node != null)
                curvOff = Double.parseDouble(node.getText());
            else
                curvOff = 15;

            node = doc.selectSingleNode("//squareCurve");
            if (node != null)
                squareCurve = Boolean.parseBoolean(node.getText());
            else
                squareCurve = true;

            node = doc.selectSingleNode("//firstCurveRight");
            if (node != null)
                firstCurveRight = Boolean.parseBoolean(node.getText());
            else
                firstCurveRight = true;

            //            ssRangeShadow = 30;
            //            paintSSRangeShadow = true;
            node = doc.selectSingleNode("//ssRangeShadow");
            if (node != null) {
                try {
                    ssRangeShadow = Short.parseShort(node.getText());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    ssRangeShadow = 30;
                }
            }
            else
                ssRangeShadow = 30;
            node = doc.selectSingleNode("//paintSSRangeShadow");
            if (node != null)
                paintSSRangeShadow = Boolean.parseBoolean(node.getText());
            else
                paintSSRangeShadow = true;

        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }
        finally {
            recalcPoints();
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.Maneuver#clone()
     */
    @Override
    public Object clone() {
        RowsManeuver clone = new RowsManeuver();
        super.clone(clone);
        clone.setManeuverLocation(getManeuverLocation());
        clone.bearingRad = bearingRad;
        clone.hstep = hstep;        
        clone.length = length;
        clone.speed = speed;
        clone.speed_units = speed_units;
        clone.width = width;

        clone.alternationPercentage = alternationPercentage;
        clone.crossAngleRadians = crossAngleRadians;
        clone.curvOff = curvOff;
        clone.squareCurve = squareCurve;
        clone.ssRangeShadow = ssRangeShadow;
        clone.paintSSRangeShadow = paintSSRangeShadow;
        clone.firstCurveRight = firstCurveRight;

        clone.recalcPoints();
        return clone;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.Maneuver#ManeuverFunction(pt.up.fe.dceg.neptus.mp.VehicleState)
     */
    @Override
    public SystemPositionAndAttitude ManeuverFunction(SystemPositionAndAttitude lastVehicleState) {
        //TODO implement this for preview
        return null;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.Maneuver#getManeuverAsDocument(java.lang.String)
     */
    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        root.addAttribute("kind", "automatic");

        //basePoint
        Element basePoint = root.addElement("basePoint");
        Element point = getManeuverLocation().asElement("point");
        basePoint.add(point);
        Element radTolerance = basePoint.addElement("radiusTolerance");
        radTolerance.setText("0");    
        basePoint.addAttribute("type", "pointType");

        root.addElement("width").setText(""+width);
        root.addElement("length").setText(""+length);
        root.addElement("hstep").setText(""+hstep);
        root.addElement("bearing").setText(""+Math.toDegrees(bearingRad));

        if (crossAngleRadians != 0)
            root.addElement("crossAngle").setText(""+Math.toDegrees(crossAngleRadians));

        if ((short)(alternationPercentage*100f) != 100)
            root.addElement("alternationPercentage").setText(""+(short)(alternationPercentage*100f));

        if (curvOff != 15)
            root.addElement("curveOffset").setText(""+curvOff);

        if (!squareCurve)
            root.addElement("squareCurve").setText(""+squareCurve);

        if (!firstCurveRight)
            root.addElement("firstCurveRight").setText(""+firstCurveRight);

        //speed
        Element speedElem = root.addElement("speed");        
        speedElem.addAttribute("unit", speed_units);
        speedElem.setText(""+speed);

        if (!paintSSRangeShadow) {
            root.addElement("paintSSRangeShadow").setText(""+paintSSRangeShadow);
        }
        if (ssRangeShadow != 30) {
            root.addElement("ssRangeShadow").setText(""+Double.valueOf(ssRangeShadow).shortValue());
        }

        return document;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#getName()
     */
    @Override
    public String getName() {
        return "Rows";
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#getIconImage()
     */
    @Override
    public Image getIconImage() {
        return adapter.getIconImage();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#getMouseCursor()
     */
    @Override
    public Cursor getMouseCursor() {
        return adapter.getMouseCursor();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#isExclusive()
     */
    @Override
    public boolean isExclusive() {
        return true;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#mouseClicked(java.awt.event.MouseEvent, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        adapter.mouseClicked(event, source);        
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#mousePressed(java.awt.event.MouseEvent, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        adapter.mousePressed(event, source);
        lastDragPoint = event.getPoint();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#mouseDragged(java.awt.event.MouseEvent, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (lastDragPoint == null) {
            adapter.mouseDragged(event, source);
            lastDragPoint = event.getPoint();
            return;
        }
        double xammount = event.getPoint().getX() - lastDragPoint.getX();
        double yammount = event.getPoint().getY() - lastDragPoint.getY();
        yammount = -yammount;
        if (event.isControlDown()) {
            width += xammount/(Math.abs(xammount) < 30 ? 10 : 2);
            length += yammount/(Math.abs(yammount) < 30 ? 10 : 2);

            width = Math.max(1, width);
            length = Math.max(1, length);
            recalcPoints();
        }
        else if (event.isShiftDown()) {
            bearingRad += Math.toRadians(yammount / (Math.abs(yammount) < 30 ? 10 : 2));

            while (bearingRad > Math.PI * 2)
                bearingRad -= Math.PI * 2;            
            while (bearingRad < 0)
                bearingRad += Math.PI * 2;            
            recalcPoints();
        }
        else {
            adapter.mouseDragged(event, source);
        }
        lastDragPoint = event.getPoint();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#mouseMoved(java.awt.event.MouseEvent, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#mouseReleased(java.awt.event.MouseEvent, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        adapter.mouseReleased(event, source);
        lastDragPoint = null;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#wheelMoved(java.awt.event.MouseWheelEvent, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#keyPressed(java.awt.event.KeyEvent, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        adapter.keyPressed(event, source);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#keyReleased(java.awt.event.KeyEvent, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        adapter.keyReleased(event, source);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#keyTyped(java.awt.event.KeyEvent, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        adapter.keyTyped(event, source);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#setActive(boolean, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        editing = mode;
        adapter.setActive(mode, source);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.maneuvers.LocationProvider#getPosition()
     */
    @Override
    public ManeuverLocation getManeuverLocation() {
        return calculatePosition();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.maneuvers.LocationProvider#getFirstPosition()
     */
    @Override
    public ManeuverLocation getStartLocation() {
        try {
            double[] first = points.firstElement();
            ManeuverLocation loc = getManeuverLocation().clone();
            loc.translatePosition(first[X], first[Y], first[Z]);
            return loc;
        }
        catch (Exception e) {
            return getManeuverLocation();
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.maneuvers.LocationProvider#getLastPosition()
     */
    @Override
    public ManeuverLocation getEndLocation() {
        try {
            double[] last = points.lastElement();
            ManeuverLocation loc = getManeuverLocation().clone();
            loc.translatePosition(last[X], last[Y], last[Z]);
            return loc;
        }
        catch (Exception e) {
            return getManeuverLocation();
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.maneuvers.LocationProvider#setPosition(pt.up.fe.dceg.neptus.types.coord.AbstractLocationPoint)
     */
    @Override
    public void setManeuverLocation(ManeuverLocation location) {
        double absoluteLatLonDepth[] = location.getAbsoluteLatLonDepth(); 
        latRad = Math.toRadians(absoluteLatLonDepth[0]);
        lonRad = Math.toRadians(absoluteLatLonDepth[1]);
        z = location.getZ();
        zunits = location.getZUnits();
        recalcPoints();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.maneuvers.LocationProvider#translate(double, double, double)
     */
    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        ManeuverLocation loc = calculatePosition();
        loc.translatePosition(offsetNorth, offsetEast, offsetDown);
        setManeuverLocation(loc);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.maneuvers.PathProvider#getPathPoints()
     */
    @Override
    public List<double[]> getPathPoints() {
        return Collections.unmodifiableList(points);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.maneuvers.PathProvider#getPathLocations()
     */
    @Override
    public List<LocationType> getPathLocations() {
        Vector<LocationType> locs = new Vector<>();
        List<double[]> lst = Collections.unmodifiableList(points);
        LocationType start = new LocationType(getManeuverLocation());
        if (getManeuverLocation().getZUnits() == ManeuverLocation.Z_UNITS.DEPTH)
            start.setDepth(getManeuverLocation().getZ());
        else if (getManeuverLocation().getZUnits() == ManeuverLocation.Z_UNITS.DEPTH)
            start.setDepth(-getManeuverLocation().getZ());
        for (double[] ds : lst) {
            LocationType loc = new LocationType(start);
            loc.translatePosition(ds);
            loc.convertToAbsoluteLatLonDepth();
            locs.add(loc);
        }
        return locs;
    }

    /**
     * @return the bearingRad
     */
    public double getBearingRad() {
        return bearingRad;
    }

    /**
     * @param bearingRad the bearingRad to set
     */
    public void setBearingRad(double bearingRad) {
        this.bearingRad = bearingRad;
        recalcPoints();
    }

    /**
     * At the end call {@link #recalcPoints()} to update maneuver points
     * @param width
     * @param length
     * @param hstep
     * @param alternationPercent
     * @param curvOff
     * @param squareCurve
     * @param bearingRad
     * @param crossAngleRadians
     * @param firstCurveRight
     */
    public void setParams(double width, double length, double hstep,
            double alternationPercent, double curvOff, boolean squareCurve, double bearingRad,
            double crossAngleRadians, boolean firstCurveRight, boolean paintSSRangeShadow, short ssRangeShadow) {
        this.width = width;
        this.length = length;
        this.hstep = hstep;
        this.alternationPercentage = Double.valueOf(alternationPercent).floatValue();
        this.curvOff = curvOff;
        this.squareCurve = squareCurve;
        this.bearingRad = bearingRad;
        this.crossAngleRadians = crossAngleRadians;
        this.firstCurveRight = firstCurveRight;
        this.paintSSRangeShadow = paintSSRangeShadow;
        this.ssRangeShadow = ssRangeShadow;

        recalcPoints();
    }

    /**
     * @return the width
     */
    public double getWidth() {
        return width;
    }

    /**
     * @return the hstep
     */
    public double getHstep() {
        return hstep;
    }

    /**
     * @return the alternationPercent
     */
    public float getAlternationPercent() {
        return alternationPercentage;
    }

    /**
     * @return the curvOff
     */
    public double getCurvOff() {
        return curvOff;
    }

    /**
     * @return the squareCurve
     */
    public boolean isSquareCurve() {
        return squareCurve;
    }

    /**
     * @return the crossAngleRadians
     */
    public double getCrossAngleRadians() {
        return crossAngleRadians;
    }

    /**
     * @return the paintSSRangeShadow
     */
    public boolean isPaintSSRangeShadow() {
        return paintSSRangeShadow;
    }

    /**
     * @param paintSSRangeShadow the paintSSRangeShadow to set
     */
    public void setPaintSSRangeShadow(boolean paintSSRangeShadow) {
        this.paintSSRangeShadow = paintSSRangeShadow;
    }

    /**
     * @return the ssRangeShadow
     */
    public double getSsRangeShadow() {
        return ssRangeShadow;
    }

    /**
     * @param ssRangeShadow the ssRangeShadow to set
     */
    public void setSsRangeShadow(double ssRangeShadow) {
        this.ssRangeShadow = ssRangeShadow;
    }

    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        g2d.setColor(Color.white);

        double zoom = renderer.getZoom();
        g2d.rotate(-renderer.getRotation());

        g2d.rotate(-Math.PI/2);
        //        recalcPoints();
        ManeuversUtil.paintBox(g2d, zoom, width, length, 0, 0, bearingRad, crossAngleRadians, !firstCurveRight, editing);
        ManeuversUtil.paintPointLineList(g2d, zoom, points, paintSSRangeShadow, ssRangeShadow, editing);
        //        ManeuversUtil.paintBox(g2d, zoom, width, width, -width/2, -width/2, bearingRad, crossAngleRadians);
        //        ManeuversUtil.paintBox(g2d, zoom, width, width, -width/2, -width/2, bearingRad+Math.toRadians(-60), crossAngleRadians);
        //        ManeuversUtil.paintBox(g2d, zoom, width, width, -width/2, -width/2, bearingRad+Math.toRadians(-120), crossAngleRadians);
        //        ManeuversUtil.paintPointLineList(g2d, zoom, points, false, sRange);
        g2d.rotate(+Math.PI/2);
    }


    /**
     * Call this to update the maneuver points.
     */
    private void recalcPoints() {
        Vector<double[]> newPoints = ManeuversUtil.calcRowsPoints(width, length, hstep,
                alternationPercentage, curvOff, squareCurve, bearingRad, crossAngleRadians,
                !firstCurveRight);

        points = newPoints;
    }

    @Override
    public IMCMessage serializeToIMC() {
        Rows man = new Rows();
        man.setTimeout(getMaxTime());
        man.setLat(latRad);
        man.setLon(lonRad);
        man.setZ(z);
        man.setZUnits(zunits.toString());
        man.setSpeed(speed);
        man.setWidth(width);
        man.setLength(length);
        man.setBearing(bearingRad);
        man.setHstep(hstep);
        man.setCrossAngle(crossAngleRadians);
        man.setCoff((short)curvOff);
        man.setAlternation((short)(alternationPercentage*100));
        man.setCustom(getCustomSettings());
        man.setFlags((short) ((squareCurve ? Rows.FLG_SQUARE_CURVE : 0) + (firstCurveRight ? Rows.FLG_CURVE_RIGHT : 0)));

        String speedU = this.getUnits();
        if ("m/s".equalsIgnoreCase(speedU))
            man.setSpeedUnits(pt.up.fe.dceg.neptus.imc.Rows.SPEED_UNITS.METERS_PS);
        else if ("RPM".equalsIgnoreCase(speedU))
            man.setSpeedUnits(pt.up.fe.dceg.neptus.imc.Rows.SPEED_UNITS.RPM);
        else if ("%".equalsIgnoreCase(speedU))
            man.setSpeedUnits(pt.up.fe.dceg.neptus.imc.Rows.SPEED_UNITS.PERCENTAGE);
        else if ("percentage".equalsIgnoreCase(speedU))
            man.setSpeedUnits(pt.up.fe.dceg.neptus.imc.Rows.SPEED_UNITS.PERCENTAGE);

        return man;
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        Rows man = null;
        try {
            man = new Rows(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }


        setMaxTime(man.getTimeout());
        latRad = man.getLat();
        lonRad = man.getLon();
        z = man.getZ();
        zunits = pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS.valueOf(man.getZUnits().toString());
        speed = man.getSpeed();
        width = man.getWidth();
        length = man.getLength();
        bearingRad = man.getBearing();
        hstep = man.getHstep();

        switch (man.getSpeedUnits()) {
            case METERS_PS:
                speed_units = "m/s";
                break;
            case RPM:
                speed_units = "RPM";
                break;
            default:
                speed_units = "%";
                break;
        }
        crossAngleRadians = man.getCrossAngle();
        curvOff = man.getCoff();
        alternationPercentage = man.getAlternation()/100f;
        
        firstCurveRight = (man.getFlags() & Rows.FLG_CURVE_RIGHT) != 0;
        squareCurve = (man.getFlags() & Rows.FLG_SQUARE_CURVE) != 0;

        setCustomSettings(man.getCustom());
        recalcPoints();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.Maneuver#setProperties(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);

        for (Property p : properties) {

            if (p.getName().equals("Width")) {
                width = (Double)p.getValue();
                continue;
            }

            if (p.getName().equals("Length")) {
                length = (Double)p.getValue();
                continue;
            }

            if (p.getName().equals("Horizontal Alternation")) {
                alternationPercentage = ((Short) p.getValue()) / 100f;
                continue;
            }

            if (p.getName().equals("Speed")) {
                speed = (Double)p.getValue();
                continue;
            }

            if (p.getName().equalsIgnoreCase("Speed Units")) {
                speed_units = (String)p.getValue();
                continue;
            }

            if (p.getName().equals("Bearing")) {
                bearingRad = Math.toRadians((Double)p.getValue());
                continue;
            }

            if (p.getName().equals("Cross Angle")) {
                crossAngleRadians = Math.toRadians((Double)p.getValue());
                continue;
            }

            if (p.getName().equals("Horizontal Step")) {
                hstep = (Double)p.getValue();
                continue;
            }

            if (p.getName().equalsIgnoreCase("Curve Offset")) {
                curvOff = (Double)p.getValue();
                continue;
            }

            if (p.getName().equalsIgnoreCase("Square Curve")) {
                squareCurve = (Boolean)p.getValue();
                continue;
            }

            if (p.getName().equalsIgnoreCase("First Curve Right")) {
                firstCurveRight = (Boolean)p.getValue();
                continue;
            }

            if (p.getName().equalsIgnoreCase("Paint SideScan Range Shadow")) {
                paintSSRangeShadow = (Boolean)p.getValue();
                continue;
            }
            if (p.getName().equalsIgnoreCase("SideScan Range Shadow")) {
                ssRangeShadow = (Short)p.getValue();
                continue;
            }
        }
        recalcPoints();
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> props = new Vector<DefaultProperty>();

        DefaultProperty length = PropertiesEditor.getPropertyInstance("Length", Double.class, this.length, true);
        length.setShortDescription("The length of the volume to cover, in meters");
        props.add(length);

        DefaultProperty width = PropertiesEditor.getPropertyInstance("Width", Double.class, this.width, true);
        width.setShortDescription("Width of the volume to cover, in meters");
        props.add(width);

        DefaultProperty halt = PropertiesEditor.getPropertyInstance("Horizontal Alternation", Short.class, (short)(this.alternationPercentage*100), unblockNewRows);
        halt.setShortDescription("Horizontal alternation in percentage. 100 will make all rows separated by the Horizontal Step");
        props.add(halt);

        DefaultProperty hstep = PropertiesEditor.getPropertyInstance("Horizontal Step", Double.class, this.hstep, true);
        hstep.setShortDescription("Horizontal distance between rows, in meters");
        props.add(hstep);

        DefaultProperty direction = PropertiesEditor.getPropertyInstance("Bearing", Double.class, Math.toDegrees(bearingRad), true);
        direction.setShortDescription("The outgoing bearing (from starting location) in degrees");       
        props.add(direction);

        DefaultProperty cross = PropertiesEditor.getPropertyInstance("Cross Angle", Double.class, Math.toDegrees(crossAngleRadians), unblockNewRows);
        cross.setShortDescription("The tilt angle of the search box in degrees");       
        props.add(cross);

        DefaultProperty speed = PropertiesEditor.getPropertyInstance("Speed", Double.class, this.speed, true);
        speed.setShortDescription("The vehicle's desired speed");
        props.add(speed);

        DefaultProperty speedUnits = PropertiesEditor.getPropertyInstance("Speed Units", String.class, speed_units, true);
        speedUnits.setShortDescription("The units to consider in the speed parameters");
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(speedUnits, new ComboEditor<String>(new String[] {"m/s", "Km/h", "RPM", "%"}));      
        props.add(speedUnits);

        DefaultProperty curvOffset = PropertiesEditor.getPropertyInstance("Curve Offset", Double.class, curvOff, true);
        curvOffset.setShortDescription("The extra lenght to use for the curve");       
        props.add(curvOffset);

        DefaultProperty squareCurveP = PropertiesEditor.getPropertyInstance("Square Curve", Boolean.class, squareCurve, unblockNewRows);
        squareCurveP.setShortDescription("If the curve should be square or direct");       
        props.add(squareCurveP);

        DefaultProperty firstCurveRightP = PropertiesEditor.getPropertyInstance("First Curve Right", Boolean.class, firstCurveRight, unblockNewRows);
        firstCurveRightP.setShortDescription("If the first curve should be to the right or left");       
        props.add(firstCurveRightP);

        DefaultProperty paintSSRangeShadowP = PropertiesEditor.getPropertyInstance("Paint SideScan Range Shadow", Boolean.class, paintSSRangeShadow, unblockNewRows);
        paintSSRangeShadowP.setShortDescription("If the sidescan range shadow is painted");       
        props.add(paintSSRangeShadowP);
        DefaultProperty ssRangeShadowtP = PropertiesEditor.getPropertyInstance("SideScan Range Shadow", Short.class, Double.valueOf(ssRangeShadow).shortValue(), unblockNewRows);
        ssRangeShadowtP.setShortDescription("The sidescan range");       
        props.add(ssRangeShadowtP);

        //        for (DefaultProperty p : props) {
        //            System.out.println("* "+p.getName()+"="+p.getValue());
        //        }

        return props;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String getUnits() {
        return speed_units;
    }

    public void setUnits(String speed_units) {
        this.speed_units = speed_units;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.maneuvers.StatisticsProvider#getCompletionTime(pt.up.fe.dceg.neptus.types.coord.LocationType, double)
     */
    @Override
    public double getCompletionTime(LocationType initialPosition) {

        double speed = this.speed;
        if (this.speed_units.equalsIgnoreCase("RPM")) {
            speed = speed/769.230769231; //1.3 m/s for 1000 RPMs
        }
        else if (this.speed_units.equalsIgnoreCase("%")) {
            speed = speed/76.923076923; //1.3 m/s for 100% speed
        }

        return getDistanceTravelled(initialPosition) / speed;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.mp.maneuvers.StatisticsProvider#getDistanceTravelled(pt.up.fe.dceg.neptus.types.coord.LocationType)
     */
    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        double meters = getStartLocation().getDistanceInMeters(initialPosition);

        if (points.size() == 0) {
            int numrows = (int) Math.floor(width/hstep);
            double planeDistance = numrows * length + numrows * hstep;

            meters += planeDistance;
            return meters;
        }
        else {
            for (int i = 0; i < points.size(); i++) {
                double[] pointI = points.get(i);
                double[] pointF;
                try {
                    pointF = points.get(i+1);
                }
                catch (Exception e) {
                    break;
                }
                double[] offsets = {pointF[0]-pointI[0], pointF[1]-pointI[1]}; 
                double sum = offsets[0] * offsets[0] + offsets[1] * offsets[1];
                double planeDistance = Math.sqrt(sum);
                meters += planeDistance;
            }
            return meters;
        }
    }

    @Override
    public double getMaxDepth() {
        return z;
    }

    @Override
    public double getMinDepth() {
        return z;
    }

    @Override
    public String getTooltipText() {
        NumberFormat nf = GuiUtils.getNeptusDecimalFormat(2);
        return super.getTooltipText()+"<hr/>"+
        "length: <b>"+nf.format(length)+" m</b><br/>"+
        "width: <b>"+nf.format(width)+" m</b><br/>"+
        "alt: <b>"+(short)(alternationPercentage*100)+" %</b><br/>"+
        "hstep: <b>"+nf.format(hstep)+" m</b><br/>"+
        "bearing: <b>"+nf.format(Math.toDegrees(bearingRad))+" degs</b><br/>"+
        "cross angle: <b>"+nf.format(Math.toDegrees(crossAngleRadians))+" degs</b><br/>"+
        "speed: <b>"+nf.format(getSpeed())+" "+getUnits()+"</b><br/>"+
        "distance: <b>"+MathMiscUtils.parseToEngineeringNotation(getDistanceTravelled((LocationType)getStartLocation()), 2)+"m</b><br/>"+
        (paintSSRangeShadow ? "ss range: <b>"+(short)(ssRangeShadow)+" m</b><br/>" : "") +
        "<br>depth: <b>"+nf.format(z)+" m</b>";    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {

    }

    public static void main(String[] args) {
        
        RowsManeuver rows = new RowsManeuver();
        System.out.println(rows.getManeuverLocation());
        System.out.println(rows.getStartLocation());
        System.out.println(rows.getEndLocation());

        RowsManeuver man = new RowsManeuver();
        //        man.latRad = Math.toRadians(38.45);
        //        man.lonRad = Math.toRadians(-8.90);
        //        man.z = 2;
        //        man.bearingRad = Math.toRadians(45);
        //        man.width = 90;
        //        man.length = 200;
        //        man.hstep = 20;
        //        man.vstep = 0;
        //        man.height = 0;
        //        man.speed = 1000;
        //        man.speed_units = "RPM";
        String xml = man.getManeuverAsDocument("Rows").asXML();
        System.out.println(FileUtil.getAsPrettyPrintFormatedXMLString(xml));
        //        RowsManeuver clone = (RowsManeuver) man.clone();
        //        System.out.println(xml);
        //        System.out.println(clone.getManeuverAsDocument("Rows").asXML());
        //        
        //        RowsManeuver tmp = new RowsManeuver();
        //        tmp.loadFromXML(clone.getManeuverAsDocument("Rows").asXML());
        //        System.out.println(tmp.getManeuverAsDocument("Rows").asXML());
        //        
        //                MissionType mission = new MissionType("./missions/rep10/rep10.nmisz");
        //                StateRenderer2D r2d = new StateRenderer2D(MapGroup.getMapGroupInstance(mission));
        //                PlanElement pelem = new PlanElement(MapGroup.getMapGroupInstance(mission), null);
        //                PlanType plan = new PlanType(mission);
        //                man.setPosition(r2d.getCenter());
        //                man.setBearingRad(Math.toRadians(20));
        //                man.setParams(200, 300, 27, .5, 15, true, Math.toRadians(20), Math.toRadians(10), true);
        //                plan.getGraph().addManeuver(man);        
        //                pelem.setPlan(plan);
        //                r2d.addPostRenderPainter(pelem, "Plan");
        //                GuiUtils.testFrame(r2d);
        RowsManeuver.unblockNewRows = true;
        //                PropertiesEditor.editProperties(man, true);

        ConsoleParse.consoleLayoutLoader("./conf/consoles/seacon-basic.ncon");
        //        
        //        System.out.println(new RowsManeuver().getManeuverAsDocument("Rows").asXML());
        //        
    }
}
