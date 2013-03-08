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
 * 29/Jul/2012
 * $Id:: SystemPainterHelper.java 9615 2012-12-30 23:08:28Z pdias               $:
 */
package pt.up.fe.dceg.neptus.gui.system;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

import pt.up.fe.dceg.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolIconEnum;
import pt.up.fe.dceg.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolOperationalConditionEnum;
import pt.up.fe.dceg.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolShapeEnum;
import pt.up.fe.dceg.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolTypeEnum;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;

/**
 * @author pdias
 *
 */
public class SystemPainterHelper {

    public static enum CircleTypeBySystemType { AIR, SUBSURFACE, SURFACE, SURFACE_UNIT, DEFAULT };

    public static final int AGE_TRANSPARENCY = 128;

    private SystemPainterHelper() {
    }
    
    /**
     * @param sys
     * @return
     */
    public static final boolean isLocationKnown(ImcSystem sys) {
        return isLocationKnown(sys.getLocation(), sys.getLocationTimeMillis());
    }

    /**
     * @param loc
     * @param timeMillis
     * @return
     */
    public static final boolean isLocationKnown(LocationType loc, long timeMillis) {
        if (getLocationAge(loc, timeMillis) < 10000
                && !loc.isLocationEqual(LocationType.ABSOLUTE_ZERO)) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * @param loc
     * @param timeMillis
     * @return
     */
    public static final long getLocationAge(LocationType loc, long timeMillis) {
        return System.currentTimeMillis() - timeMillis;
    }


    /**
     * @param g
     * @param sys
     * @param isLocationKnown
     * @param isMainVehicle
     * @param milStd2525FilledOrNot
     */
    public static final void drawMilStd2525LikeSymbolForSystem(Graphics2D g, ImcSystem sys, boolean isLocationKnown,
            boolean isMainVehicle, boolean milStd2525FilledOrNot) {
        MilStd2525LikeSymbolsDefinitions.SymbolTypeEnum type = SymbolTypeEnum.AIR;
        if (sys.getType() == SystemTypeEnum.VEHICLE) {
            if (sys.getTypeVehicle() == VehicleTypeEnum.UAV)
                type = SymbolTypeEnum.AIR;
            else if (sys.getTypeVehicle() == VehicleTypeEnum.UUV)
                type = SymbolTypeEnum.SUBSURFACE;
            else if (sys.getTypeVehicle() == VehicleTypeEnum.UGV)
                type = SymbolTypeEnum.SURFACE;
            else if (sys.getTypeVehicle() == VehicleTypeEnum.USV)
                type = SymbolTypeEnum.SURFACE;
            else
                type = SymbolTypeEnum.SURFACE_UNIT;
        }
        else if (sys.getType() == SystemTypeEnum.CCU)
            type = SymbolTypeEnum.SURFACE_UNIT;
        else
            type = SymbolTypeEnum.SURFACE;

        MilStd2525LikeSymbolsDefinitions.SymbolShapeEnum shapeType = SymbolShapeEnum.FRIEND;
        if (!sys.isWithAuthority()) // (!sd..isWithAuthority())
            shapeType = SymbolShapeEnum.NEUTRAL;
        if (sys.getType() == SystemTypeEnum.UNKNOWN)
            shapeType = SymbolShapeEnum.UNKNOWN;

        MilStd2525LikeSymbolsDefinitions.SymbolOperationalConditionEnum operationalCondition = SymbolOperationalConditionEnum.NONE;
        if (sys.isOnErrorState())
            operationalCondition = SymbolOperationalConditionEnum.ERROR;
        
        MilStd2525LikeSymbolsDefinitions.SymbolIconEnum drawIcon = MilStd2525LikeSymbolsDefinitions.SymbolIconEnum.UAS;
        if (sys.getType() == SystemTypeEnum.CCU)
            drawIcon = SymbolIconEnum.CCU;
        else if (sys.getType() == SystemTypeEnum.UNKNOWN)
            drawIcon = SymbolIconEnum.UNKNOWN;
        else if (sys.getType() == SystemTypeEnum.MOBILESENSOR || sys.getType() == SystemTypeEnum.STATICSENSOR)
            drawIcon = SymbolIconEnum.SENSOR;
        
        drawMilStd2525LikeSymbolForSystem(g, type, shapeType, operationalCondition, drawIcon, isLocationKnown,
                isMainVehicle, milStd2525FilledOrNot);
    }

    /**
     * @param g
     * @param type
     * @param shapeType
     * @param operationalCondition
     * @param drawIcon
     * @param isLocationKnown
     * @param isMainVehicle
     * @param milStd2525FilledOrNot
     */
    public static final void drawMilStd2525LikeSymbolForSystem(Graphics2D g, MilStd2525LikeSymbolsDefinitions.SymbolTypeEnum type, 
            MilStd2525LikeSymbolsDefinitions.SymbolShapeEnum shapeType,
            MilStd2525LikeSymbolsDefinitions.SymbolOperationalConditionEnum operationalCondition,
            MilStd2525LikeSymbolsDefinitions.SymbolIconEnum drawIcon,
            boolean isLocationKnown, boolean isMainVehicle, boolean milStd2525FilledOrNot) {
        Graphics2D g2 = (Graphics2D) g.create();

        boolean drawMainIndicator = isMainVehicle; //sd.isMainVehicle();

        MilStd2525LikeSymbolsDefinitions.paintMilStd2525(g2, type, shapeType, operationalCondition, 30, true,
                milStd2525FilledOrNot, true, (isLocationKnown ? 255 : 128), drawIcon, drawMainIndicator);

        g2.dispose();
    }

    /**
     * @param renderer
     * @param g
     * @param sys
     * @param color
     * @param iconDiameter 
     * @param isLocationKnownUpToDate 
     */
    public static final void drawSystemIcon(StateRenderer2D renderer, Graphics2D g, ImcSystem sys, Color color,
            double iconDiameter, boolean isLocationKnownUpToDate) {
        drawSystemIcon(renderer, g, sys.getYawDegrees(), color, iconDiameter, isLocationKnownUpToDate);
    }
     
    /**
     * @param renderer
     * @param g
     * @param headingAngleDegrees
     * @param color
     * @param iconDiameter
     * @param isLocationKnownUpToDate
     */
    public static final void drawSystemIcon(StateRenderer2D renderer, Graphics2D g, double headingAngleDegrees, Color color,
            double iconDiameter, boolean isLocationKnownUpToDate) {
        Graphics2D g2 = (Graphics2D) g.create();
        // long atMillis = sys.getAttitudeTimeMillis();
        double yawRad = Math.toRadians(headingAngleDegrees);
        GeneralPath gp = SystemIconsUtil.getUAV();
        double scale = iconDiameter / gp.getBounds2D().getWidth();
        if (scale != 1.0)
            g2.scale(scale, scale);

        int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
        if (useTransparency != 255)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));
        
