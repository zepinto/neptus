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
 * $Id:: NMEAUtils.java 9616 2012-12-30 23:23:22Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import org.dinopolis.gpstool.gpsinput.nmea.NMEA0183Sentence;

import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCMessageType;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;
import pt.up.fe.dceg.neptus.util.llf.NeptusMessageLogger;

/**
 * @author ZP
 */
public class NMEAUtils {
	
	private static LinkedHashMap<String, Integer> gps_uids = new LinkedHashMap<String, Integer>();
	private static IMCMessage gpsFix = IMCDefinition.getInstance().create("GpsFix");	
	private static IMCMessage customMessage = null;
	
    private static boolean isExtGpsFix = true;

    {
        if (gpsFix != null) {
            if (gpsFix.getMessageType().getFieldType("validity") != null)
                isExtGpsFix = true;
            else
                isExtGpsFix = false;
        }
    }

	
	public static IMCMessage getCustomMessage() {
		if (customMessage == null) {
			IMCMessageType type = new IMCMessageType();
			type.addField("id", "plaintext", null, null, null);
			type.addField("home_lat", "fp64_t", "rad", null, null);
			type.addField("home_lon", "fp64_t", "rad", null, null);
			type.addField("x", "fp32_t", "meter", null, null);
			type.addField("y", "fp32_t", "meter", null, null);
			type.addField("z", "fp32_t", "meter", null, null);
			customMessage = new IMCMessage(type);
		}
		return customMessage;
	}
	
