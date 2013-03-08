/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: Location.java 9616 2012-12-30 23:23:22Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.ws;

import java.util.StringTokenizer;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.w3c.dom.NodeList;

public class Location {

    private double latitude, longitude, depth, easting, northing;
    String id = null;
    String name = null;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.depth = 0;
        this.easting = 0;
        this.northing = 0;
    }

    public Location(Location loc) {
        this.latitude = loc.getLatitude();
        this.longitude = loc.getLongitude();
        this.depth = loc.getDepth();
        this.easting = loc.getEasting();
        this.northing = loc.getNorthing();
    }

    public Location(double latitude, double longitude, double depth) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.depth = depth;
        this.easting = 0;
        this.northing = 0;
    }

    public double[] getNEDOffsetFrom(Location anotherLocation) {
        double diff[] = CoordinateSimpleUtil.latLonDiff(anotherLocation.getLatitude(), anotherLocation.getLongitude(),
                latitude, longitude);
        double ret[] = new double[3];
        ret[0] = diff[0] + getNorthing() - anotherLocation.getNorthing();
        ret[1] = diff[1] + getEasting() - anotherLocation.getEasting();
        ret[2] = depth - anotherLocation.depth;

        return ret;
    }

    private double[] latLonAddNE(double lat, double lon, double north, double east) {
        double lat_rad = lat * Math.PI / 180;
        lat += north / (111132.92 - 559.82 * Math.cos(2 * lat_rad) + 1.175 * Math.cos(4 * lat_rad));
        lon += east / (111412.84 * Math.cos(lat_rad) - 93.5 * Math.cos(3 * lat_rad));

        while (lon > 180)
            lon -= 360;
        while (lon < -180)
            lon += 360;
        while (lat > 90)
            lat -= 180;
        while (lat < -90)
            lat += 180.0;

        double[] latLon = new double[2];
        latLon[0] = lat;
        latLon[1] = lon;
        return latLon;
    }

    private String latitudeAsString(double latitude) {
        double tmp = latitude;
        String letter = "N";
        String result = "";
        if (tmp < 0) {
            tmp = -tmp;
            letter = "S";
        }
        int degs = (int) Math.floor(tmp);
        result = result + degs + "" + letter;
        tmp -= degs;
        tmp *= 60;
        int mins = (int) Math.floor(tmp);
        result = result + mins + "'";
        tmp -= mins;
        tmp *= 60;
        float secs = (float) tmp;
        result = result + secs + "''";

        return result;
    }

    private String longitudeAsString(double longitude) {
        double tmp = longitude;
        String letter = "E";
        String result = "";
        if (tmp < 0) {
            tmp = -tmp;
            letter = "W";
        }
        int degs = (int) Math.floor(tmp);
        result = result + degs + "" + letter;
        tmp -= degs;
        tmp *= 60;
        int mins = (int) Math.floor(tmp);
        result = result + mins + "'";
        tmp -= mins;
        tmp *= 60;
        float secs = (float) tmp;
        result = result + secs + "''";

        return result;
    }

    public String getLatitudeAsString() {
        return latitudeAsString(getLatitude());
    }

    public String getLongitudeAsString() {
        return longitudeAsString(getLongitude());
    }

    public String getAbsoluteLatitude() {
        double newLatLon[] = latLonAddNE(getLatitude(), getLongitude(), getNorthing(), getEasting());
        return latitudeAsString(newLatLon[0]);
    }

    public String getAbsoluteLongitude() {
        double newLatLon[] = latLonAddNE(getLatitude(), getLongitude(), getNorthing(), getEasting());
        return longitudeAsString(newLatLon[1]);
    }

    public double getAbsoluteLongitudeDouble() {
        double newLatLon[] = latLonAddNE(getLatitude(), getLongitude(), getNorthing(), getEasting());
        return newLatLon[1];
    }

    public double getAbsoluteLatitudeDouble() {
        double newLatLon[] = latLonAddNE(getLatitude(), getLongitude(), getNorthing(), getEasting());
        return newLatLon[0];
    }

    public double getDepth() {
        return depth;
    }

    public void setDepth(double depth) {
        this.depth = depth;
    }

    public double getEasting() {
        return easting;
    }

    public void setEasting(double easting) {
        this.easting = easting;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = parseDMSString(latitude);
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = parseDMSString(longitude);
    }

    public double getNorthing() {
        return northing;
    }

    public void setNorthing(double northing) {
        this.northing = northing;
    }

    public void translate(double northOffset, double eastOffset, double depthOffset) {
        northing += northOffset;
        easting += eastOffset;
        depth += depthOffset;
    }

    /**
     * This method transforms spherical coordinates to cartesian coordinates.
     * 
     * @param r Distance
     * @param theta Azimuth (\u00B0)
     * @param phi Zenith (\u00B0)
     * @return Array with x,y,z positions
     */
    public static double[] sphericalToCartesianCoordinates(double r, double theta, double phi) {
        // converts degrees to rad
        theta = Math.toRadians(theta);
        phi = Math.toRadians(phi);

        double x = r * Math.cos(theta) * Math.sin(phi);
        double y = r * Math.sin(theta) * Math.sin(phi);
        double z = r * Math.cos(phi);

        double[] cartesian = new double[3];
        cartesian[0] = x;
        cartesian[1] = y;
        cartesian[2] = z;

        return cartesian;
    }

    public static void main(String[] args) {
        Location loc = new Location(41.23974, -8.341);
        Location loc2 = new Location(loc);
        loc2.setLongitude(-8.3434);
        System.out.println(loc.getNEDOffsetFrom(loc2)[1]);
        loc = new Location(41.2323, -8.2323);
        loc.setEasting(-23);
        loc.setDepth(34.2343);
        System.out.println(loc.asXMLElement().asXML());
    }

    public static double parseDMSString(String dms) {
        StringTokenizer st = new StringTokenizer(dms, " 'º\u00B0NWES");
        double degrees = 0, minutes = 0, seconds = 0;
        try {
            degrees = Double.parseDouble(st.nextToken());
            minutes = Double.parseDouble(st.nextToken());
            seconds = Double.parseDouble(st.nextToken());
        }
        catch (Exception e) {

        }
        double value = degrees + minutes / 60.0 + seconds / 3600.0;
        if (dms.contains("W") || dms.contains("S")) {
            value = -value;
        }
        return value;
    }

    public Element asXMLElement() {
        Element elem = DocumentHelper.createElement("coordinate");
        elem.addElement("latitude").setText(getLatitudeAsString());
        elem.addElement("longitude").setText(getLongitudeAsString());
        elem.addElement("depth").setText("" + getDepth());
        elem.addElement("offset-north").setText("" + getNorthing());
        elem.addElement("offset-east").setText("" + getEasting());

        return elem;
    }

    public static Location readLocation(org.w3c.dom.Node node) {
        Location loc = new Location(0, 0);
        NodeList nl = node.getChildNodes();
        double azimuth = 0, zenith = 0, distance = 0;
        for (int i = 0; i < nl.getLength(); i++) {
            org.w3c.dom.Node nd = nl.item(i);

            if (nd.getNodeName().equalsIgnoreCase("latitude"))
                loc.setLatitude(nd.getTextContent());

            else if (nd.getNodeName().equalsIgnoreCase("longitude"))
                loc.setLongitude(nd.getTextContent());

            else if (nd.getNodeName().equalsIgnoreCase("height"))
                loc.setDepth(-Double.parseDouble(nd.getTextContent()));

            else if (nd.getNodeName().equalsIgnoreCase("depth"))
                loc.setDepth(Double.parseDouble(nd.getTextContent()));

            else if (nd.getNodeName().equalsIgnoreCase("offset-north"))
                loc.setNorthing(Double.parseDouble(nd.getTextContent()));

            else if (nd.getNodeName().equalsIgnoreCase("offset-east"))
                loc.setEasting(Double.parseDouble(nd.getTextContent()));

            else if (nd.getNodeName().equalsIgnoreCase("offset-west"))
                loc.setEasting(-Double.parseDouble(nd.getTextContent()));

            else if (nd.getNodeName().equalsIgnoreCase("offset-south"))
                loc.setNorthing(-Double.parseDouble(nd.getTextContent()));

            else if (nd.getNodeName().equalsIgnoreCase("offset-down"))
                loc.setDepth(loc.getDepth() + Double.parseDouble(nd.getTextContent()));

            else if (nd.getNodeName().equalsIgnoreCase("offset-up"))
                loc.setDepth(loc.getDepth() - Double.parseDouble(nd.getTextContent()));

            else if (nd.getNodeName().equalsIgnoreCase("azimuth"))
                azimuth = Double.parseDouble(nd.getTextContent());

            else if (nd.getNodeName().equalsIgnoreCase("zenith"))
                zenith = Double.parseDouble(nd.getTextContent());

            else if (nd.getNodeName().equalsIgnoreCase("offset-distance"))
                distance = Double.parseDouble(nd.getTextContent());

        }
        if (distance != 0) {
            double[] offsets = Location.sphericalToCartesianCoordinates(distance, azimuth, zenith);
            loc.translate(offsets[0], offsets[1], offsets[2]);
        }
        return loc;
    }

    public static Location readLocation(Element elem) {
        Location loc = new Location(0, 0);

        Node node = elem.selectSingleNode("./latitude");
        if (node != null)
            loc.setLatitude(node.getText());

        node = elem.selectSingleNode("./longitude");
        if (node != null)
            loc.setLongitude(node.getText());

        node = elem.selectSingleNode("./height");
        if (node != null)
            loc.setDepth(-Double.parseDouble(node.getText()));

        node = elem.selectSingleNode("./depth");
        if (node != null)
            loc.setDepth(Double.parseDouble(node.getText()));

        node = elem.selectSingleNode("./offset-north");
        if (node != null)
            loc.setNorthing(Double.parseDouble(node.getText()));

        node = elem.selectSingleNode("./offset-east");
        if (node != null)
            loc.setEasting(Double.parseDouble(node.getText()));

        node = elem.selectSingleNode("./offset-west");
        if (node != null)
            loc.setEasting(-Double.parseDouble(node.getText()));

        node = elem.selectSingleNode("./offset-south");
        if (node != null)
            loc.setNorthing(-Double.parseDouble(node.getText()));

        node = elem.selectSingleNode("./offset-down");
        if (node != null)
            loc.setDepth(Double.parseDouble(node.getText()) + loc.getDepth());

        node = elem.selectSingleNode("./offset-up");
        if (node != null)
            loc.setDepth(-Double.parseDouble(node.getText()) + loc.getDepth());

        double azimuth = 0, zenith = 0, distance = 0;

        node = elem.selectSingleNode("./azimuth");
        if (node != null)
            azimuth = Double.parseDouble(node.getText());

        node = elem.selectSingleNode("./zenith");
        if (node != null)
            zenith = Double.parseDouble(node.getText());

        node = elem.selectSingleNode("./offset-distance");
        if (node != null)
            distance = Double.parseDouble(node.getText());

        double[] offsets = Location.sphericalToCartesianCoordinates(distance, azimuth, zenith);

        loc.translate(offsets[0], offsets[1], offsets[2]);

        return loc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