        g2.setColor(color);
        g2.rotate(yawRad - renderer.getRotation());
        g2.fill(gp);
        g2.setColor(Color.BLACK);
        g2.draw(gp);

        g2.dispose();
    }

    /**
     * @param g
     * @param sysName
     * @param color
     * @param safetyOffset
     * @param isLocationKnownUpToDate
     */
    public static final void drawSystemNameLabel(Graphics2D g, String sysName, Color color, double safetyOffset, boolean isLocationKnownUpToDate) {
        Graphics2D g2 = (Graphics2D) g.create();

        int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
        if (useTransparency != 255)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));

        g2.setColor(Color.BLACK);
        g2.drawString(sysName, (int) (12 * safetyOffset / 20) + 1, 1);
        g2.setColor(color);
        g2.drawString(sysName, (int) (12 * safetyOffset / 20), 0);
        g2.dispose();
    }

    /**
     * @param g
     * @param color
     * @param diameter
     * @param isLocationKnownUpToDate
     */
    public static final void drawCircleForSystem(Graphics2D g, Color color, double diameter, boolean isLocationKnownUpToDate) {
        drawCircleForSystem(g, color, diameter, CircleTypeBySystemType.DEFAULT, isLocationKnownUpToDate);
    }

    /**
     * @param g
     * @param color
     * @param diameter
     * @param circleType
     * @param isLocationKnownUpToDate
     */
    public static final void drawCircleForSystem(Graphics2D g, Color color, double diameter, CircleTypeBySystemType circleType, boolean isLocationKnownUpToDate) {
        Graphics2D g2 = (Graphics2D) g.create();

        int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
        if (useTransparency != 255)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));