	public static void logGGASentenceAsGpsFix(String source, String ggaSentence) {
		
		try {
			NMEA0183Sentence nmea = new NMEA0183Sentence(ggaSentence);
		
			if (!nmea.isValid())
				return;
			
			
			List<?> data_fields = nmea.getDataFields();
			
			int time = (int)Float.parseFloat((String) data_fields.get(0));
			String latitude = (String) data_fields.get(1);
			String north_south = (String) data_fields.get(2);
			String longitude = (String) data_fields.get(3);
			String east_west = (String) data_fields.get(4);
			int valid_fix = Integer.parseInt((String) data_fields.get(5));
			int num_satellites = Integer.parseInt((String) data_fields.get(6));
			float hor_dilution = Float.parseFloat((String) data_fields.get(7));
			float altitude = Float.parseFloat((String) data_fields.get(8));
			
			double wgs84_lat = nmeaLatOrLongToWGS84(latitude);
			double wgs84_long = nmeaLatOrLongToWGS84(longitude);
	
			if (north_south.equalsIgnoreCase("S"))
				wgs84_lat = -wgs84_lat;
	
			if (east_west.equalsIgnoreCase("W"))
				wgs84_long = -wgs84_long;
			
			int uid = 0;
			if (gps_uids.containsKey(source))
				uid = gps_uids.get(source);
			else {
				uid = gps_uids.size();
				gps_uids.put(source, uid);
			}

			gpsFix.setValue("id", uid);
			gpsFix.setValue("time", (int)time);
			gpsFix.setValue("lat", Math.toRadians(new Double(wgs84_lat)));
			gpsFix.setValue("lon", Math.toRadians(new Double(wgs84_long)));           
			if (isExtGpsFix) {
                int valitity = (valid_fix > 0 ? 0x4/* VALID_POS */: 0x0) | 0x80/* VALID_HDOP */;
			    gpsFix.setValue("validity", valitity);
			    gpsFix.setValue("height", altitude);
			    gpsFix.setValue("hdop", hor_dilution);
			}
			else {
			    gpsFix.setValue("fix_quality", (valid_fix > 0)? 1 : 0);
			    gpsFix.setValue("altitude", altitude);
	            gpsFix.setValue("hdilution", hor_dilution);
			}
			gpsFix.setValue("satellites", num_satellites);
			
//			NeptusMessageLogger.getLogger().logMessage(source, "unknown", gpsFix);
			gpsFix.setSrc(ImcId16.NULL_ID.intValue());
            NeptusMessageLogger.logMessage(gpsFix);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void convertGGALogToLLF(File source, File dest) throws IOException {
		
		BufferedReader reader = new BufferedReader(new FileReader(source));
		
		String line = reader.readLine();
		while (line != null) {
			logGGASentenceAsGpsFix(source.getName(), line);
		}		
		reader.close();
	}
	
	
	public static double nmeaLatOrLongToWGS84(String nmea_pos) throws NumberFormatException {
		int comma_pos = nmea_pos.indexOf('.');
		if ((comma_pos != 4) && (comma_pos != 5))
			throw new NumberFormatException("unknown NMEA position format: '" + nmea_pos + "'");

		String wgs84_deg = nmea_pos.substring(0, comma_pos - 2);
		String wgs84_min = nmea_pos.substring(comma_pos - 2);
		double wgs84_pos = Double.parseDouble(wgs84_deg) + Double.parseDouble(wgs84_min) / 60.0;
		return (wgs84_pos);
	}


	public static LocationType processGGASentence(String sentence) {		
		try {
			NMEA0183Sentence nmea = new NMEA0183Sentence(sentence);
		
			if (!nmea.isValid())
				return null;
			
			List<?> data_fields = nmea.getDataFields();
			
			String latitude = (String) data_fields.get(1);
			String north_south = (String) data_fields.get(2);
			String longitude = (String) data_fields.get(3);
			String east_west = (String) data_fields.get(4);
			int valid_fix = Integer.parseInt((String) data_fields.get(5));
	
			if (valid_fix == 0)
				return null;
	
			// check for empty messages:
			if (latitude.length() == 0)
				return null;
	
			double wgs84_lat = nmeaLatOrLongToWGS84(latitude);
			double wgs84_long = nmeaLatOrLongToWGS84(longitude);
	
			if (north_south.equalsIgnoreCase("S"))
				wgs84_lat = -wgs84_lat;
	
			if (east_west.equalsIgnoreCase("W"))
				wgs84_long = -wgs84_long;
	
			LocationType loc = new LocationType();
			loc.setLatitude(wgs84_lat);
			loc.setLongitude(wgs84_long);
			loc.setDepth(-new Float((String)data_fields.get(8)));
	
			return loc;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	

	public static LocationType processRMCSentence(String sentence) {
		try {
			NMEA0183Sentence nmea = new NMEA0183Sentence(sentence);
			if (!nmea.isValid())
				return null;
			List<?> data_fields = nmea.getDataFields();

			String latitude = (String) data_fields.get(2);
			String north_south = (String) data_fields.get(3);
			String longitude = (String) data_fields.get(4);
			String east_west = (String) data_fields.get(5);

			// check for empty messages:
			if (latitude.length() == 0)
				return null;

			double wgs84_lat = nmeaLatOrLongToWGS84(latitude);
			double wgs84_long = nmeaLatOrLongToWGS84(longitude);

			if (north_south.equalsIgnoreCase("S"))
				wgs84_lat = -wgs84_lat;

			if (east_west.equalsIgnoreCase("W"))
				wgs84_long = -wgs84_long;

			LocationType loc = new LocationType();
			loc.setLatitude(wgs84_lat);
			loc.setLongitude(wgs84_long);

			return loc;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args) {
		String nmea = "$GPGGA,120602.476,4112.4827,N,00832.0861,W,1,03,3.4,-51.3,M,51.3,M,,0000*5C";
		LocationType lt = NMEAUtils.processGGASentence(nmea);
		System.out.println(lt.getDebugString());
		if (lt != null) {
			IMCMessage msg = IMCDefinition.getInstance().create("RemoteSensorInfo");
			msg.setValue("id", "914785889");
			msg.setValue("lat", lt.getLatitudeAsDoubleValue());
			msg.setValue("lon", lt.getLongitudeAsDoubleValue());
			msg.setValue("alt", -lt.getDepth());
			msg.setValue("heading", 0);
			msg.setValue("data", "");
			msg.setValue("sensor_class", "auv");
			
			msg.dump(System.out);
		}
		
		logGGASentenceAsGpsFix("+351914785889", nmea);
		logGGASentenceAsGpsFix("+351914785889", nmea);
		logGGASentenceAsGpsFix("+351914785887", nmea);
		logGGASentenceAsGpsFix("+351914785889", nmea);
	}
}