//        g2.setColor(Color.BLACK);
//        g2.drawOval((int) (-diameter / 2) - 1, (int) (-diameter / 2) - 1, (int) diameter + 2, (int) diameter + 2);
//        g2.setColor(color);
//        g2.drawOval((int) (-diameter / 2), (int) (-diameter / 2), (int) diameter, (int) diameter);
//        g2.dispose();
        
        Shape shape;
        Shape shape1;
        switch (circleType) {
            case AIR: //AIR
                shape = new Arc2D.Double(-diameter * 0.1 / 2., -diameter * 0.2 * 2. / 2., diameter * 1.1,
                        diameter * 1.2 * 2, 0, 180, 0);
                shape1 = new Arc2D.Double(-diameter * 0.1 / 2. - 1, -diameter * 0.2 * 2. / 2. - 1, diameter * 1.1 + 2,
                        diameter * 1.2 * 2 + 2, 0, 180, 0);
                break;
            case SUBSURFACE: //SUBSURFACE
                shape = new Arc2D.Double(-diameter * 0.1 / 2., -diameter * 1.2 * 2. / 2., diameter * 1.1,
                        diameter * 1.2 * 2, 0, -180, 0);
                shape1 = new Arc2D.Double(-diameter * 0.1 / 2. - 1, -diameter * 1.2 * 2. / 2. - 1, diameter * 1.1 + 2,
                        diameter * 1.2 * 2 + 2, 0, -180, 0);
                break;
            case SURFACE: //SURFACE
            default:
                shape = new Ellipse2D.Double(-diameter * 0.2 / 2., -diameter * 0.2 / 2., diameter * 1.2,
                        diameter * 1.2);
                shape1 = new Ellipse2D.Double(-diameter * 0.2 / 2. - 1, -diameter * 0.2 / 2. - 1, diameter * 1.2 + 2,
                        diameter * 1.2 + 2);
                break;
            case SURFACE_UNIT: //SURFACE_UNIT
                shape = new RoundRectangle2D.Double(-diameter * 0.5 / 2., 0, diameter * 1.5, diameter, 0, 0);
                shape1 = new RoundRectangle2D.Double(-diameter * 0.5 / 2. - 1, 0 - 1, diameter * 1.5 + 2, diameter + 2, 0, 0);
                break;
        }
        g2.translate(-diameter / 2., -diameter / 2.);
        g2.setColor(Color.BLACK);
        g2.draw(shape1);
        g2.setColor(color);
        g2.draw(shape);

        g2.dispose();
    }

    /**
     * @param renderer
     * @param g2
     * @param sys
     * @param isLocationKnownUpToDate 
     */
    public static final void drawErrorStateForSystem(StateRenderer2D renderer, Graphics2D g, ImcSystem sys, double diameter, boolean isLocationKnownUpToDate) {
        if (sys.isOnErrorState()) {
            Graphics2D g2 = (Graphics2D) g.create();

            int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
            if (useTransparency != 255)
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));

            Shape opShape = new RoundRectangle2D.Double(-diameter / 2.0, diameter / 2.0 + diameter / 10, diameter, diameter / 3, 0, 0);
            Color opColor = new Color(255, 128, 0); // orange
            g2.setColor(opColor);
            g2.fill(opShape);
            g2.setColor(Color.BLACK);
            g2.draw(opShape);
            
            g2.dispose();
        }
    }

    /**
     * @param renderer
     * @param g2
     * @param sys
     * @param isLocationKnownUpToDate 
     */
    public static final void drawCourseSpeedVectorForSystem(StateRenderer2D renderer, Graphics2D g, ImcSystem sys, boolean isLocationKnownUpToDate,
            double minimumSpeedToBeStopped) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
        if (useTransparency != 255)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));

        Object obj = sys.retrieveData(ImcSystem.COURSE_KEY);
        if (obj != null) {
            double courseDegrees = (Integer) obj;
            obj = sys.retrieveData(ImcSystem.GROUND_SPEED_KEY);
            if (obj != null) {
                double gSpeed = (Double) obj;
                if (gSpeed > minimumSpeedToBeStopped) {
                    g2.rotate(Math.toRadians(courseDegrees) - renderer.getRotation());
                    Stroke cs = g2.getStroke();
                    double zs = gSpeed * renderer.getZoom();
                    if (zs < 50) {
                        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0,
                                new float[] { 5, 5 }, 0));
                        g2.drawLine(0, 0, 0, -50);
                    }
                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                    g2.drawLine(0, 0, 0, -(int) zs);
                    g2.setStroke(cs);
                }
            }
        }
        
        g2.dispose();
        return;
    }

}
